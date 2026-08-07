package dev.bg.jetbird.util.ktx

import dev.bg.jetbird.data.model.Peer
import io.netbird.android.PeerInfoArray

fun PeerInfoArray?.toPeerList(): List<Peer> {
    if (this == null) return emptyList()
    val peers = mutableListOf<Peer>()
    for (i in 0..<size()) {
        val peer = this[i]
        peers.add(
            Peer(
                ip = peer.ip,
                ipv6 = peer.iPv6,
                fqdn = peer.fqdn,
                connectionStatus = when (peer.connStatus) {
                    0L -> "Idle"
                    1L -> "Connecting"
                    2L -> "Connected"
                    else -> null
                },
                connectionStatusUpdate = peer.connStatusUpdate,
                publicKey = peer.pubKey,
                relayed = peer.relayed,
                relayServerAddress = peer.relayServerAddress,
                lastWireguardHandshake = peer.lastWireguardHandshake.toString(),
                bytesTx = peer.bytesTx,
                bytesRx = peer.bytesRx,
                latency = peer.latencyMs,
                rosenpassEnabled = peer.rosenpassEnabled,
                peerRoutes = peer.peerRoutes
            )
        )
    }
    return peers
}
