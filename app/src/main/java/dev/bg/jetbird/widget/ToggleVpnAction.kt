package dev.bg.jetbird.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.data.model.ConnectionState
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.repository.VpnRepository
import dev.bg.jetbird.service.VPNService

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun vpnRepository(): VpnRepository
    fun preferencesRepository(): PreferencesRepository
}

class ToggleVpnAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        if (!entry.preferencesRepository().hasAuthed) {
            openApp(context)
            return
        }
        VpnService.prepare(context)?.let { intent ->
            runCatching {
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                ).send()
            }.onFailure { openApp(context) }
            return
        }
        val serviceIntent = Intent(context, VPNService::class.java)
        when (entry.vpnRepository().connectionState.value) {
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> return
            ConnectionState.CONNECTED -> context.startService(
                serviceIntent.apply { action = Actions.TILE_STOP_SERVICE_ACTION }
            )
            ConnectionState.DISCONNECTED -> ContextCompat.startForegroundService(
                context,
                serviceIntent.apply { action = Actions.TILE_START_SERVICE_ACTION }
            )
        }
    }

    private fun openApp(context: Context) {
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

}
