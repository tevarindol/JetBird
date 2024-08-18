package dev.bg.jetbird.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bg.jetbird.repository.LogRepository
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.util.IoDispatcher
import io.netbird.android.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val logRepository: LogRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {

    val state = preferencesRepository.state

    fun updatePreSharedKey(
        configPath: String,
        key: String?
    ) {
        log("Updating pre-shared key to $key in config")
        viewModelScope.launch(ioDispatcher) {
            withContext(ioDispatcher) {
                val preferences = Preferences(configPath)
                preferences.preSharedKey = key
                preferences.commit()                
            }
            preferencesRepository.preSharedKey = key
            updatePreferenceState()
        }
    }

    fun toggleRosenpass(
        configPath: String,
        enabled: Boolean
    ) {
        log(if (enabled) "Enabling rosenpass in config" else "Disabling rosenpass in config")
        viewModelScope.launch(ioDispatcher) {
            withContext(ioDispatcher) {
                val preferences = Preferences(configPath)
                preferences.setRosenpass(enabled)
                preferences.commit()
            }
            preferencesRepository.rosenpass = enabled
            if (!enabled) {
                toggleRosenpassPermissive(configPath, false)
            }
            updatePreferenceState()
        }
    }

    fun toggleRosenpassPermissive(
        configPath: String,
        enabled: Boolean
    ) {
        log(if (enabled) "Enabling rosenpass permissive in config" else "Disabling rosenpass permissive in config")
        viewModelScope.launch(ioDispatcher) {
            withContext(ioDispatcher) {
                val preferences = Preferences(configPath)
                preferences.setRosenpassPermissive(enabled)
                preferences.commit()
            }
            preferencesRepository.rosenpassPermissive = enabled
            updatePreferenceState()
        }
    }

    fun toggleLogging(enabled: Boolean) {
        preferencesRepository.logging = enabled
        launchUpdatePreferenceState()
    }

    fun toggleVerboseLogging(enabled: Boolean) {
        preferencesRepository.verboseLogging = enabled
        launchUpdatePreferenceState()
    }

    fun toggleEngineLogging(enabled: Boolean) {
        preferencesRepository.engineLogging = enabled
        launchUpdatePreferenceState()
    }

    fun toggleVerboseEngineLogging(enabled: Boolean) {
        preferencesRepository.verboseEngineLogging = enabled
        launchUpdatePreferenceState()
    }

    fun toggleLiveEngineLogging(enabled: Boolean) {
        preferencesRepository.liveEngineLogging = enabled
        launchUpdatePreferenceState()
    }

    fun updateExcludedApps(packageNames: Set<String>) {
        preferencesRepository.excludedApps = packageNames
        launchUpdatePreferenceState()
    }

    fun updateFallbackDns(dns: String?) {
        preferencesRepository.fallbackDns = dns
        launchUpdatePreferenceState()
    }

    fun resetPreferences() {
        log("Resetting JetBird preferences")
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.reset()
        }
    }

    private suspend fun updatePreferenceState() {
        preferencesRepository.updateState()
    }

    private fun launchUpdatePreferenceState() {
        viewModelScope.launch(ioDispatcher) {
            updatePreferenceState()
        }
    }

    private fun log(message: String) {
        if (preferencesRepository.state.value.verboseLogging) {
            logRepository.log(message)
        }
    }

}
