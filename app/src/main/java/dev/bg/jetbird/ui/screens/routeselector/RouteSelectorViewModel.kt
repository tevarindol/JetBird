package dev.bg.jetbird.ui.screens.routeselector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.bg.jetbird.repository.PreferencesRepository
import dev.bg.jetbird.util.DefaultDispatcher
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.calculateTunneledRoutes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RouteSelectorViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
): ViewModel() {

    val state = preferencesRepository.state
    private val _calculatedRoutes: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val calculatedRoutes: StateFlow<Set<String>>
        get() = _calculatedRoutes.asStateFlow()

    init {
        viewModelScope.launch {
            calculateRoutes()
        }
    }

    fun toggleRouteOverridesEnabled(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            preferencesRepository.routeOverridesEnabled = enabled
            preferencesRepository.updateState()
        }
    }

    fun addRoute(route: String) {
        viewModelScope.launch {
            val routes = preferencesRepository
                .routeOverrides
                .toMutableSet()
                .apply {
                    add(route)
                }
            preferencesRepository.routeOverrides = routes
            preferencesRepository.updateState()
            calculateRoutes(routes)
        }
    }

    fun removeRoute(route: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val routes = preferencesRepository
                    .routeOverrides
                    .toMutableSet()
                    .apply {
                        removeIf { r -> r == route }
                    }
                preferencesRepository.routeOverrides = routes
                preferencesRepository.updateState()
                calculateRoutes(routes)
            }
        }
    }

    private suspend fun calculateRoutes(
        routes: Set<String>? = null
    ) {
        if (routes == null) {
            var r: Set<String>
            withContext(ioDispatcher) {
                r = preferencesRepository.routeOverrides
            }
            withContext(defaultDispatcher) {
                _calculatedRoutes.update { calculateTunneledRoutes(setOf("0.0.0.0/0", "::/0"), r) }
            }
        } else {
            withContext(defaultDispatcher) {
                _calculatedRoutes.update { calculateTunneledRoutes(setOf("0.0.0.0/0", "::/0"), routes) }
            }
        }
    }

}
