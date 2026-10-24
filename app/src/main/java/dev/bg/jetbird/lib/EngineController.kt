package dev.bg.jetbird.lib

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import dev.bg.jetbird.R
import dev.bg.jetbird.data.NetBirdErrors
import dev.bg.jetbird.data.model.ActiveEngine
import dev.bg.jetbird.data.model.ConnectionState
import dev.bg.jetbird.data.model.EngineMode
import dev.bg.jetbird.data.model.Network
import dev.bg.jetbird.data.model.ServiceEvent
import dev.bg.jetbird.data.model.VPNState
import dev.bg.jetbird.receiver.VpnControlReceiver
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.repository.VpnRepository
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.Log
import dev.bg.jetbird.util.ReplaceableJob
import dev.bg.jetbird.util.ktx.getPendingIntent
import dev.bg.jetbird.util.ktx.toNetworkList
import dev.bg.jetbird.util.ktx.toPeerList
import dev.bg.jetbird.widget.JetbirdWidget
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

class EngineController @Inject constructor(
    private val service: Service,
    private val host: TunnelHost,
    private val vpnRepository: VpnRepository,
    private val preferencesRepository: PreferencesRepository,
    private val logRepository: LogRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    val mode: EngineMode,
    private val notificationText: () -> String,
    private val onEngineStopped: () -> Unit
): TunnelListener {

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

    // Logging
    private var engineLoggingLevel = EngineLoggingLevel.None
    private var isLiveLoggingEnabled = false
    private lateinit var liveLoggingScope: CoroutineScope
    private var liveLoggingJob = ReplaceableJob()
    private var isLoggingEnabled = false
    private var isVerboseLoggingEnabled = false
    private var fallbackDns: String? = null

    val state: StateFlow<VPNState>
        get() = _state.asStateFlow()
    val events: SharedFlow<ServiceEvent>
        get() = _events.asSharedFlow()
    val isEngineStarted: Boolean
        get() = isTunnelStarted

    fun onCreate() {
        liveLoggingScope = CoroutineScope(ioDispatcher)
        tunnel = Tunnel(host, this, connectionListener)
        scope.launch(ioDispatcher) {
            isLoggingEnabled = preferencesRepository.logging
            isVerboseLoggingEnabled = preferencesRepository.verboseLogging
            fallbackDns = preferencesRepository.fallbackDns
        }
        setEngineLoggingLevel(preferencesRepository.engineLogging, preferencesRepository.verboseEngineLogging)
        scope.launch {
            preferencesRepository.state.collectLatest {
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

    fun start(urlOpener: URLOpener?) {
        log("Starting NetBird", verbose = true)
        ActiveEngine.claim(mode) { stop() }
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
            iceFailedTimeout = preferencesRepository.iceFailedTimeout,
            socksPort = if (mode == EngineMode.PROXY) preferencesRepository.socksPort else null
        )
    }

    fun stop() {
        log("Requested disconnect")
        if (liveLoggingJob.isActive) {
            scope.launch {
                liveLoggingJob.cancel()
            }
        }
        tunnel?.stop()
        ActiveEngine.release(mode)
        onEngineStopped()
    }

    fun networks(): List<Network> {
        return tunnel?.networks().toNetworkList()
    }

    fun selectRoute(route: String) {
        tunnel?.selectRoute(route)
        _state.update { it.copy(networks = tunnel?.networks().toNetworkList()) }
    }

    fun deselectRoute(route: String) {
        tunnel?.deselectRoute(route)
        _state.update { it.copy(networks = tunnel?.networks().toNetworkList()) }
    }

    fun enableExitNode(requestedName: String?) {
        if (!isTunnelStarted) {
            log("Cannot enable exit node because the tunnel is not running")
            return
        }

        val exitRoutes = networks()
            .filter { it.isExitNodeRoute() }

        if (exitRoutes.isEmpty()) {
            log("Cannot enable exit node: no default-route network is available")
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

        matchingRoutes
            .filterNot { it.selected }
            .map { it.name }
            .distinct()
            .forEach { routeName ->
                log("Selecting exit-node route '$routeName'")
                selectRoute(routeName)
            }
    }

    fun disableExitNode() {
        networks()
            .filter { it.selected && it.isExitNodeRoute() }
            .map { it.name }
            .distinct()
            .forEach { routeName ->
                log("Deselecting exit-node route '$routeName'")
                deselectRoute(routeName)
            }
    }

    private fun Network.isExitNodeRoute(): Boolean = network.contains("0.0.0.0/0") || network.contains("::/0")

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
            JetbirdWidget.updateAll(service.applicationContext)
        }
    }

    private fun broadcastStatus(connected: Boolean) {
        val statusIntent = Intent(VpnControlReceiver.ACTION_VPN_STATUS).apply {
            putExtra(VpnControlReceiver.EXTRA_CONNECTED, connected)
        }
        service.sendBroadcast(statusIntent)
    }

    private fun startInForeground() {
        log("Starting in foreground", verbose = true)
        val channelId = service.getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(
                    channelId,
                    service.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val notification = NotificationCompat.Builder(service.applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(service.getString(R.string.app_name))
            .setContentText(notificationText())
            .setContentIntent(service.getPendingIntent())
            .build()
        service.startForeground(1, notification)
    }

    private var connectionListener = object : ConnectionListener {
        override fun onConnecting() {
            log("NetBird connecting")
            setConnectionState(ConnectionState.CONNECTING)
        }

        override fun onConnected() {
            log("NetBird connected")
            setConnectionState(ConnectionState.CONNECTED)
        }

        override fun onStateChanged(state: Long) {}

        override fun onAddressChanged(fqdn: String, address: String) {
            log("NetBird address changed - our fqdn: $fqdn - our address: $address")
        }

        override fun onPeersListChanged(numberOfPeers: Long) {
            log("NetBird peers update (${numberOfPeers} peers)", verbose = true)
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
            log("NetBird disconnecting")
            setConnectionState(ConnectionState.DISCONNECTING)
        }

        override fun onDisconnected() {
            log("NetBird disconnected")
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

    fun log(message: String) {
        log(message, force = true)
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
