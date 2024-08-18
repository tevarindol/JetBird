package dev.bg.jetbird.lib

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import dev.bg.jetbird.util.ktx.addFallbackDns
import dev.bg.jetbird.util.ktx.toDnsList
import io.netbird.android.DNSList
import timber.log.Timber
import java.net.InetAddress

interface DNSListener {
    fun onNetworkChanged()
    fun onDnsChanged(dnsServers: DNSList)
}

class DNSWatch(
    context: Context,
    private val dnsListener: DNSListener
): NetworkCallback() {

    private val connectivityManager: ConnectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private var dnsServers: DNSList
    private var fallbackDns: String? = null

    init {
        dnsServers = getActiveDns()
    }

    @get:Synchronized
    var isPrivateDnsActive = false
        private set

    fun getActiveDns(): DNSList {
        val activeNetwork = connectivityManager.activeNetwork ?: return DNSList()
        val props = connectivityManager.getLinkProperties(activeNetwork) ?: return DNSList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isPrivateDnsActive = props.isPrivateDnsActive
        }
        return extendWithFallbackDNS(props.dnsServers)
    }

    fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        connectivityManager.requestNetwork(networkRequest, this)
    }

    fun engineHasStopped() {
        connectivityManager.unregisterNetworkCallback(this)
    }

    @Synchronized
    private fun onNewDNSList(linkProperties: LinkProperties) {
        val newDNSList = extendWithFallbackDNS(linkProperties.dnsServers)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isPrivateDnsActive = linkProperties.isPrivateDnsActive
        }
        if (newDNSList.size() != dnsServers.size()) {
            try {
                notifyDnsWatcher(newDNSList)
                dnsServers = newDNSList
            } catch (e: Exception) {
                Timber.e("Failed to update dns servers $e")
            }
            return
        }
        for (i in 0..<newDNSList.size()) {
            try {
                if (newDNSList[i] != dnsServers[i]) {
                    notifyDnsWatcher(newDNSList)
                    dnsServers = newDNSList
                    return
                }
            } catch (e: Exception) {
                Timber.e("Failed to update dns servers $e")
                return
            }
        }
    }

    fun updateFallbackDns(dns: String?) {
        fallbackDns = dns
    }

    private fun extendWithFallbackDNS(dnsServers: List<InetAddress>): DNSList {
        val modifiableDnsServers: List<InetAddress> = ArrayList(dnsServers)
        if (dnsServers.isEmpty()) {
            return modifiableDnsServers.toDnsList()
        }
        if (!dnsServers[0].isLinkLocalAddress) {
            return modifiableDnsServers.toDnsList()
        }
        return modifiableDnsServers.addFallbackDns(fallbackDns)
    }

    private fun notifyDnsWatcher(dnsServers: DNSList) {
        Timber.d("Received DNS update: $dnsServers")
        dnsListener.onDnsChanged(dnsServers)
    }

    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
        onNewDNSList(linkProperties)
        dnsListener.onNetworkChanged()
    }

}
