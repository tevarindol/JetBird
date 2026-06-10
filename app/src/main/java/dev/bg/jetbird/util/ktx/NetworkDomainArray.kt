package dev.bg.jetbird.util.ktx

import dev.bg.jetbird.data.model.Domain
import io.netbird.android.NetworkDomains

fun NetworkDomains?.toDomains(): List<Domain> {
    if (this == null) return emptyList()
    val domains = mutableListOf<Domain>()
    for (i in 0..<size()) {
        val domain = this[i]
        domains.add(
            Domain(
                address = domain.address
            )
        )
    }
    return domains
}
