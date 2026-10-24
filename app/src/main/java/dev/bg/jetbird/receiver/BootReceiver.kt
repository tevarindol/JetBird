package dev.bg.jetbird.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.service.SocksService
import dev.bg.jetbird.service.VPNService
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver: BroadcastReceiver() {

    @Inject
    lateinit var logRepository: LogRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val proxyMode = preferencesRepository.socksMode
            if (proxyMode || VpnService.prepare(context) == null) {
                val target = if (proxyMode) SocksService::class.java else VPNService::class.java
                context.startService(
                    Intent(context, target).apply {
                        action = Actions.BOOT_START_SERVICE_ACTION
                    }
                )
            } else {
                logRepository.log("JetBird could not start at boot - please allow JetBird VPN permissions by connecting using the UI")
            }
        }
    }

}
