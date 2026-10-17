package dev.bg.jetbird.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.R
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.data.NetBirdErrors
import dev.bg.jetbird.data.model.ConnectionState
import dev.bg.jetbird.data.model.Network
import dev.bg.jetbird.data.model.ServiceEvent
import dev.bg.jetbird.data.model.VPNState
import dev.bg.jetbird.lib.EngineLoggingLevel
import dev.bg.jetbird.lib.Tunnel
import dev.bg.jetbird.lib.TunnelListener
import dev.bg.jetbird.receiver.VpnControlReceiver
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.repository.VpnRepository
import dev.bg.jetbird.widget.JetbirdWidget
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.Log
import dev.bg.jetbird.util.ReplaceableJob
import dev.bg.jetbird.util.ktx.getPendingIntent
import dev.bg.jetbird.util.ktx.toNetworkList
import dev.bg.jetbird.util.ktx.toPeerList
import io.netbird.android.ConnectionListener
import io.netbird.android.URLOpener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("VpnServicePolicy")
@AndroidEntryPoint
class VPNService: VpnService() {

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var vpnRepository: VpnRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val scope = CoroutineScope(SupervisorJob())
    private val widgetScope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }
    private val _state: MutableStateFlow<VPNState> = MutableStateFlow(
        VPNState(
            connectionState = ConnectionState.DISCONNECTED,
            peers = emptyList(),
            networks = emptyList()
        )
    )
    private val _events = MutableSharedFlow<ServiceEvent>()
    private var tunnel: Tunnel? = null
    private var isTunnelStarted = false
    private var hasAuthed = false

    // Logging
    private var engineLoggingLevel = EngineLoggingLevel.None
    private var isLiveLoggingEnabled = false
    private lateinit var liveLoggingScope: CoroutineScope
    private var liveLoggingJob = ReplaceableJob()
    private var isLoggingEnabled = false
    private var isVerboseLoggingEnabled = false
    private var fallbackDns: String? = null

    override fun onCreate() {
        super.onCreate()
        liveLoggingScope = CoroutineScope(ioDispatcher)
        tunnel = Tunnel(this, serviceStateListener, connectionListener)
        scope.launch(ioDispatcher) {
            hasAuthed = preferencesRepository.hasAuthed
            isLoggingEnabled = preferencesRepository.logging
            isVerboseLoggingEnabled = preferencesRepository.verboseLogging
            fallbackDns = preferencesRepository.fallbackDns
        }
        setEngineLoggingLevel(preferencesRepository.engineLogging, preferencesRepository.verboseEngineLogging)
        scope.launch {
            preferencesRepository.state.collectLatest {
                hasAuthed = it.hasAuthed
                isLoggingEnabled = it.logging
                isVerboseLoggingEnabled = it.verboseLogging
                setEngineLoggingLevel(it.engineLogging, it.verboseEngineLogging)
                isLiveLoggingEnabled = it.liveEngineLogging
                fallbackDns = it.fallbackDns
                if (it.liveEngineLogging) {
                    startEngineLogTailing()
                } else {
                    if (liveLoggingJob.isActive) {
                        liveLoggingJob.cancel()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        if (intent.action == Actions.TILE_STOP_SERVICE_ACTION && isTunnelStarted) {
            stop()
        }
        fun _start() {
            startInForeground()
            start(null)
        }
        if (hasAuthed) {
            when (intent.action) {
                Actions.ALWAYS_ON_START -> {
                    log("Android requesting us to start through always-on VPN", force = true)
                    _start()
                }
                Actions.BOOT_START_SERVICE_ACTION -> {
                    log("Starting tunnel after device reboot", force = true)
                    _start()
                }
                Actions.TILE_START_SERVICE_ACTION -> {
                    if (!isTunnelStarted) {
                        log("Starting tunnel from quick tile", force = true)
                        _start()
                    } else {
                        log("Requested start through quick tile but tunnel is already up", force = true)
                    }
                }
                Actions.CONTROL_ENABLE_EXIT_NODE_ACTION -> {
                    enableExitNode(intent.getStringExtra(Actions.EXTRA_EXIT_NODE_NAME))
                }
                Actions.CONTROL_DISABLE_EXIT_NODE_ACTION -> {
                    disableExitNode()
                }
                else -> {
                    log("${intent.`package` ?: "Unknown package"} (${intent.action}) tried to start tunnel - ignoring", force = true)
                }
            }
        } else {
            intent.action?.let { action ->
                log("$action requested start although user has not authed")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun enableExitNode(requestedName: String?) {
        if (!isTunnelStarted || tunnel == null) {
            log("Cannot enable exit node because the tunnel is not running", force = true)
            return
        }

        val exitRoutes = tunnel
            ?.networks()
            .toNetworkList()
            .filter { it.isExitNodeRoute() }

        if (exitRoutes.isEmpty()) {
            log("Cannot enable exit node: no default-route network is available", force = true)
            return
        }

        val selector = requestedName?.trim().orEmpty()
        val matchingRoutes = if (selector.isEmpty()) {
            exitRoutes
        } else {
            exitRoutes.filter { route ->
                route.name.equals(selector, ignoreCase = true) ||
                    route.peer.equals(selector, ignoreCase = true) ||
                    route.network.equals(selector, ignoreCase = true)
            }
        }

        if (matchingRoutes.isEmpty()) {
            log("Cannot enable exit node: no exit route matched '$selector'", force = true)
            return
        }

        // A dual-stack exit node can appear as more than one default route.
        // NetBird selects routes by their network name, so avoid duplicate calls.
        matchingRoutes
            .filterNot { it.selected }
            .map { it.name }
            .distinct()
            .forEach { routeName ->
                log("Selecting exit-node route '$routeName'", force = true)
                tunnel?.selectRoute(routeName)
            }

        _state.update { it.copy(networks = tunnel?.networks().toNetworkList()) }
    }

    private fun disableExitNode() {
        if (!isTunnelStarted || tunnel == null) {
            log("Cannot disable exit node because the tunnel is not running", force = true)
            return
        }

        val selectedExitRoutes = tunnel
            ?.networks()
            .toNetworkList()
            .filter { it.selected && it.isExitNodeRoute() }

        selectedExitRoutes
            .map { it.name }
            .distinct()
            .forEach { routeName ->
                log("Deselecting exit-node route '$routeName'", force = true)
                tunnel?.deselectRoute(routeName)
            }

        _state.update { it.copy(networks = tunnel?.networks().toNetworkList()) }
    }

    private fun Network.isExitNodeRoute(): Boolean {
        return network.contains("0.0.0.0/0") || network.contains("::/0")
    }

    override fun onBind(intent: Intent): IBinder {
        Timber.d("onBind()")
        return VPNServiceBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.d("onUnbind()")
        if (!isTunnelStarted) {
            stopSelf()
        }
        return false
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        super.onDestroy()
        stop()
    }

    override fun onRevoke() {
        log("onRevoke()", verbose = true)
        stop()
        if (isLiveLoggingEnabled && liveLoggingJob.isActive) {
            scope.launch {
                liveLoggingJob.cancel()
            }
        }
    }

    private fun setConnectionState(
        state: ConnectionState,
        clearPeers: Boolean = false
    ) {
        _state.update {
            it.copy(
                connectionState = state,
                peers = if (clearPeers) emptyList() else it.peers
            )
        }
        vpnRepository.updateConnectionState(state)
        widgetScope.launch {
            JetbirdWidget.updateAll(applicationContext)
        }
    }

    private fun broadcastStatus(connected: Boolean) {
        val statusIntent = Intent(VpnControlReceiver.ACTION_VPN_STATUS).apply {
            putExtra(VpnControlReceiver.EXTRA_CONNECTED, connected)
        }
        sendBroadcast(statusIntent)
    }

    private fun start(urlOpener: URLOpener?) {
        log("Starting NetBird", verbose = true)
        startInForeground()
        if (isLiveLoggingEnabled) {
            startEngineLogTailing()
        }
        tunnel?.start(
            urlOpener = urlOpener,
            loggingLevel = engineLoggingLevel,
            excludeApps = preferencesRepository.excludedApps,
            invertExcludeApps = preferencesRepository.invertExclusion,
            enableRouteOverrides = preferencesRepository.routeOverridesEnabled,
            overrides = preferencesRepository.routeOverrides,
            fallbackDns = fallbackDns,
            forceRelay = preferencesRepository.forceRelay,
            iceKeepAliveInterval = preferencesRepository.iceKeepAliveInterval,
            iceDisconnectedTimeout = preferencesRepository.iceDisconnectedTimeout,
            iceFailedTimeout = preferencesRepository.iceFailedTimeout
        )
    }

    private fun stop() {
        log("Requested disconnect")
        if (liveLoggingJob.isActive) {
            scope.launch {
                liveLoggingJob.cancel()
            }
        }
        tunnel?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    val tunnelBuilder: Builder
        get() = Builder()

    val isUsingPrivateDns: Boolean
        get() = tunnel?.dnsWatch?.isPrivateDnsActive ?: false

    inner class VPNServiceBinder: Binder() {
        val state: StateFlow<VPNState>
            get() = _state.asStateFlow()
        val events: SharedFlow<ServiceEvent>
            get() = _events.asSharedFlow()

        fun start(urlOpener: URLOpener?) {
            this@VPNService.start(urlOpener)
        }

        fun stop() {
            log("Stopping NetBird", verbose = true)
            tunnel?.stop()
        }

        fun selectRoute(route: String) {
            tunnel?.selectRoute(route)
            _state.update { it.copy(networks = tunnel?.networks().toNetworkList()) }
        }

        fun deselectRoute(route: String) {
            tunnel?.deselectRoute(route)
            _state.update { it.copy(networks = tunnel?.networks().toNetworkList()) }
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == LAST_CALL_TRANSACTION) {
                onRevoke()
                return true
            }
            return false
        }
    }

    private fun startInForeground() {
        log("Starting in foreground", verbose = true)
        val channelId = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(
                    channelId,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val notification = NotificationCompat.Builder(application, channelId)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(this.getString(R.string.app_name))
            .setContentText(this.getString(R.string.foreground_notification_description))
            .setContentIntent(getPendingIntent())
            .build()
        startForeground(1, notification)
    }

    private var connectionListener = object : ConnectionListener {
        override fun onConnecting() {
            log("Netbird connecting")
            setConnectionState(ConnectionState.CONNECTING)
        }

        override fun onConnected() {
            log("Netbird connected")
            setConnectionState(ConnectionState.CONNECTED)
        }

        override fun onStateChanged(state: Long) {}

        override fun onAddressChanged(fqdn: String, address: String) {
            log("Netbird address changed - our fqdn: $fqdn - our address: $address")
        }

        override fun onPeersListChanged(numberOfPeers: Long) {
            log("Netbird peers update (${numberOfPeers} peers)", verbose = true)
            val newPeers = tunnel?.peersInfo().toPeerList().sortedBy { p -> p.fqdn }
            if (_state.value.peers != newPeers) {
                _state.update { it.copy(peers = newPeers) }
            }
            val newNetworks = tunnel?.networks().toNetworkList().sortedBy { n -> n.network }
            if (_state.value.networks != newNetworks) {
                _state.update { it.copy(networks = newNetworks) }
            }
        }

        override fun onDisconnecting() {
            log("Netbird disconnecting")
            setConnectionState(ConnectionState.DISCONNECTING)
        }

        override fun onDisconnected() {
            log("Netbird disconnected")
            setConnectionState(ConnectionState.DISCONNECTED, clearPeers = true)
        }
    }

    private fun log(
        message: String,
        verbose: Boolean = false,
        force: Boolean = false
    ) {
        Timber.d(message)
        if (force) {
            return logRepository.log(message)
        }
        if (isVerboseLoggingEnabled || (isLoggingEnabled && !verbose)) {
            logRepository.log(message)
        }
    }

    private fun engineLog(
        message: String
    ) {
        logRepository.log(Log.getLogcatLineContent(message), timestamp = false)
    }

    private fun setEngineLoggingLevel(
        logging: Boolean,
        verboseLogging: Boolean
    ) {
        engineLoggingLevel = when {
            logging && verboseLogging -> EngineLoggingLevel.Trace
            logging -> EngineLoggingLevel.Info
            else -> EngineLoggingLevel.None
        }
    }

    private fun startEngineLogTailing() {
        liveLoggingJob += liveLoggingScope.launch {
            Runtime.getRuntime().exec("logcat -c")
            Runtime.getRuntime().exec("logcat -s GoLog")
                .inputStream
                .bufferedReader()
                .useLines {
                    it.forEach { l ->
                        ensureActive()
                        engineLog(l)
                    }
                }
        }
    }

    private fun reduceTunnelFailure(
        failure: String
    ) {
        when {
            NetBirdErrors.all.contains(failure) -> {
                log("NetBird error: $failure", force = true)
                scope.launch {
                    _events.emit(ServiceEvent(failure))
                }
            }
            else -> log("Tunnel failure: $failure", force = true)
        }
    }

    private var serviceStateListener = object : TunnelListener {
        override fun onStarted() {
            isTunnelStarted = true
            log("Tunnel setup complete", verbose = true)
            setConnectionState(ConnectionState.CONNECTED)
            broadcastStatus(true)
        }

        override fun onStopped() {
            isTunnelStarted = false
            log("Tunnel stopped", verbose = true)
            setConnectionState(ConnectionState.DISCONNECTED)
            stop()
            broadcastStatus(false)
        }

        override fun onLog(
            message: String,
            verbose: Boolean,
            force: Boolean
        ) {
            log(message, verbose, force)
        }

        override fun onError(message: String) {
            isTunnelStarted = false
            reduceTunnelFailure(message)
            setConnectionState(ConnectionState.DISCONNECTED)
            stop()
        }
    }

}
