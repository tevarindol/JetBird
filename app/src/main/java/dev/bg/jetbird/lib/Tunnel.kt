package dev.bg.jetbird.lib

import android.os.Build
import android.system.OsConstants
import dev.bg.jetbird.BuildConfig
import dev.bg.jetbird.data.model.CIDR
import dev.bg.jetbird.service.VPNService
import dev.bg.jetbird.util.calculateTunneledRoutes
import dev.bg.jetbird.util.ktx.getConfigPath
import io.netbird.android.Android
import io.netbird.android.Client
import io.netbird.android.ConnectionListener
import io.netbird.android.DNSList
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
    fun onRoutesChange()
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

    private val scope = CoroutineScope(SupervisorJob())
    private val client: Client = Android.newClient(
        vpnService.baseContext.getConfigPath(),
        Build.MODEL,
        "JetBird-${BuildConfig.VERSION_NAME}",
        this,
        InterfaceDiscovery(),
        this
    )
    private var isRunning = false
    private var excludedApps = emptySet<String>()
    private var routeOverrideEnabled = false
    private var routeOverrides = emptySet<String>()

    @Synchronized
    fun start(
        urlOpener: URLOpener?,
        loggingLevel: EngineLoggingLevel,
        excludeApps: Set<String>,
        enableRouteOverrides: Boolean,
        overrides: Set<String>,
        fallbackDns: String?
    ) {
        if (isRunning) return
        when (loggingLevel) {
            EngineLoggingLevel.Info -> client.setInfoLogLevel()
            EngineLoggingLevel.Trace -> client.setTraceLogLevel()
            EngineLoggingLevel.None -> client.disableLogs()
        }
        excludedApps = excludeApps
        routeOverrideEnabled = enableRouteOverrides
        routeOverrides = overrides
        dnsWatch.updateFallbackDns(fallbackDns)
        scope.launch {
            try {
                notifyServiceStateListeners(true)
                if (urlOpener == null) {
                    dnsWatch.registerNetworkCallback()
                    client.runWithoutLogin(dnsWatch.getActiveDns()) {
                        log("Netbird DNS ready")
                    }
                } else {
                    dnsWatch.registerNetworkCallback()
                    client.run(urlOpener, dnsWatch.getActiveDns()) {
                        log("Netbird DNS ready")
                    }
                }
            } catch (e: Exception) {
                notifyError(e)
            } finally {
                notifyServiceStateListeners(false)
                dnsWatch.engineHasStopped()
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

    fun startClientListener() {
        client.setConnectionListener(connectionListener)
    }

    fun stopClientListener() {
        client.removeConnectionListener()
    }

    fun peersInfo(): PeerInfoArray {
        return client.peersList()
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

    override fun onNetworkChanged(routes: String) {
        log("NetBird sent new routes - $routes")
        tunnelListener.onRoutesChange()
    }

    override fun setInterfaceIP(ip: String) {}

    override fun configureInterface(
        cidr: String?,
        mtu: Long,
        dns: String?,
        domains: String?,
        routes: String?
    ): Long {
        return try {
            log("Adding CIDR block to interface $cidr", verbose = true)
            if (cidr == null) {
                log("Netbird sent invalid CIDR block")
                return 0L
            }
            val allocatedCidr = CIDR(cidr)
            val builder = vpnService.tunnelBuilder
                .addAddress(allocatedCidr.address, allocatedCidr.mask)
                .allowFamily(OsConstants.AF_INET)
                .allowFamily(OsConstants.AF_INET6)
                .setMtu(mtu.toInt())
                .apply {
                    if (dns.isNullOrEmpty()) {
                        log("DNS null")
                        return@apply
                    }
                    if (vpnService.isUsingPrivateDns) {
                        log("Ignoring DNS because private DNS is active")
                        return@apply
                    }
                    log("Adding DNS: $dns")
                    addDnsServer(dns)
                }
                .apply {
                    when (domains) {
                        null, "" -> emptyList()
                        else -> domains.split(";")
                    }.forEach { domain ->
                        log("Adding search domain: $domain", verbose = true)
                        addSearchDomain(domain)
                    }
                    val upstreamRoutes = when (routes) {
                        null, "" -> emptyList()
                        else -> routes.split(";")
                    }.toSet()
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
                    excludedApps.forEach { packageName ->
                        try {
                            log("Excluding $packageName from tunnel")
                            addDisallowedApplication(packageName)
                        } catch (e: Exception) {
                            log("Failed to exclude $packageName from tunnel")
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
            log("Failed to create tunnel ${e.message}")
            0L
        }
    }

    override fun protectSocket(socket: Int): Boolean {
        // TODO https://github.com/netbirdio/android-client/commit/26481951455cc77fec106651c946755174c675ef
        vpnService.protect(socket)
        return true
    }

    override fun updateAddr(s: String) {}

    override fun onNetworkChanged() {
        log("Network changed")
    }
}
