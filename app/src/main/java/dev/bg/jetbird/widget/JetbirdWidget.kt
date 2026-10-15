package dev.bg.jetbird.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.android.EntryPointAccessors
import dev.bg.jetbird.R
import dev.bg.jetbird.data.model.ConnectionState

object JetbirdWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val vpnRepository = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        ).vpnRepository()
        val colors = themeColors(context)

        provideContent {
            val state by vpnRepository.connectionState.collectAsState()

            GlanceTheme(colors = colors) {
                Column(
                    GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ImageButton(
                        ImageProvider(R.drawable.ic_widget_bird),
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .defaultWeight(),
                        iconSize = 72.dp,
                        onClick = actionRunCallback<ToggleVpnAction>()
                    )

                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        context.getString(state.label()),
                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                    )
                }
            }
        }
    }

    private fun themeColors(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ColorProviders(
            dynamicLightColorScheme(context),
            dynamicDarkColorScheme(context)
        )
    } else {
        ColorProviders(
            lightColorScheme(primary = Color(0xFF018BB3)),
            darkColorScheme(primary = Color(0xFF018BB3))
        )
    }

    private fun ConnectionState.label(): Int = when (this) {
        ConnectionState.CONNECTED -> R.string.connected
        ConnectionState.CONNECTING -> R.string.connecting
        ConnectionState.DISCONNECTING -> R.string.disconnecting
        ConnectionState.DISCONNECTED -> R.string.disconnected
    }

}

@Composable
private fun ImageButton(
    imageProvider: ImageProvider,
    modifier: GlanceModifier,
    iconSize: Dp,
    onClick: Action
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(GlanceTheme.colors.primary)
            .cornerRadius(16.dp)
            .clickable(onClick)
    ) {
        Image(
            provider = imageProvider,
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
            modifier = GlanceModifier.size(iconSize)
        )
    }
}
