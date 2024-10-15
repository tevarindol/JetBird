package dev.bg.jetbird.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import dev.bg.jetbird.data.Languages
import kotlin.collections.component1
import kotlin.collections.component2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLanguageDropDown(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    val ctx = LocalContext.current

    val localeList = remember {
        Languages.resourceLanguageCodeMap.map { (k, v) -> ctx.getString(k) to v }.toMap()
    }

    Box {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest
        ) {
            localeList.keys.sorted().forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeList[item]))
                        onDismissRequest()
                    }
                )
            }
        }
    }
}
