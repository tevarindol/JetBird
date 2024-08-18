package dev.bg.jetbird.data

import android.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color as ComposeColor

@Immutable
object JetBirdColors {
    val ErrorRed = ComposeColor(Color.parseColor("#F6685E"))
    object IndicatorColors {
        val Connected = ComposeColor(Color.parseColor("#CDFAD5"))
        val Connecting = ComposeColor(Color.parseColor("#FFCF96"))
        val Disconnected = ComposeColor(Color.parseColor("#FF8080"))
    }
}

object Actions {
    const val ALWAYS_ON_START = "android.net.VpnService"
    const val TILE_START_SERVICE_ACTION = "dev.bg.jetbird.Tile.Start"
    const val TILE_STOP_SERVICE_ACTION = "dev.bg.jetbird.Tile.Stop"
}

object NetBirdErrors {
    const val NETBIRD_TUNNEL_FAILURE = "rpc error: code = PermissionDenied desc = no peer auth method provided, please use a setup key or interactive SSO login"

    val all = listOf(NETBIRD_TUNNEL_FAILURE)
}
