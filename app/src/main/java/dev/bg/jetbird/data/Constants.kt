package dev.bg.jetbird.data

import android.graphics.Color
import androidx.compose.runtime.Immutable
import dev.bg.jetbird.R
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
    const val BOOT_START_SERVICE_ACTION = "dev.bg.jetbird.BootReceiver"
}

object JetBirdConstants {
    val RECONNECT_NOTIFICATION_CHANNEL = "reconnect" to "Route changes"
    val EXCEPTION_NOTIFICATION_CHANNEL = "exception" to "Crashes"
}

object NetBirdErrors {
    const val NETBIRD_TUNNEL_FAILURE = "rpc error: code = PermissionDenied desc = no peer auth method provided, please use a setup key or interactive SSO login"
    const val NETBIRD_LOGIN_EXPIRED = "rpc error: code = PermissionDenied desc = peer login has expired, please log in once more"

    val all = listOf(
        NETBIRD_TUNNEL_FAILURE,
        NETBIRD_LOGIN_EXPIRED
    )
}

object Languages {
    val resourceLanguageCodeMap = hashMapOf(
        R.string.en to "en",
        R.string.de to "de",
        R.string.ru to "ru",
        R.string.zh_CN to "zh-CN",
        R.string.cs to "cs",
        R.string.da to "da",
        R.string.et to "et",
        R.string.fi to "fi",
        R.string.hu to "hu",
        R.string.ga to "ga",
        R.string.pl to "pl",
        R.string.ro to "ro",
        R.string.sk to "sk",
        R.string.sv to "sv",
        R.string.tr to "tr",
        R.string.uk to "uk",
        R.string.zu to "zu"
    )
    val languageCodeResourceMap = resourceLanguageCodeMap
        .entries
        .associate { (k, v) -> v to k }
        .plus("zh" to R.string.zh_CN)
}
