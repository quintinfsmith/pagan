package com.qfs.radixulous

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity.CENTER
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.lang.Integer.max
import java.lang.Integer.min


class NumberSelector: LinearLayout {
    var size: Int = 1
    var button_map = HashMap<View, Int>()
    var active_button: View? = null
    var active_color_fg: Int = 0
    var active_color_bg: Int = 0
    var button_color_fg: Int = 0
    var button_color_bg: Int = 0

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.NumberSelector, 0, 0).apply {
            try {
                size = getInteger(R.styleable.NumberSelector_size, 2)
                active_color_bg = getColor(R.styleable.NumberSelector_active_bg, 0)
                active_color_fg = getColor(R.styleable.NumberSelector_active_fg, 0)
                button_color_bg = getColor(R.styleable.NumberSelector_button_bg, 0)
                button_color_fg = getColor(R.styleable.NumberSelector_button_fg, 0)
            } finally {
                recycle()
            }
        }
        this.populate()
    }

    override fun onLayout(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(isChanged, left, top, right, bottom)
        var margin = 5
        var inner_width = ((right - left) - ((this.size - 1) * margin)) / this.size
        var remainder = (right - left) % inner_width
        for (i in 0 until this.size) {
            var button = this.getChildAt(i)
            var x = i * (margin + inner_width)
            var working_width = inner_width
            if (i < remainder) {
                working_width += 1
            }

            x += min(remainder, i)
            (button as TextView).gravity = CENTER
            button.layout(x, 0, x + working_width, bottom)
        }
    }

    fun getState(): Int {
        if (this.active_button == null) {
            return 0
        }

        return this.button_map[this.active_button!!]!!
    }

    fun populate() {
        for (i in 0 until this.size) {
            var currentView = TextView(this.context)
            this.addView(currentView)
            currentView.text = "${get_number_string(i, 12,2)}"
            currentView.setBackgroundColor(this.button_color_bg)
            currentView.setTextColor(this.button_color_fg)
            this.button_map[currentView] = i

            var that = this
            currentView.setOnTouchListener { view: View, _: MotionEvent ->
                that.set_active_button(view)
                false
            }
        }
    }

    fun set_active_button(view: View) {
        this.unset_active_button()
        this.active_button = view
        view.setBackgroundColor(this.active_color_bg)
        (view as TextView).setTextColor(this.active_color_fg)
    }

    fun unset_active_button() {
        if (this.active_button == null) {
            return
        }
        this.active_button!!.setBackgroundColor(this.button_color_bg)
        (this.active_button as TextView).setTextColor(this.button_color_fg)
        this.active_button = null

    }
}