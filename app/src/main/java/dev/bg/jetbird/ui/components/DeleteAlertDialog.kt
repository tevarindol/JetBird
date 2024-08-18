package dev.bg.jetbird.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.bg.jetbird.R
import dev.bg.jetbird.data.JetBirdColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.ceil
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun DeleteAlertDialog(
    title: String,
    description: String,
    open: Boolean,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit
) {
    if (open) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(title)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(description)
                    Text(
                        stringResource(R.string.action_not_reversible),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                CountdownDeleteButton(onDelete = onDelete)
            }
        )
    }
}

@Composable
private fun CountdownDeleteButton(
    onDelete: () -> Unit
) {
    val waitTime = 3000

    var remaining by remember { mutableIntStateOf(waitTime) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val start = SystemClock.uptimeMillis()
        while (isActive && remaining > 0) {
            remaining = (waitTime - ((SystemClock.uptimeMillis() - start).coerceAtLeast(0L))).coerceAtLeast(0L).toInt()
            if (remaining == 0) {
                ready = true
            }
            delay(200.coerceAtMost(remaining).toLong())
        }
    }


    Button(
        enabled = remaining == 0,
        colors = ButtonDefaults.buttonColors(
            containerColor = JetBirdColors.ErrorRed,
            contentColor = ComposeColor.White
        ),
        onClick = onDelete
    ) {
        AnimatedContent(ready, label = "") {
            when (it) {
                true -> {
                    Text(stringResource(R.string.delete))
                }
                false -> {
                    Text(ceil(remaining.toDouble() / 1000).toInt().toString())
                }
            }
        }
    }
}
