package com.qfs.pagan.structure.opusmanager.base.OpusColorPalette

import androidx.compose.ui.graphics.Color

interface ColorPalettable {
    val palette: OpusColorPalette
    fun set_event_color(color: Color?) {
        this.palette.event = color
    }
    fun set_event_bg_color(color: Color?) {
        this.palette.event_bg = color
    }
    fun set_effect_color(color: Color?) {
        this.palette.effect = color
    }
    fun set_effect_bg_color(color: Color?) {
        this.palette.effect_bg = color
    }
}