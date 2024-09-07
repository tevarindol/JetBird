package dev.bg.jetbird.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.bg.jetbird.R

@Composable
fun CopyLog(
    onClick: () -> Unit
) {
    ElevatedAssistChip(
        onClick = onClick,
        label = { Text(stringResource(R.string.copy_log)) },
        colors = AssistChipDefaults.elevatedAssistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        leadingIcon = {
            Icon(
                painterResource(R.drawable.baseline_content_copy_24),
                contentDescription = stringResource(R.string.copy_log),
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}

@Composable
fun SaveLog(
    onClick: () -> Unit
) {
    ElevatedAssistChip(
        onClick = onClick,
        label = { Text(stringResource(R.string.save_log)) },
        colors = AssistChipDefaults.elevatedAssistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant),
        leadingIcon = {
            Icon(
                painterResource(R.drawable.baseline_save_24),
                contentDescription = stringResource(R.string.save_log),
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        }
    )
}
