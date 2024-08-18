package dev.bg.jetbird.ui.screens.credits

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bg.jetbird.R

const val GPL_V2 = "GNU General Public License v2.0"
const val APACHE_V2 = "Apache License, Version 2.0"
const val MIT = "MIT License"
const val BSD = "BSD 3-Clause License"

@Composable
fun CreditsScreen() {
    val libraries = listOf(
        Library("Kotlin", APACHE_V2, "https://kotlinlang.org"),
        Library("AndroidX / Jetpack", APACHE_V2, "https://github.com/androidx/androidx"),
        Library("Desugar JDK", GPL_V2, "https://github.com/google/desugar_jdk_libs"),
        Library("NetBird", BSD, "https://github.com/netbirdio/netbird"),
        Library("Dagger", APACHE_V2, "https://dagger.dev"),
        Library("MMKV", BSD, "https://github.com/Tencent/MMKV"),
        Library("Timber", APACHE_V2, "https://github.com/JakeWharton/timber"),
        Library("LeakCanary", APACHE_V2, "https://square.github.io/leakcanary"),
        Library("IPAddress", APACHE_V2, "https://github.com/seancfoley/IPAddress")
    )

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.credits),
                fontSize = 36.sp,
                fontWeight = FontWeight.W400
            )
            Spacer(Modifier.size(16.dp))
        }
        itemsIndexed(libraries) { idx, library ->
            LibraryItem(library)
            Spacer(Modifier.size(8.dp))
            if (idx != libraries.size - 1) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun LibraryItem(
    library: Library
) {
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier
            .fillMaxWidth()
            .clickable {
                uriHandler.openUri(library.url)
            }
    ) {
        Text(library.title)
        Text(
            library.license,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private data class Library(
    val title: String,
    val license: String,
    val url: String
)
