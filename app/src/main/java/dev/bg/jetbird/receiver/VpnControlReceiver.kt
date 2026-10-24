package dev.bg.jetbird.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.repository.ProfileRepository
import dev.bg.jetbird.service.SocksService
import dev.bg.jetbird.service.VPNService
import javax.inject.Inject

@AndroidEntryPoint
class VpnControlReceiver: BroadcastReceiver() {

    @Inject
    lateinit var profileRepository: ProfileRepository

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private fun engineServiceClass() = if (preferencesRepository.socksMode) SocksService::class.java else VPNService::class.java

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CONNECT_VPN -> {
                val svcIntent = Intent(context, engineServiceClass()).apply {
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
                    Intent(context, engineServiceClass()).apply {
                        action = Actions.TILE_STOP_SERVICE_ACTION
                    }
                )
            }

            ACTION_SWITCH_PROFILE -> switchProfile(intent)

            ACTION_ENABLE_EXIT_NODE -> {
                context.startService(
                    Intent(context, engineServiceClass()).apply {
                        action = Actions.CONTROL_ENABLE_EXIT_NODE_ACTION
                        intent.getStringExtra(EXTRA_EXIT_NODE_NAME)?.let {
                            putExtra(Actions.EXTRA_EXIT_NODE_NAME, it)
                        }
                    }
                )
            }

            ACTION_DISABLE_EXIT_NODE -> {
                context.startService(
                    Intent(context, engineServiceClass()).apply {
                        action = Actions.CONTROL_DISABLE_EXIT_NODE_ACTION
                    }
                )
            }
        }
    }

    private fun switchProfile(intent: Intent) {
        val requestedId = intent.getStringExtra(EXTRA_PROFILE_ID)?.trim()
        val requestedName = intent.getStringExtra(EXTRA_PROFILE_NAME)?.trim()

        val profile = when {
            !requestedId.isNullOrEmpty() ->
                profileRepository.listProfiles().firstOrNull { it.id == requestedId }

            !requestedName.isNullOrEmpty() ->
                profileRepository.listProfiles().firstOrNull { it.name == requestedName }

            else -> null
        } ?: return

        if (profileRepository.activeProfileId() != profile.id) {
            // ProfileRepository requires the caller to stop the tunnel first.
            // For predictable automation, send DISCONNECT_VPN, then SWITCH_PROFILE,
            // then CONNECT_VPN from Tasker.
            profileRepository.switchProfile(profile.id)
        }
    }

    companion object {
        const val ACTION_CONNECT_VPN = "dev.bg.jetbird.EXTERNAL_ACTION_CONNECT_VPN"
        const val ACTION_DISCONNECT_VPN = "dev.bg.jetbird.EXTERNAL_ACTION_DISCONNECT_VPN"
        const val ACTION_SWITCH_PROFILE = "dev.bg.jetbird.EXTERNAL_ACTION_SWITCH_PROFILE"
        const val ACTION_ENABLE_EXIT_NODE = "dev.bg.jetbird.EXTERNAL_ACTION_ENABLE_EXIT_NODE"
        const val ACTION_DISABLE_EXIT_NODE = "dev.bg.jetbird.EXTERNAL_ACTION_DISABLE_EXIT_NODE"

        const val EXTRA_PROFILE_ID = "profile_id"
        const val EXTRA_PROFILE_NAME = "profile_name"
        const val EXTRA_EXIT_NODE_NAME = "exit_node_name"

        const val ACTION_VPN_STATUS = "dev.bg.jetbird.ACTION_VPN_STATUS"
        const val EXTRA_CONNECTED = "connected"
    }
}
