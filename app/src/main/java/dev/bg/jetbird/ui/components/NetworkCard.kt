package dev.bg.jetbird.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bg.jetbird.R
import dev.bg.jetbird.data.JetBirdColors
import dev.bg.jetbird.util.ktx.conditional

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NetworkCard(
    title: String,
    description: String,
    connectionStatus: String,
    isSelected: Boolean,
    modifier: Modifier,
) {
    val c = MaterialTheme.colorScheme.primary

    Card(
        modifier
            .conditional(isSelected) {
                clip(RoundedCornerShape(12.dp))
                border(1.dp, c, RoundedCornerShape(12.dp))
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title)
                Text(description)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = when (connectionStatus) {
                        "Connected" -> stringResource(R.string.connected)
                        "Disconnected" -> stringResource(R.string.disconnected)
                        "Connecting" -> stringResource(R.string.connecting)
                        "Idle" -> stringResource(R.string.idle)
                        else -> stringResource(R.string.unknown)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Canvas(
                    Modifier.size(16.dp)
                ) {
                    drawCircle(
                        color = when (connectionStatus) {
                            "Connected" -> JetBirdColors.IndicatorColors.Connected
                            "Disconnected" -> JetBirdColors.IndicatorColors.Disconnected
                            "Idle",
                            "Connecting" -> JetBirdColors.IndicatorColors.Connecting
                            else -> JetBirdColors.IndicatorColors.Disconnected
                        }
                    )
                }
            }
        }
    }
}
