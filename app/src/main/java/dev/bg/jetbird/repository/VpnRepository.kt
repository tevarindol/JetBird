package dev.bg.jetbird.repository

import javax.inject.Inject

interface VpnRepository {
    fun isVpnConnected(): Boolean
    fun updateVpnState(connected: Boolean)
}

class VpnRepositoryImpl @Inject constructor(): VpnRepository {
    private var _isVpnConnected = false

    override fun isVpnConnected() = _isVpnConnected

    override fun updateVpnState(connected: Boolean) {
        _isVpnConnected = connected
    }
}
