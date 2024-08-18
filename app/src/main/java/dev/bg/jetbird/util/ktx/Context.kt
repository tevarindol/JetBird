package dev.bg.jetbird.util.ktx

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

fun Context.getConfigPath(): String = "${filesDir.path}/netbird.cfg"

fun Context.hasOtherAlwaysOnVpn(): Boolean {
    val connectivityManager = (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
    val network = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        network.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                network.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED) &&
                network.hasCapability(NetworkCapabilities.NET_CAPABILITY_FOREGROUND)
    } else {
        network.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                network.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    }
}
