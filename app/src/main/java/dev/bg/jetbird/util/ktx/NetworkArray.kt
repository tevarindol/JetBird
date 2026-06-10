package dev.bg.jetbird.util.ktx

import dev.bg.jetbird.data.model.Network
import io.netbird.android.NetworkArray

fun NetworkArray?.toNetworkList(): List<Network> {
    if (this == null) return emptyList()
    val networks = mutableListOf<Network>()
    for (i in 0..<size()) {
        val network = this[i]
        networks.add(
            Network(
                name = network.name,
                network = network.network,
                peer = network.peer,
                status = network.status,
                selected = network.isSelected,
                domains = network.networkDomains.toDomains()
            )
        )
    }
    return networks
}
