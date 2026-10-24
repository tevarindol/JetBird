package dev.bg.jetbird.lib

import android.os.Build
import dev.bg.jetbird.BuildConfig
import dev.bg.jetbird.data.model.IFace
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
    private val host: TunnelHost,
    private val tunnelListener: TunnelListener,
    private val connectionListener: ConnectionListener
): TunAdapter, NetworkChangeListener, DNSListener {

    val dnsWatch: DNSWatch = DNSWatch(host.appContext, this)
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
        iceKeepAliveInterval: String?,
        iceDisconnectedTimeout: String?,
        iceFailedTimeout: String?,
        socksPort: Int? = null
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
                val profileManager = host.appContext.getProfileManager()
                val platformFiles = PlatformFiles(
                    profileManager.activeConfigPath,
                    profileManager.activeStateFilePath,
                    host.appContext.cacheDir.absolutePath
                )
                val envList = EnvList().apply {
                    put(Android.getEnvKeyNBForceRelay(), forceRelay.toString())
                    put("NB_USE_NETSTACK_MODE", (socksPort != null).toString())
                    if (socksPort != null) {
                        put("NB_SOCKS5_LISTENER_ADDRESS", "127.0.0.1")
                        put("NB_SOCKS5_LISTENER_PORT", socksPort.toString())
                    }
                    putEnvSeconds("NB_ICE_KEEP_ALIVE_INTERVAL_SEC", iceKeepAliveInterval)
                    putEnvSeconds("NB_ICE_DISCONNECTED_TIMEOUT_SEC", iceDisconnectedTimeout)
                    putEnvSeconds("NB_ICE_FAILED_TIMEOUT_SEC", iceFailedTimeout)
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
                        host.appContext.isLeanback(),
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

    fun selectRoute(route: String) {
        client.selectRoute(route)
    }

    fun deselectRoute(route: String) {
        client.deselectRoute(route)
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

    override fun onNetworkChanged(ignore: String) {
        log("NetBird sent new routes - $routes")
        val iface = this.iface
        if (!isRunning || iface == null) return

        val settings = client.tunSettings ?: return
        if (!iface.didChange(settings.searchDomains, settings.routes)) return

        try {
            val fd = configureInterface(
                iface.ipv4Cidr,
                iface.ipv6Cidr,
                iface.mtu,
                iface.dns,
                settings.searchDomains,
                settings.routes
            )
            if (fd != -1L) {
                // it is cast as Long because of the override, but it is an underlying int
                log("Renewing tunnel")
                host.protectSocket(fd.toInt())
                client.renewTun(fd)
            } else {
                log("Failed to configure interface")
            }
        } catch (e: Exception) {
            log("Failed to update routes ${e.message}")
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
            domains = domains,
            routes = routes
        )
        this.routes = routes
        log("Adding CIDR block to interface $ipv4Cidr", verbose = true)
        if (ipv4Cidr == null) {
            log("NetBird sent invalid CIDR block")
            return 0L
        }
        val effectiveDns = when {
            dns.isNullOrEmpty() -> null
            dnsWatch.isPrivateDnsActive -> {
                log("Ignoring DNS because private DNS is active")
                null
            }
            else -> dns
        }
        return host.configureVpnInterface(
            ipv4Cidr = ipv4Cidr,
            ipv6Cidr = ipv6Cidr,
            mtu = mtu,
            dns = effectiveDns,
            domains = domains,
            routes = routes,
            excludedApps = excludedApps,
            invertExclusion = invertExclusion,
            routeOverrideEnabled = routeOverrideEnabled,
            routeOverrides = routeOverrides
        )
    }

    override fun protectSocket(socket: Int): Boolean {
        // 2762aafc3bedd4ddbed7ddfe7771a6020e2f3491/tool/src/main/java/io/netbird/client/tool/IFace.java#L44-L50
        return host.protectSocket(socket)
    }

    override fun updateAddr(s: String) {}

    override fun onNetworkChanged() {
        log("Network changed")
    }

    private fun EnvList.putEnvSeconds(key: String, value: String?) {
        val seconds = value?.trim()?.toIntOrNull() ?: return
        put(key, seconds.toString())
    }

}
