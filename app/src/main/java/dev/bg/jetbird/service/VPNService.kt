package dev.bg.jetbird.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.R
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.data.JetBirdConstants
import dev.bg.jetbird.data.NetBirdErrors
import dev.bg.jetbird.data.model.ConnectionState
import dev.bg.jetbird.data.model.Peer
import dev.bg.jetbird.data.model.ServiceEvent
import dev.bg.jetbird.data.model.VPNState
import dev.bg.jetbird.lib.EngineLoggingLevel
import dev.bg.jetbird.lib.Tunnel
import dev.bg.jetbird.lib.TunnelListener
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.repository.VpnRepository
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.Log
import dev.bg.jetbird.util.ReplaceableJob
import dev.bg.jetbird.util.ktx.getPendingIntent
import dev.bg.jetbird.util.ktx.sendNotification
import dev.bg.jetbird.util.ktx.toPeerList
import io.netbird.android.ConnectionListener
import io.netbird.android.URLOpener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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
    private val _state: MutableStateFlow<VPNState> = MutableStateFlow(
        VPNState(
            connectionState = ConnectionState.DISCONNECTED,
            peers = emptyList()
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
    private var reconnectOnRouteChanges = false

    override fun onCreate() {
        super.onCreate()
        liveLoggingScope = CoroutineScope(ioDispatcher)
        tunnel = Tunnel(this, serviceStateListener, connectionListener)
        scope.launch(ioDispatcher) {
            hasAuthed = preferencesRepository.hasAuthed
            isLoggingEnabled = preferencesRepository.logging
            isVerboseLoggingEnabled = preferencesRepository.verboseLogging
            fallbackDns = preferencesRepository.fallbackDns
            reconnectOnRouteChanges = false
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
                reconnectOnRouteChanges = it.reconnectOnNewRoutes
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
        if (intent.action == Actions.TILE_START_SERVICE_ACTION && !isTunnelStarted && hasAuthed) {
            log("Starting tunnel from quick tile")
            startInForeground()
            start(null)
        }
        if (intent.action == Actions.ALWAYS_ON_START && hasAuthed) {
            log("Android requesting us to start through always-on VPN", force = true)
            startInForeground()
            start(null)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        log("onBind()", verbose = true)
        return VPNServiceBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        log("onUnbind()", verbose = true)
        if (!isTunnelStarted) {
            stopSelf()
        }
        return false
    }

    override fun onDestroy() {
        log("onDestroy()", verbose = true)
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
            enableRouteOverrides = preferencesRepository.routeOverridesEnabled,
            overrides = preferencesRepository.routeOverrides,
            fallbackDns = fallbackDns
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

        fun getState() = _state.value

        fun getPeers(): List<Peer>? {
            return tunnel
                ?.peersInfo()
                ?.toPeerList()
                ?.sortedBy { peer -> peer.fqdn }
        }

        // These are broken
        // Can't start listener without making Netbird unfunctional - needs issue
//        fun startClientListener() {
//            tunnel?.startClientListener()
//        }
//
//        fun stopClientListener() {
//            tunnel?.stopClientListener()
//        }

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

    // THIS IS NOT IN USE
    private var connectionListener = object : ConnectionListener {
        override fun onConnecting() {
            Timber.d("Netbird connecting")
            _state.update { it.copy(connectionState = ConnectionState.CONNECTING) }
        }

        override fun onConnected() {
            Timber.d("Netbird connected")
            _state.update { it.copy(connectionState = ConnectionState.CONNECTED) }
        }

        override fun onAddressChanged(fqdn: String, address: String) {
            Timber.d("Netbird address changed - fqdn: $fqdn address: $address")
        }

        override fun onPeersListChanged(numberOfPeers: Long) {
            Timber.d("Netbird peers changed: $numberOfPeers")
            _state.update { it.copy(peers = tunnel?.peersInfo().toPeerList()) }
        }

        override fun onDisconnecting() {
            Timber.d("Netbird disconnecting")
            _state.update { it.copy(connectionState = ConnectionState.DISCONNECTING) }
        }

        override fun onDisconnected() {
            Timber.d("Netbird disconnected")
            _state.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
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
        logRepository.log(Log.getLogcatLineContent(message))
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
                log("NetBird auth error: $failure", force = true)
                scope.launch {
                    _events.emit(ServiceEvent(failure))
                }
            }
            else -> log("Tunnel failure: $failure", force = true)
        }
    }

    private fun sendRouteChangeNotification() {
        sendNotification(
            channel = JetBirdConstants.RECONNECT_NOTIFICATION_CHANNEL,
            title = getString(R.string.route_changes),
            description = getString(R.string.route_changes_description)
        )
    }

    private var serviceStateListener = object : TunnelListener {
        override fun onStarted() {
            isTunnelStarted = true
            vpnRepository.updateVpnState(true)
            log("Tunnel setup complete", verbose = true)
            _state.update { it.copy(connectionState = ConnectionState.CONNECTED) }
        }

        override fun onStopped() {
            isTunnelStarted = false
            vpnRepository.updateVpnState(false)
            log("Tunnel stopped", verbose = true)
            _state.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
            stop()
        }

        override fun onRoutesChange() {
            if (reconnectOnRouteChanges) {
                stop()
                scope.launch {
                    delay(5000L)
                    start(null)
                }
            } else {
                sendRouteChangeNotification()
            }
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
            vpnRepository.updateVpnState(false)
            reduceTunnelFailure(message)
            _state.update { it.copy(connectionState = ConnectionState.DISCONNECTED) }
            stop()
        }
    }

}
