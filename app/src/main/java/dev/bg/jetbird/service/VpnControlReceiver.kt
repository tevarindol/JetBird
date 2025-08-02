package dev.bg.jetbird.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.BuildConfig
import timber.log.Timber

class VpnControlReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CONNECT_VPN = "dev.bg.jetbird.EXTERNAL_ACTION_CONNECT_VPN"
        const val ACTION_DISCONNECT_VPN = "dev.bg.jetbird.EXTERNAL_ACTION_DISCONNECT_VPN"
        const val EXTRA_TOKEN = "dev.bg.jetbird.EXTRA_TOKEN"
        const val ACTION_VPN_STATUS = "dev.bg.jetbird.ACTION_VPN_STATUS"
        const val EXTRA_CONNECTED = "connected"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CONNECT_VPN -> {
                val svcIntent = Intent(context, VPNService::class.java).apply {
                    action = Actions.TILE_START_SERVICE_ACTION
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, svcIntent)
                } else {
                    context.startService(svcIntent)
                }
            }
            ACTION_DISCONNECT_VPN -> {
                val svcIntent = Intent(context, VPNService::class.java).apply {
                    action = Actions.TILE_STOP_SERVICE_ACTION
                }
                context.startService(svcIntent)
            }
        }
    }
}

