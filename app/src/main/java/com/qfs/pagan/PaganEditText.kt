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
        val palette = context.view_model.palette!!

        val states = arrayOf<IntArray>(
            intArrayOf(android.R.attr.state_focused),
            intArrayOf(-android.R.attr.state_focused)
        )
        val colors = intArrayOf(
            palette.selection,
            palette.foreground
        )

        this.setTextColor(palette.foreground)

        this.background.setTintList(ColorStateList(states, colors))
        this.textCursorDrawable?.setTint(palette.selection)
        this.textSelectHandle?.setTint(palette.selection)
        this.textSelectHandleLeft?.setTint(palette.selection)
        this.textSelectHandleRight?.setTint(palette.selection)
        val selection_color = Color.valueOf(palette.selection)
        val bg = Color.valueOf(palette.background)
        this.highlightColor = Color.valueOf(
            (selection_color.red() * .4f) + (bg.red() * .6f),
            (selection_color.green() * .4f) + (bg.green() * .6f),
            (selection_color.blue() * .4f) + (bg.blue() * .6f)
        ).toArgb()

    }
}