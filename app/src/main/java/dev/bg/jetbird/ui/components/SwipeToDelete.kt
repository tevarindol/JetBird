package dev.bg.jetbird.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import dev.bg.jetbird.data.JetBirdColors

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SwipeToDelete(
    modifier: Modifier,
    onDeleted: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDeleted()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val dir = dismissState.dismissDirection
            val color by animateColorAsState(
                JetBirdColors.ErrorRed.copy(alpha = (dismissState.progress * 2).coerceAtMost(1f)),
                label = ""
            )
            val scale by animateFloatAsState(
                (0.5f + (dismissState.progress * 2)).coerceAtMost(1f),
                label = ""
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, CardDefaults.elevatedShape)
                    .padding(horizontal = 20.dp),
                contentAlignment = when (dir) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }
            ) {
                Icon(
                    when (dir) {
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                        else -> Icons.Default.Delete
                    },
                    contentDescription = null,
                    modifier = Modifier.scale(scale)
                )
            }
        },
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        content()
    }
}
