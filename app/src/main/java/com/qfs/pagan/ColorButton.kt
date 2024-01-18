package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.content.res.AppCompatResources

class ColorButton(context: Context, attrs: AttributeSet? = null): LinearLayout(context, attrs) {
    val color_square = Space(context)
    init {
        this.background = AppCompatResources.getDrawable(context, R.drawable.button)
        this.addView(this.color_square)
        this.color_square.layoutParams.height = MATCH_PARENT
        this.color_square.layoutParams.width = MATCH_PARENT
    }

    fun set_color(color: Int) {
        this.color_square.setBackgroundColor(color)
    }
}