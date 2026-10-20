package dev.bg.jetbird.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface ConnectivityRepository {
    val isPrivateDnsActive: StateFlow<Boolean>
}

class ConnectivityRepositoryImpl @Inject constructor(
    @ApplicationContext private val ctx: Context
): ConnectivityRepository {

    private val connectivityManager = ctx.getSystemService(ConnectivityManager::class.java)

    private val _isPrivateDnsActive = MutableStateFlow(false)
    override val isPrivateDnsActive: StateFlow<Boolean> = _isPrivateDnsActive.asStateFlow()

    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .build()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = update()
        override fun onLost(network: Network) = update()
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = update()
    }

    init {
        update()
        connectivityManager.registerNetworkCallback(networkRequest, callback)
    }

    private fun update() {
        _isPrivateDnsActive.value = connectivityManager.allNetworks.any { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) {
                false
            } else {
                val properties = connectivityManager.getLinkProperties(network)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && properties?.isPrivateDnsActive == true
            }
        }
    }

}
