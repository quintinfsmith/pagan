package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper

open class PaganEditText(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatEditText(context, attrs) {
    init {
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }
        val color_map = context.view_model.color_map

        val states = arrayOf<IntArray>(
            intArrayOf(android.R.attr.state_focused),
            intArrayOf(-android.R.attr.state_focused)
        )
        val colors = intArrayOf(
            color_map[Palette.Selection],
            color_map[Palette.Foreground]
        )

        this.setTextColor(color_map[Palette.Foreground])

        this.background.setTintList(ColorStateList(states, colors))
        this.textCursorDrawable?.setTint(color_map[Palette.Selection])
        this.textSelectHandle?.setTint(color_map[Palette.Selection])
        this.textSelectHandleLeft?.setTint(color_map[Palette.Selection])
        this.textSelectHandleRight?.setTint(color_map[Palette.Selection])
        val selection_color = Color.valueOf(color_map[Palette.Selection])
        val bg = Color.valueOf(color_map[Palette.Background])
        this.highlightColor = Color.valueOf(
            (selection_color.red() * .4f) + (bg.red() * .6f),
            (selection_color.green() * .4f) + (bg.green() * .6f),
            (selection_color.blue() * .4f) + (bg.blue() * .6f)
        ).toArgb()

    }
}