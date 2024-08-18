package dev.bg.jetbird.data.model

data class Peer(
    val ip: String,
    val fqdn: String,
    val connectionStatus: String,
    val connectionStatusUpdate: String,
    val publicKey: String,
    val relayed: Boolean,
    val direct: Boolean,
    val lastWireguardHandshake: String,
    val bytesTx: Long,
    val bytesRx: Long,
    val latency: Long,
    val rosenpassEnabled: Boolean,
    val routes: List<String>
)
