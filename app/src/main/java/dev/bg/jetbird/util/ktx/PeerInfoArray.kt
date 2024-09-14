package dev.bg.jetbird.util.ktx

import dev.bg.jetbird.data.model.Peer
import io.netbird.android.PeerInfoArray
import java.text.SimpleDateFormat
import java.util.Locale

fun PeerInfoArray?.toPeerList(): List<Peer> {
    if (this == null) return emptyList()
    val peers = mutableListOf<Peer>()
    for (i in 0..<size()) {
        val peer = this[i]
        peers.add(
            Peer(
                ip = peer.ip,
                fqdn = peer.fqdn,
                connectionStatus = peer.connStatus,
                connectionStatusUpdate = if (peer.connStatusUpdate > 0) SimpleDateFormat("dd'/'MM HH:mm", Locale.getDefault()).format(peer.connStatusUpdate * 1000) else "",
                publicKey = peer.pubKey,
                relayed = peer.relayed,
                relayServerAddress = peer.relayServerAddress,
                lastWireguardHandshake = peer.lastWireguardHandshake.toString(),
                bytesTx = peer.bytesTx,
                bytesRx = peer.bytesRx,
                latency = peer.latency,
                rosenpassEnabled = peer.rosenpassEnabled,
                routes = peer.routes.split(";")
            )
        )
    }
    return peers
}
