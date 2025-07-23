package dev.bg.jetbird.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bg.jetbird.R

@Composable
fun OutlinedCopyButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick
    ) {
        Icon(
            painterResource(R.drawable.baseline_content_copy_24),
            contentDescription = stringResource(R.string.copy),
            modifier = Modifier.size(AssistChipDefaults.IconSize)
        )
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.copy))
    }
}
