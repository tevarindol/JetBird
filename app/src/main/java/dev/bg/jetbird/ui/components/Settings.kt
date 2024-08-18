package dev.bg.jetbird.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bg.jetbird.LocalLayoutType
import dev.bg.jetbird.data.LayoutType
import dev.bg.jetbird.util.ktx.conditional

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
fun SettingsTile(
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxWidth()
            .conditional(onClick != null) {
                clickable {
                    onClick!!.invoke()
                }
            }
    ) {
        Text(title)
        Text(
            description,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun SettingsTileWithValue(
    title: String,
    description: String? = null,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val layoutType = LocalLayoutType.current

    Row(
        Modifier
            .fillMaxWidth()
            .conditional(onClick != null) {
                clickable {
                    onClick!!()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (description == null) {
            Text(
                title,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            Column {
                Text(title)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Text(
            when {
                layoutType != LayoutType.CompactPortrait -> value
                value.length > 15 -> "${value.take(8)}...${value.takeLast(8)}"
                else -> value
            },
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
fun SettingsSwitch(
    title: String,
    description: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable {
                onChange(!checked)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (description == null) {
            Text(title)
        } else {
            Column(Modifier.weight(1f)) {
                Text(title)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            thumbContent = if (checked) {
                {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            } else null,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .padding(start = 12.dp)
        )
    }
}
