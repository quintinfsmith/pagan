package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper

open class PaganRadioButton(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatRadioButton(context, attrs) {
    init {
        var working_context = context
        while (working_context !is MainActivity) {
            working_context = (working_context as ContextThemeWrapper).baseContext
        }
        val color_map = working_context.view_model.color_map

        val states = arrayOf<IntArray>(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked),

        )
        val fg = Color.valueOf(color_map[Palette.Foreground])
        val bg = Color.valueOf(color_map[Palette.Background])
        val unchecked_color = Color.valueOf(
            (fg.red() * .4f) + (bg.red() * .6f),
            (fg.green() * .4f) + (bg.green() * .6f),
            (fg.blue() * .4f) + (bg.blue() * .6f)
        ).toArgb()

        val colors = intArrayOf( color_map[Palette.Leaf], unchecked_color )
        this.buttonTintList = ColorStateList( states, colors )
        this.setTextColor(color_map[Palette.Foreground])

    }
}