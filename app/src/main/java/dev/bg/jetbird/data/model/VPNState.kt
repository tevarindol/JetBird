package dev.bg.jetbird.data.model

data class VPNState(
    val connectionState: ConnectionState,
    val peers: List<Peer>,
    val networks: List<Network>
)
