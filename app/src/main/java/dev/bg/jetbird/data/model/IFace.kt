package dev.bg.jetbird.data.model

data class IFace(
    val ipv4Cidr: String?,
    val ipv6Cidr: String?,
    val mtu: Long,
    val dns: String?,
    val domains: String?,
    val routes: String?
) {
    fun didChange(
        newDomains: String?,
        newRoutes: String?
    ) = newDomains != domains || newRoutes != routes
}
