package dev.bg.jetbird.lib

import android.content.Context

interface TunnelHost {
    val appContext: Context

    fun configureVpnInterface(
        ipv4Cidr: String?,
        ipv6Cidr: String?,
        mtu: Long,
        dns: String?,
        domains: String?,
        routes: String?,
        excludedApps: Set<String>,
        invertExclusion: Boolean,
        routeOverrideEnabled: Boolean,
        routeOverrides: Set<String>
    ): Long = 0L

    fun protectSocket(fd: Int): Boolean = true
}
