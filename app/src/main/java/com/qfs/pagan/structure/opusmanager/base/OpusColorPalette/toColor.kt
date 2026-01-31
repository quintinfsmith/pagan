package com.qfs.pagan.structure.opusmanager.base.OpusColorPalette

import androidx.compose.ui.graphics.Color

fun String.toColor(): Color? {
    return try {
        Color(
            alpha = this.substring(1, 3).toInt(16),
            red = this.substring(3, 5).toInt(16),
            green = this.substring(5, 7).toInt(16),
            blue = this.substring(7, 9).toInt(16)
        )
    } catch (e: Exception) {
        null
    }
}