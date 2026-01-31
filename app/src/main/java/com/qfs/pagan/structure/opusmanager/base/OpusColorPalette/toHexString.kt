package com.qfs.pagan.structure.opusmanager.base.OpusColorPalette

import androidx.compose.ui.graphics.Color

fun Color.toHexString(): String {
    val alpha: Int = (this.alpha * 0xFF).toInt()
    val red: Int = (this.red * 0xFF).toInt()
    val green: Int = (this.green * 0xFF).toInt()
    val blue: Int = (this.blue * 0xFF).toInt()
    return "#%02x".format(alpha) + "%02x".format(red) + "%02x".format(green) + "%02x".format(blue)
}