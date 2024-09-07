package dev.bg.jetbird.util

import android.content.Intent

fun getLogIntent(
    exception: Boolean = false
) = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "text/plain"
    putExtra(Intent.EXTRA_TITLE, "${("JetBird${if (exception) "_Crash" else ""}").replace("\\s+", "_")}_${System.currentTimeMillis() / 1000}.txt")
}
