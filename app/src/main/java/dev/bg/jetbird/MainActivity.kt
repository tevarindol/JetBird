package dev.bg.jetbird

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.data.LayoutType
import dev.bg.jetbird.service.VPNService
import dev.bg.jetbird.ui.Navigation
import dev.bg.jetbird.ui.theme.JetBirdTheme
import dev.bg.jetbird.util.OnLayoutChange
import dev.bg.jetbird.util.ktx.hasOtherAlwaysOnVpn
import timber.log.Timber

val LocalNavController = staticCompositionLocalOf<NavHostController> { error("No NavController") }
val LocalVPNServiceBinder = staticCompositionLocalOf<VPNService.VPNServiceBinder?> { error("No VPNService") }
val LocalServiceConnection = staticCompositionLocalOf<ServiceConnection> { error("No ServiceConnection") }
val LocalSnackbar = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState") }
val LocalLayoutType = staticCompositionLocalOf<LayoutType> { error("No LayoutType") }

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var binder by mutableStateOf<VPNService.VPNServiceBinder?>(null)
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is VPNService.VPNServiceBinder) {
                Timber.d("Bound service")
                binder = service
//                binder?.startClientListener()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
//            binder?.stopClientListener()
            binder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val intent = VpnService.prepare(this);
        if (!hasOtherAlwaysOnVpn() && intent != null) {
            Timber.d("JetBird has all permissions - auto-starting service")
            val i = Intent(this, VPNService::class.java)
            startService(i)
            bindService(i, serviceConnection, Context.BIND_AUTO_CREATE)
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
                    LocalVPNServiceBinder provides binder,
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
        // TODO check if running
        if (binder == null) {
            bindService(
                Intent(this, VPNService::class.java),
                serviceConnection, Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onDestroy() {
        if (binder != null) {
            unbindService(serviceConnection)
        }
        super.onDestroy()
    }

}
