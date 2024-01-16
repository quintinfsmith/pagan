package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat

open class PaganSwitch(context: Context, attrs: AttributeSet? = null): SwitchCompat(context, attrs) {
    init {
        var working_context = context
        while (working_context !is MainActivity) {
            working_context = (working_context as ContextThemeWrapper).baseContext
        }
        val palette = working_context.view_model.palette!!

        val states = arrayOf<IntArray>(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        val fg = Color.valueOf(palette.foreground)
        val bg = Color.valueOf(palette.background)
        val colors = intArrayOf(
            palette.selection,
            Color.valueOf(
                (fg.red() + bg.red()) / 2f,
                (fg.green() + bg.green()) / 2f,
                (fg.blue() + bg.blue()) / 2f
            ).toArgb()
        )
        this.thumbTintList = ColorStateList( states, colors )
        this.trackDrawable.setTintList(ColorStateList( states, colors ))
    }
}