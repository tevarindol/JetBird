package dev.bg.jetbird.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bg.jetbird.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private typealias State = ProfileContract.State

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
): ViewModel() {

    private val _state: MutableStateFlow<State> = MutableStateFlow(
        State(
            profiles = null
        )
    )
    private val _errors = MutableSharedFlow<String>()
    val state: StateFlow<State>
        get() = _state.asStateFlow()
    val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    init {
        getProfiles()
    }

    fun activeProfileId() = profileRepository.activeProfileId()

    fun addProfile(
        name: String,
        managementUrl: String
    ) {
        profileRepository.addProfile(
            name,
            managementUrl,
            onSuccess = {
                getProfiles()
            },
            onError = { error ->
                viewModelScope.launch {
                    _errors.emit(error)
                }
            }
        )
    }

    fun switchProfile(
        id: String
    ) {
        // Caller stops tunnel
        profileRepository.switchProfile(id)
        getProfiles()
    }

    fun removeProfile(
        id: String
    ) {
        profileRepository.removeProfile(id)
    }

    fun getProfiles() {
        _state.update {
            it.copy(
                profiles = profileRepository.listProfiles()
            )
        }
    }

}
