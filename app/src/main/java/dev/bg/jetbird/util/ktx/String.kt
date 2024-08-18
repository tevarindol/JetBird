package dev.bg.jetbird.util.ktx

import android.content.Context
import dev.bg.jetbird.R

fun String?.toStringOrNotSet(
    ctx: Context
) = this ?: ctx.getString(R.string.not_set)
