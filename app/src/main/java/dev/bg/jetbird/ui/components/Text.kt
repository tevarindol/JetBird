package dev.bg.jetbird.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign

@Composable
fun MonospaceText(
    text: String,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        textAlign = textAlign,
        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
    )
}
