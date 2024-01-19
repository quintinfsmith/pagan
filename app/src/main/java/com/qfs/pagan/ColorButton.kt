package com.qfs.pagan

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper

class ColorButton(context: Context, attrs: AttributeSet? = null, private var color: Int = 0): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    private var on_change: ((Int) -> Unit)? = null
    init {
        this.background = AppCompatResources.getDrawable(context, R.drawable.button)
        this.isClickable = true
        this._setup_colors()
        this.set_color(this.color)
    }

    fun get_string(): String {
        return Integer.toHexString(this.color).substring(2)
    }

    fun get_color(): Int {
        return this.color
    }

    fun set_color(color: Int, skip_callback: Boolean = false) {
        this.color = color
        this._setup_colors()
        this.refreshDrawableState()
        if (this.on_change != null && ! skip_callback) {
            this.on_change!!(color)
        }
    }

    fun set_color(color: String, skip_callback: Boolean = false) {
        this.set_color(Color.parseColor(color), skip_callback)
    }

    fun set_on_change(listener: (Int) -> Unit) {
        this.on_change = listener
    }

    private fun _setup_colors() {
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }

        for (i in 0 until (this.background as StateListDrawable).stateCount) {
            val background = ((this.background as StateListDrawable).getStateDrawable(i) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
            background?.setTint(this.color)
        }
    }
}