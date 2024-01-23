package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.appcompat.widget.SwitchCompat
import com.qfs.pagan.ColorMap.Palette

open class PaganSwitch(context: Context, attrs: AttributeSet? = null): SwitchCompat(context, attrs) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        var working_context = context
        while (working_context !is MainActivity) {
            working_context = (working_context as ContextThemeWrapper).baseContext
        }
        val color_map = working_context.view_model.color_map

        val states = arrayOf<IntArray>(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        val fg = Color.valueOf(color_map[Palette.Foreground])
        val bg = Color.valueOf(color_map[Palette.Background])
        val colors_track = intArrayOf(
            color_map[Palette.Leaf],
            Color.valueOf(
                (fg.red() * .4f) + (bg.red() * .6f),
                (fg.green() * .4f) + (bg.green() * .6f),
                (fg.blue() * .4f) + (bg.blue() * .6f)
            ).toArgb()
        )
        val colors_thumb = intArrayOf(
            color_map[Palette.Leaf],
            Color.valueOf(
                (fg.red() * .6f) + (bg.red() * .4f),
                (fg.green() * .6f) + (bg.green() * .4f),
                (fg.blue() * .6f) + (bg.blue() * .4f),
            ).toArgb()
        )
        this.thumbTintList = ColorStateList( states, colors_thumb )
        this.trackDrawable.setTintList(ColorStateList( states, colors_track ))
    }

}