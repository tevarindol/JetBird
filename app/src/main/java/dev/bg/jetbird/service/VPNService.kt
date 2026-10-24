package dev.bg.jetbird.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.system.OsConstants
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.R
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.data.model.CIDR
import dev.bg.jetbird.data.model.EngineMode
import dev.bg.jetbird.data.model.ServiceEvent
import dev.bg.jetbird.data.model.VPNState
import dev.bg.jetbird.lib.EngineBinder
import dev.bg.jetbird.lib.EngineController
import dev.bg.jetbird.lib.TunnelHost
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.repository.VpnRepository
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.calculateTunneledRoutes
import io.netbird.android.URLOpener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("VpnServicePolicy")
@AndroidEntryPoint
class VPNService: VpnService(), TunnelHost {

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var vpnRepository: VpnRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var controller: EngineController

    override val appContext: Context
        get() = applicationContext

    override fun onCreate() {
        super.onCreate()
        controller = EngineController(
            service = this,
            host = this,
            vpnRepository = vpnRepository,
            preferencesRepository = preferencesRepository,
            logRepository = logRepository,
            ioDispatcher = ioDispatcher,
            mode = EngineMode.VPN,
            notificationText = { getString(R.string.foreground_notification_description) },
            onEngineStopped = { stopForeground(STOP_FOREGROUND_REMOVE) }
        )
        controller.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        if (intent.action == Actions.TILE_STOP_SERVICE_ACTION && controller.isEngineStarted) {
            controller.stop()
        }
        if (preferencesRepository.hasAuthed) {
            when (intent.action) {
                Actions.ALWAYS_ON_START -> {
                    controller.log("Android requesting us to start through always-on VPN")
                    controller.start(null)
                }
                Actions.BOOT_START_SERVICE_ACTION -> {
                    controller.log("Starting tunnel after device reboot")
                    controller.start(null)
                }
                Actions.TILE_START_SERVICE_ACTION -> {
                    if (!controller.isEngineStarted) {
                        controller.log("Starting tunnel from quick tile")
                        controller.start(null)
                    } else {
                        controller.log("Requested start through quick tile but tunnel is already up")
                    }
                }
                Actions.CONTROL_ENABLE_EXIT_NODE_ACTION -> {
                    controller.enableExitNode(intent.getStringExtra(Actions.EXTRA_EXIT_NODE_NAME))
                }
                Actions.CONTROL_DISABLE_EXIT_NODE_ACTION -> {
                    controller.disableExitNode()
                }
                else -> {
                    controller.log("${intent.`package` ?: "Unknown package"} (${intent.action}) tried to start tunnel - ignoring")
                }
            }
        } else {
            intent.action?.let { action ->
                controller.log("$action requested start although user has not authed")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.d("onBind()")
        return VPNServiceBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("onUnbind()")
        if (!controller.isEngineStarted) {
            stopSelf()
        }
        return false
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        super.onDestroy()
        if (this::controller.isInitialized) {
            controller.stop()
        }
    }

    override fun onRevoke() {
        controller.stop()
    }

    override fun configureVpnInterface(
        ipv4Cidr: String?,
        ipv6Cidr: String?,
        mtu: Long,
        dns: String?,
        domains: String?,
        routes: String?,
        excludedApps: Set<String>,
        invertExclusion: Boolean,
        routeOverrideEnabled: Boolean,
        routeOverrides: Set<String>
    ): Long {
        if (ipv4Cidr == null) {
            return 0L
        }
        return try {
            val upstreamRoutes = when (routes) {
                null, "" -> emptyList()
                else -> routes.split(";")
            }.toSet()
            val allocatedIpv6Cidr = when {
                !ipv6Cidr.isNullOrEmpty() -> CIDR(ipv6Cidr)
                else -> null
            }
            val allocatedIpv4Cidr = CIDR(ipv4Cidr)
            val builder = tunnelBuilder
                .addAddress(allocatedIpv4Cidr.address, allocatedIpv4Cidr.mask)
                .allowFamily(OsConstants.AF_INET)
                .allowFamily(OsConstants.AF_INET6)
                .setMtu(mtu.toInt())
                // IPv6 setup
                .apply {
                    if (allocatedIpv6Cidr != null) {
                        addAddress(allocatedIpv6Cidr.address, allocatedIpv6Cidr.mask)
                        if (hasIPv4DefaultRoute(upstreamRoutes)) {
                            addRoute("::", 0)
                        }
                    }
                }
                // DNS
                .apply {
                    if (dns.isNullOrEmpty()) {
                        return@apply
                    }
                    addDnsServer(dns)
                }
                // Route and app overrides
                .apply {
                    when (domains) {
                        null, "" -> emptyList()
                        else -> domains.split(";")
                    }.forEach { domain ->
                        addSearchDomain(domain)
                    }
                    if (upstreamRoutes.isNotEmpty()) {
                        if (routeOverrideEnabled && routeOverrides.isNotEmpty()) {
                            calculateTunneledRoutes(
                                allowedIps = upstreamRoutes,
                                disallowedIps = routeOverrides
                            ).map { CIDR(it) }.forEach { route ->
                                addRoute(route.address, route.mask)
                            }
                        } else {
                            upstreamRoutes.map { CIDR(it) }.forEach { route ->
                                addRoute(route.address, route.mask)
                            }
                        }
                    }
                    if (invertExclusion) {
                        // Include
                        // For some reason addAllowedApplication seems to block DNS
                        // Calculate the inverse and exclude all other apps
                        val apps = baseContext
                            .packageManager
                            .getInstalledApplications(PackageManager.GET_META_DATA)
                            .toList()
                            .filter { p ->
                                (p.flags and ApplicationInfo.FLAG_SYSTEM == 0 && p.packageName != baseContext.packageName) ||
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
                                controller.log("Failed to exclude $packageName from the tunnel: $e")
                            }
                        }
                        if (difference.isNotEmpty()) {
                            difference.forEach { packageName ->
                                controller.log("Including $packageName in the tunnel")
                            }
                            controller.log("Excluding all other packages from the tunnel")
                        } else {
                            controller.log("Including all apps in tunnel")
                        }
                    } else {
                        // Exclude
                        excludedApps.forEach { packageName ->
                            try {
                                addDisallowedApplication(packageName)
                            } catch (e: Exception) {
                                controller.log("Failed to exclude $packageName from tunnel: $e")
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setMetered(false)
                    }
                    setBlocking(true)
                }
            setUnderlyingNetworks(null)
            builder.establish().use { tun ->
                if (tun == null) {
                    controller.log("Failed to create tunnel")
                    return 0L
                }
                return tun.detachFd().toLong()
            }
        } catch (e: Exception) {
            controller.log("Failed to create tunnel ${e.message} ${e.cause?.message.let { "(${it})" }}")
            0L
        }
    }

    override fun protectSocket(socket: Int): Boolean {
        // 2762aafc3bedd4ddbed7ddfe7771a6020e2f3491/tool/src/main/java/io/netbird/client/tool/IFace.java#L44-L50
        protect(socket)
        return true
    }

    private val tunnelBuilder: Builder
        get() = Builder()

    // 9b0f0fa6d1772112ffea1096361bf402426e3b00/tool/src/main/java/io/netbird/client/tool/IFace.java#L166-L173
    private fun hasIPv4DefaultRoute(routes: Set<String>): Boolean {
        return routes.any { r ->
            val cidr = CIDR(r)
            cidr.address == "0.0.0.0" && cidr.mask == 0
        }
    }

    inner class VPNServiceBinder: Binder(), EngineBinder {
        override val state: StateFlow<VPNState>
            get() = controller.state
        override val events: SharedFlow<ServiceEvent>
            get() = controller.events

        override fun start(urlOpener: URLOpener?) {
            controller.start(urlOpener)
        }

        override fun stop() {
            controller.stop()
        }

        override fun selectRoute(route: String) {
            controller.selectRoute(route)
        }

        override fun deselectRoute(route: String) {
            controller.deselectRoute(route)
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == LAST_CALL_TRANSACTION) {
                onRevoke()
                return true
            }
            return false
        }
    }
}
