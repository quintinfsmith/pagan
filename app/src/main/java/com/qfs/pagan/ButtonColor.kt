package com.qfs.pagan

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources

class ButtonColor(context: Context, attrs: AttributeSet? = null, private var _color: Int = 0): androidx.appcompat.widget.AppCompatTextView(context, attrs) {
    private var _on_change: ((Int) -> Unit)? = null
    init {
        this.background = AppCompatResources.getDrawable(context, R.drawable.button)
        this.isClickable = true
        //this._setup_colors()
        this.set_color(this._color)
    }

    fun get_string(): String {
        return Integer.toHexString(this._color).substring(2)
    }

    fun get_color(): Int {
        return this._color
    }

    fun set_color(color: Int, skip_callback: Boolean = false) {
        this._color = color
        this.refreshDrawableState()
        if (this._on_change != null && ! skip_callback) {
            this._on_change!!(color)
        }
    }

    fun set_color(color: String, skip_callback: Boolean = false) {
        this.set_color(Color.parseColor(color), skip_callback)
    }

    fun set_on_change(listener: (Int) -> Unit) {
        this._on_change = listener
    }
}