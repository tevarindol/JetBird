package dev.bg.jetbird.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.service.VPNService
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver: BroadcastReceiver() {

    @Inject
    lateinit var logRepository: LogRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (VpnService.prepare(context) == null) {
                context.startService(
                    Intent(context, VPNService::class.java).apply {
                        action = Actions.BOOT_START_SERVICE_ACTION
                    }
                )
            } else {
                logRepository.log("JetBird could not start at boot - please allow JetBird VPN permissions by connecting using the UI")
            }
        }
    }

}
