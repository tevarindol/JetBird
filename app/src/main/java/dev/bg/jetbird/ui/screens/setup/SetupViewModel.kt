package dev.bg.jetbird.ui.screens.setup

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.ktx.getProfileManager
import io.netbird.android.Android
import io.netbird.android.ErrListener
import io.netbird.android.SSOListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val loading: StateFlow<Boolean>
        get() = _loading.asStateFlow()
    private val _ssoSupported: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val ssoSupported: StateFlow<Boolean?>
        get() = _ssoSupported.asStateFlow()
    private val _setupComplete: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val setupComplete: StateFlow<Boolean>
        get() = _setupComplete.asStateFlow()
    val errors = Channel<String>(Channel.UNLIMITED)

    fun checkManagementUrl(
        url: String
    ) {
        _loading.update { true }
        val configPath = ctx.getProfileManager().activeConfigPath
        val auth = Android.newAuth(configPath, url)
        auth.saveConfigIfSSOSupported(object : SSOListener {
            override fun onSuccess(sso: Boolean) {
                _loading.update { false }
                _ssoSupported.update { sso }
            }

            override fun onError(e: Exception?) {
                _loading.update { false }
                viewModelScope.launch { errors.send(e?.message ?: "Unknown exception") }
                Timber.d("Failed to login: $e")
            }
        })
    }

    fun updateManagementUrl(
        url: String,
        ssoSupported: Boolean
    ) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.managementUrl = url
            preferencesRepository.ssoSupported = ssoSupported
            preferencesRepository.updateState()
        }
    }

    fun updateSetupKey(
        managementUrl: String,
        setupKey: String,
        ssoSupported: Boolean = false
    ) {
        viewModelScope.launch(ioDispatcher) {
            val configPath = ctx.getProfileManager().activeConfigPath
            val auth = Android.newAuth(configPath, managementUrl)
            preferencesRepository.managementUrl = managementUrl
            preferencesRepository.setupKey = setupKey
            preferencesRepository.ssoSupported = ssoSupported
            preferencesRepository.updateState()
            auth.loginWithSetupKeyAndSaveConfig(
                object: ErrListener {
                    override fun onSuccess() {
                        _setupComplete.update { true }
                    }

                    override fun onError(e: java.lang.Exception?) {
                        viewModelScope.launch { errors.send(e?.message ?: "Unknown exception") }
                    }
                },
                setupKey,
                Build.PRODUCT
            )
        }
    }

}
