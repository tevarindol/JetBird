package dev.bg.jetbird.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.R
import dev.bg.jetbird.data.Actions
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
import io.netbird.android.URLOpener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@AndroidEntryPoint
class SocksService: Service(), TunnelHost {

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
            mode = EngineMode.PROXY,
            notificationText = { getString(R.string.foreground_notification_description_socks, preferencesRepository.socksPort) },
            onEngineStopped = {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        )
        controller.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }
        if (preferencesRepository.hasAuthed) {
            when (intent.action) {
                Actions.TILE_START_SERVICE_ACTION -> {
                    if (!controller.isEngineStarted) {
                        controller.start(null)
                    }
                }
                Actions.BOOT_START_SERVICE_ACTION -> {
                    controller.start(null)
                }
                Actions.TILE_STOP_SERVICE_ACTION -> {
                    if (controller.isEngineStarted) {
                        controller.stop()
                    }
                }
                Actions.CONTROL_ENABLE_EXIT_NODE_ACTION -> {
                    controller.enableExitNode(intent.getStringExtra(Actions.EXTRA_EXIT_NODE_NAME))
                }
                Actions.CONTROL_DISABLE_EXIT_NODE_ACTION -> {
                    controller.disableExitNode()
                }
                else -> {}
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return SocksBinder()
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (!controller.isEngineStarted) {
            stopSelf()
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::controller.isInitialized) {
            controller.stop()
        }
    }

    inner class SocksBinder: Binder(), EngineBinder {
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
    }

}
