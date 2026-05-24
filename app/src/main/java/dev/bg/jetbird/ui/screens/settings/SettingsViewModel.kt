package dev.bg.jetbird.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bg.jetbird.receiver.BootReceiver
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.ktx.getProfileManager
import io.netbird.android.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val preferencesRepository: PreferencesRepository,
    private val logRepository: LogRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {

    val state = preferencesRepository.state

    fun updatePreSharedKey(
        key: String?
    ) {
        log("Updating pre-shared key to $key in config")
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.preSharedKey = key
            preferences.commit()
            preferencesRepository.preSharedKey = key
            preferencesRepository.updateState()
        }
    }

    fun toggleLazyConnections(
        enabled: Boolean
    ) {
        log(if (enabled) "Enabling lazy connections in config" else "Disabling lazy connections in config")
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.setLazyConnection(enabled)
            preferences.commit()
            preferencesRepository.lazyConnections = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleIpv6(
        enabled: Boolean
    ) {
        log(if (enabled) "Enabling IPv6" else "Disabling IPv6")
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.disableIPv6 = !enabled
            preferences.commit()
            preferencesRepository.ipv6 = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleRosenpass(
        enabled: Boolean
    ) {
        log(if (enabled) "Enabling rosenpass in config" else "Disabling rosenpass in config")
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.rosenpassEnabled = enabled
            preferences.commit()
            preferencesRepository.rosenpass = enabled
            if (!enabled) {
                toggleRosenpassPermissive(false)
            }
            preferencesRepository.updateState()
        }
    }

    fun toggleRosenpassPermissive(
        enabled: Boolean
    ) {
        log(if (enabled) "Enabling rosenpass permissive in config" else "Disabling rosenpass permissive in config")
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.rosenpassPermissive = enabled
            preferences.commit()
            preferencesRepository.rosenpassPermissive = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleLogging(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.logging = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleVerboseLogging(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.verboseLogging = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleEngineLogging(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.engineLogging = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleVerboseEngineLogging(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.verboseEngineLogging = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleLiveEngineLogging(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.liveEngineLogging = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleClientRoutes(
        enabled: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.disableClientRoutes = !enabled
            preferences.commit()
            preferencesRepository.clientRoutes = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleServerRoutes(
        enabled: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.disableServerRoutes = !enabled
            preferences.commit()
            preferencesRepository.serverRoutes = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleForceRelay(
        enabled: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.forceRelay = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleDNS(
        enabled: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.disableDNS = !enabled
            preferences.commit()
            preferencesRepository.dns = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleFirewall(
        enabled: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.disableFirewall = !enabled
            preferences.commit()
            preferencesRepository.firewall = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleSSH(
        enabled: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.serverSSHAllowed = enabled
            preferences.commit()
            preferencesRepository.ssh = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleBlockInbound(
        enabled: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val preferences = Preferences(configPath)
            preferences.blockInbound = enabled
            preferences.commit()
            preferencesRepository.blockInbound = enabled
            preferencesRepository.updateState()
        }
    }

    fun toggleReconnectOnRouteChanges(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.reconnectOnNewRoutes = enabled
            preferencesRepository.updateState()
        }
    }

    fun updateExcludedApps(packageNames: Set<String>) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.excludedApps = packageNames
            preferencesRepository.updateState()
        }
    }

    fun toggleInvertExclusion(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.invertExclusion = enabled
            preferencesRepository.updateState()
        }
    }

    fun updateFallbackDns(dns: String?) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.fallbackDns = dns
            preferencesRepository.updateState()
        }
    }

    fun toggleStartOnReboot(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.startOnReboot = enabled
            preferencesRepository.updateState()
        }
        ctx.packageManager.setComponentEnabledSetting(
            ComponentName(ctx, BootReceiver::class.java),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        log("${if (enabled) "Enabled" else "Disabled"} boot receiver", force = true)
    }

    fun resetPreferences() {
        log("Resetting JetBird preferences")
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.reset()
        }
    }

    private fun log(
        message: String,
        force: Boolean = false
    ) {
       if (force) {
           return logRepository.log(message)
       }
        if (preferencesRepository.state.value.verboseLogging) {
            logRepository.log(message)
        }
    }

}
