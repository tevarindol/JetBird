package dev.bg.jetbird

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.data.LayoutType
import dev.bg.jetbird.lib.EngineBinder
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.service.SocksService
import dev.bg.jetbird.service.VPNService
import dev.bg.jetbird.ui.Navigation
import dev.bg.jetbird.ui.theme.JetBirdTheme
import dev.bg.jetbird.util.OnLayoutChange
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

val LocalNavController = staticCompositionLocalOf<NavHostController> { error("No NavController") }
val LocalEngineBinder = staticCompositionLocalOf<EngineBinder?> { error("No engine service") }
val LocalServiceConnection = staticCompositionLocalOf<ServiceConnection> { error("No ServiceConnection") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState") }
val LocalLayoutType = staticCompositionLocalOf<LayoutType> { error("No LayoutType") }

@AndroidEntryPoint
class MainActivity: AppCompatActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private var binder by mutableStateOf<EngineBinder?>(null)
    private var boundServiceClass: Class<*>? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is EngineBinder) {
                Timber.d("Bound service")
                binder = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                preferencesRepository.state.collect {
                    val desired = if (it.socksMode) SocksService::class.java else VPNService::class.java
                    if (boundServiceClass != desired) {
                        binder?.stop()
                        if (boundServiceClass != null) {
                            unbindService(serviceConnection)
                        }
                        binder = null
                        boundServiceClass = null
                        bindEngineService()
                    }
                }
            }
        }

        setContent {
            JetBirdTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                var layoutType by remember { mutableStateOf(LayoutType.CompactPortrait) }

                OnLayoutChange {
                    layoutType = it
                }

                CompositionLocalProvider(
                    LocalNavController provides navController,
                    LocalEngineBinder provides binder,
                    LocalServiceConnection provides serviceConnection,
                    LocalSnackbar provides snackbarHostState,
                    LocalLayoutType provides layoutType
                ) {
                    Navigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (binder == null) {
            bindEngineService()
        }
    }

    override fun onDestroy() {
        if (binder != null) {
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }

    private fun engineServiceClass(): Class<*> {
        return if (preferencesRepository.socksMode) SocksService::class.java else VPNService::class.java
    }

    private fun bindEngineService() {
        val cls = engineServiceClass()
        val i = Intent(this, cls)
        startService(i)
        bindService(i, serviceConnection, Context.BIND_AUTO_CREATE)
        boundServiceClass = cls
    }

}
