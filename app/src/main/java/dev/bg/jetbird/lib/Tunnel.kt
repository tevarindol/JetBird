package dev.bg.jetbird.lib

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.system.OsConstants
import dev.bg.jetbird.BuildConfig
import dev.bg.jetbird.data.model.CIDR
import dev.bg.jetbird.data.model.IFace
import dev.bg.jetbird.service.VPNService
import dev.bg.jetbird.util.calculateTunneledRoutes
import dev.bg.jetbird.util.ktx.getProfileManager
import dev.bg.jetbird.util.ktx.isLeanback
import io.netbird.android.Android
import io.netbird.android.Client
import io.netbird.android.ConnectionListener
import io.netbird.android.DNSList
import io.netbird.android.EnvList
import io.netbird.android.NetworkArray
import io.netbird.android.NetworkChangeListener
import io.netbird.android.PeerInfoArray
import io.netbird.android.TunAdapter
import io.netbird.android.URLOpener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface TunnelListener {
    fun onStarted()
    fun onStopped()
    fun onLog(message: String, verbose: Boolean, force: Boolean)
    fun onError(message: String)
}

enum class EngineLoggingLevel {
    None,
    Info,
    Trace
}

class Tunnel(
    private val vpnService: VPNService,
    private val tunnelListener: TunnelListener,
    private val connectionListener: ConnectionListener
): TunAdapter, NetworkChangeListener, DNSListener {

    val dnsWatch: DNSWatch = DNSWatch(vpnService.baseContext, this)
    var iface: IFace? = null

    private val scope = CoroutineScope(SupervisorJob())
    private val client: Client = Android.newClient(
        Build.VERSION.SDK_INT.toLong(),
        Build.MODEL,
        "JetBird-${BuildConfig.VERSION_NAME}",
        this,
        InterfaceDiscovery(),
        this
    )
    private var isRunning = false
    private var excludedApps = emptySet<String>()
    private var invertExclusion = false
    private var routes: String? = null
    private var routeOverrideEnabled = false
    private var routeOverrides = emptySet<String>()

    @Synchronized
    fun start(
        urlOpener: URLOpener?,
        loggingLevel: EngineLoggingLevel,
        excludeApps: Set<String>,
        invertExcludeApps: Boolean,
        enableRouteOverrides: Boolean,
        overrides: Set<String>,
        fallbackDns: String?,
        forceRelay: Boolean,
        lazyConnection: Boolean
    ) {
        if (isRunning) return
        when (loggingLevel) {
            EngineLoggingLevel.Info -> client.setInfoLogLevel()
            EngineLoggingLevel.Trace -> client.setTraceLogLevel()
            EngineLoggingLevel.None -> client.disableLogs()
        }
        excludedApps = excludeApps
        invertExclusion = invertExcludeApps
        routeOverrideEnabled = enableRouteOverrides
        routeOverrides = overrides
        dnsWatch.updateFallbackDns(fallbackDns)
        scope.launch {
            try {
                client.setConnectionListener(connectionListener)
                notifyServiceStateListeners(true)
                val profileManager = vpnService.baseContext.getProfileManager()
                val platformFiles = PlatformFiles(
                    profileManager.activeConfigPath,
                    profileManager.activeStateFilePath,
                    vpnService.baseContext.cacheDir.absolutePath
                )
                val envList = EnvList().apply {
                    put(Android.getEnvKeyNBForceRelay(), forceRelay.toString())
                    put(Android.getEnvKeyNBLazyConn(), lazyConnection.toString())
                }
                if (urlOpener == null) {
                    dnsWatch.registerNetworkCallback()
                    client.runWithoutLogin(
                        platformFiles,
                        dnsWatch.getActiveDns(),
                        {
                            log("Netbird DNS ready")
                        },
                        envList
                    )
                } else {
                    dnsWatch.registerNetworkCallback()
                    client.run(
                        platformFiles,
                        urlOpener,
                        vpnService.baseContext.isLeanback(),
                        dnsWatch.getActiveDns(),
                        {
                            log("Netbird DNS ready")
                        },
                        envList
                    )
                }
            } catch (e: Exception) {
                notifyError(e)
            } finally {
                notifyServiceStateListeners(false)
                dnsWatch.engineHasStopped()
                client.removeConnectionListener()
            }
        }
    }

    override fun onDnsChanged(dnsServers: DNSList) {
        log("Telling Netbird about new DNS servers", verbose = true)
        try {
            client.onUpdatedHostDNS(dnsServers)
        } catch (e: Exception) {
            log("Failed to update DNS servers in client $e")
        }
    }

    fun stop() {
        client.stop()
    }

    fun peersInfo(): PeerInfoArray {
        return client.peersList()
    }

    fun networks(): NetworkArray {
        return client.networks() ?: NetworkArray()
    }

    private fun log(
        message: String,
        verbose: Boolean = false,
        force: Boolean = false
    ) {
        tunnelListener.onLog(message, verbose, force)
    }

    private fun notifyError(e: Exception) {
        tunnelListener.onError(e.message ?: "Unknown error")
    }

    private fun notifyServiceStateListeners(engineIsRunning: Boolean) {
        isRunning = engineIsRunning
        if (engineIsRunning) {
            tunnelListener.onStarted()
        } else {
            tunnelListener.onStopped()
        }
    }

    override fun onNetworkChanged(commaDelimitedRoutes: String) {
        // routes is comma-delimited instead of semicolon-delimited
        val routes = commaDelimitedRoutes.replace(',', ';')
        if (this.routes != routes) {
            log("NetBird sent new routes - $routes")
            val iface = this.iface
            if (!isRunning || iface == null) return
            try {
                val fd = configureInterface(
                    iface.ipv4Cidr,
                    iface.ipv6Cidr,
                    iface.mtu,
                    iface.dns,
                    iface.domains,
                    routes
                )
                if (fd != -1L) {
                    // it is cast as Long because of the override, but it is an underlying int
                    log("Renewing tunnel")
                    vpnService.protect(fd.toInt())
                    client.renewTun(fd)
                } else {
                    log("Failed to configure interface")
                }
            } catch (e: Exception) {
                log("Failed to update routes ${e.message}")
            }
        }
    }

    override fun setInterfaceIP(ip: String) {}

    override fun setInterfaceIPv6(ip: String?) {}

    override fun configureInterface(
        ipv4Cidr: String?,
        ipv6Cidr: String?,
        mtu: Long,
        dns: String?,
        domains: String?,
        routes: String?
    ): Long {
        this.iface = IFace(
            ipv4Cidr = ipv4Cidr,
            ipv6Cidr = ipv6Cidr,
            mtu = mtu,
            dns = dns,
            domains = domains
        )
        this.routes = routes
        return try {
            log("Adding CIDR block to interface $ipv4Cidr", verbose = true)
            if (ipv4Cidr == null) {
                log("NetBird sent invalid CIDR block")
                return 0L
            }
            val upstreamRoutes = when (routes) {
                null, "" -> emptyList()
                else -> routes.split(";")
            }.toSet()
            val allocatedIpv6Cidr = when {
                !ipv6Cidr.isNullOrEmpty() -> CIDR(ipv6Cidr)
                else -> null
            }
            val allocatedIpv4Cidr = CIDR(ipv4Cidr)
            val builder = vpnService.tunnelBuilder
                .addAddress(allocatedIpv4Cidr.address, allocatedIpv4Cidr.mask)
                .allowFamily(OsConstants.AF_INET)
                .allowFamily(OsConstants.AF_INET6)
                .setMtu(mtu.toInt())
                // IPv6 setup
                .apply {
                    if (allocatedIpv6Cidr != null) {
                        log("Adding IPv6 CIDR block to interface ${allocatedIpv6Cidr.address}/${allocatedIpv6Cidr.mask}")
                        addAddress(allocatedIpv6Cidr.address, allocatedIpv6Cidr.mask)
                        if (hasIPv4DefaultRoute(upstreamRoutes)) {
                            log("Adding ::/0 route to prevent leak")
                            addRoute("::", 0)
                        }
                    }
                }
                // DNS
                .apply {
                    if (dns.isNullOrEmpty()) {
                        log("DNS null")
                        return@apply
                    }
                    if (vpnService.isUsingPrivateDns) {
                        log("Ignoring DNS because private DNS is active")
                        return@apply
                    }
                    log("Registering DNS: $dns")
                    addDnsServer(dns)
                }
                // Route and app overrides
                .apply {
                    when (domains) {
                        null, "" -> emptyList()
                        else -> domains.split(";")
                    }.forEach { domain ->
                        log("Adding search domain: $domain", verbose = true)
                        addSearchDomain(domain)
                    }
                    if (upstreamRoutes.isNotEmpty()) {
                        if (routeOverrideEnabled && routeOverrides.isNotEmpty()) {
                            log("Using route overrides")
                            calculateTunneledRoutes(
                                allowedIps = upstreamRoutes,
                                disallowedIps = routeOverrides
                            ).map { CIDR(it) }.forEach { route ->
                                log("Adding route: ${route.address}/${route.mask}", verbose = true)
                                addRoute(route.address, route.mask)
                            }
                        } else {
                            log("Using upstream routes")
                            upstreamRoutes.map { CIDR(it) }.forEach { route ->
                                log("Adding route: ${route.address}/${route.mask}", verbose = true)
                                addRoute(route.address, route.mask)
                            }
                        }
                    }
                    if (invertExclusion) {
                        // Include
                        // For some reason addAllowedApplication seems to block DNS
                        // Calculate the inverse and exclude all other apps
                        val apps = vpnService
                            .baseContext
                            .packageManager
                            .getInstalledApplications(PackageManager.GET_META_DATA)
                            .toList()
                            .filter { p ->
                                (p.flags and ApplicationInfo.FLAG_SYSTEM == 0 && p.packageName != vpnService.baseContext.packageName) ||
                                        p.packageName == "com.google.android.projection.gearhead" ||
                                        p.packageName == "com.google.android.apps.messaging" ||
                                        p.packageName == "com.google.android.gms"
                            }
                        val exclude = apps.filter { p -> !excludedApps.contains(p.packageName) }.map { p -> p.packageName }
                        val difference = apps.filter { p -> !exclude.contains(p.packageName) }.map { p -> p.packageName }
                        exclude.forEach { packageName ->
                            try {
                                addDisallowedApplication(packageName)
                            } catch (e: Exception) {
                                log("Failed to exclude $packageName from the tunnel: $e")
                            }
                        }
                        if (difference.isNotEmpty()) {
                            difference.forEach { packageName ->
                                log("Including $packageName in the tunnel")
                            }
                            log("Excluding all other packages from the tunnel")
                        } else {
                            log("Including all apps in tunnel")
                        }
                    } else {
                        // Exclude
                        excludedApps.forEach { packageName ->
                            try {
                                log("Excluding $packageName from tunnel")
                                addDisallowedApplication(packageName)
                            } catch (e: Exception) {
                                log("Failed to exclude $packageName from tunnel: $e")
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setMetered(false)
                    }
                    setBlocking(true)
                }
            vpnService.setUnderlyingNetworks(null)
            builder.establish().use { tun ->
                if (tun == null) {
                    log("Failed to create tunnel")
                    return 0L
                }
                return tun.detachFd().toLong()
            }
        } catch (e: Exception) {
            log("Failed to create tunnel ${e.message} ${e.cause?.message.let { "(${it})" }}")
            0L
        }
    }

    override fun protectSocket(socket: Int): Boolean {
        // 2762aafc3bedd4ddbed7ddfe7771a6020e2f3491/tool/src/main/java/io/netbird/client/tool/IFace.java#L44-L50
        vpnService.protect(socket)
        return true
    }

    override fun updateAddr(s: String) {}

    override fun onNetworkChanged() {
        log("Network changed")
    }

    // 9b0f0fa6d1772112ffea1096361bf402426e3b00/tool/src/main/java/io/netbird/client/tool/IFace.java#L166-L173
    private fun hasIPv4DefaultRoute(routes: Set<String>): Boolean {
        return routes.any { r ->
            val cidr = CIDR(r)
            cidr.address == "0.0.0.0" && cidr.mask == 0
        }
    }
}
