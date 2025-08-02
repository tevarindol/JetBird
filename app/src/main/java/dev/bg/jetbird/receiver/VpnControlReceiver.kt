package dev.bg.jetbird.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.service.VPNService

class VpnControlReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CONNECT_VPN -> {
                val svcIntent = Intent(context, VPNService::class.java).apply {
                    action = Actions.TILE_START_SERVICE_ACTION
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, svcIntent)
                } else {
                    context.startService(svcIntent)
                }
            }
            ACTION_DISCONNECT_VPN -> {
                context.startService(
                    Intent(context, VPNService::class.java).apply {
                        action = Actions.TILE_STOP_SERVICE_ACTION
                    }
                )
            }
        }
    }

    companion object {
        const val ACTION_CONNECT_VPN = "dev.bg.jetbird.EXTERNAL_ACTION_CONNECT_VPN"
        const val ACTION_DISCONNECT_VPN = "dev.bg.jetbird.EXTERNAL_ACTION_DISCONNECT_VPN"
        const val ACTION_VPN_STATUS = "dev.bg.jetbird.ACTION_VPN_STATUS"
        const val EXTRA_CONNECTED = "connected"
    }

}
