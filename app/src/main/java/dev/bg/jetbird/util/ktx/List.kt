package dev.bg.jetbird.util.ktx

import io.netbird.android.DNSList
import timber.log.Timber
import java.net.InetAddress

fun List<InetAddress>.toDnsList(): DNSList {
    val list = DNSList()
    this.forEach { list.add(it.hostAddress) }
    return list
}

fun List<InetAddress>.addFallbackDns(
    dnsHostname: String?
): DNSList {
    if (dnsHostname == null || this.isEmpty()) return toDnsList()
    if (!this[0].isLinkLocalAddress) return toDnsList()
    val dns = this.toMutableList()
    runCatching {
        Timber.d("Adding fallback DNS")
        dns.add(InetAddress.getByName(dnsHostname))
    }.onFailure {
        Timber.d("Failed to add DNS server $dnsHostname")
    }
    return dns.toDnsList()
}
