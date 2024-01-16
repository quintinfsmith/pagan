package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.ContextThemeWrapper

open class PaganSeekBar(context: Context, attrs: AttributeSet? = null): androidx.appcompat.widget.AppCompatSeekBar(context, attrs) {
    init {
        var working_context = context
        while (working_context !is MainActivity) {
            working_context = (working_context as ContextThemeWrapper).baseContext
        }
        val palette = working_context.view_model.palette!!

        val states = arrayOf<IntArray>(
            intArrayOf(android.R.attr.state_enabled)
        )

        val colors = intArrayOf( palette.selection )
        this.thumbTintList = ColorStateList( states, colors )
        this.progressDrawable.setTintList(ColorStateList( states, colors ))

    }
}