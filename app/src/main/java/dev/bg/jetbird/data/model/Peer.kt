package dev.bg.jetbird.data.model

import io.netbird.android.PeerRoutes

data class Peer(
    val ip: String,
    val fqdn: String,
    val connectionStatus: String,
    val connectionStatusUpdate: String,
    val publicKey: String,
    val relayed: Boolean,
    val relayServerAddress: String,
    val lastWireguardHandshake: String,
    val bytesTx: Long,
    val bytesRx: Long,
    val latency: Long,
    val rosenpassEnabled: Boolean,
    val peerRoutes: PeerRoutes
)
