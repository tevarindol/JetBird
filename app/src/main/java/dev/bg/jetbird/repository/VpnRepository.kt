package dev.bg.jetbird.repository

import dev.bg.jetbird.data.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface VpnRepository {
    val connectionState: StateFlow<ConnectionState>
    fun isVpnConnected(): Boolean
    fun updateConnectionState(state: ConnectionState)
}

class VpnRepositoryImpl @Inject constructor(): VpnRepository {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)

    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    override fun isVpnConnected() = _connectionState.value == ConnectionState.CONNECTED

    override fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }
}
