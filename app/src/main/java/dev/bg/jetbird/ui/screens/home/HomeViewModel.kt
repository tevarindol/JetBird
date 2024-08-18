package dev.bg.jetbird.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.util.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {
    fun hasAuthed() = preferencesRepository.hasAuthed

    fun getSetupKey() = preferencesRepository.setupKey

    fun updateHasAuthed() {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.hasAuthed = true
            preferencesRepository.updateState()
        }
    }
}
