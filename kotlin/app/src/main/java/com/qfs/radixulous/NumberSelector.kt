package com.qfs.radixulous

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity.CENTER
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.internal.ViewUtils.dpToPx
import java.lang.Integer.min


class NumberSelector: LinearLayout {
    var min: Int = 0
    var max: Int = 1
    var button_map = HashMap<View, Int>()
    var active_button: View? = null
    var active_color_fg: Int = 0
    var active_color_bg: Int = 0
    var button_color_fg: Int = 0
    var button_color_bg: Int = 0

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.NumberSelector, 0, 0).apply {
            try {
                max = getInteger(R.styleable.NumberSelector_max, 2)
                min = getInteger(R.styleable.NumberSelector_min, 0)
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
        var size = this.max - this.min
        var margin = 5
        var working_width = (this.width - (this.paddingLeft + this.paddingRight))
        var inner_width = (working_width - ((size - 1) * margin)) / size
        var remainder = working_width % inner_width
        for (i in this.min .. this.max) {
            var j = i - this.min
            var button = this.getChildAt(j)
            var x = (j * (margin + inner_width)) + this.paddingLeft
            var working_width = inner_width
            if (j < remainder) {
                working_width += 1
            }

            x += min(remainder, j)
            (button as TextView).gravity = CENTER
            button.layout(x, this.paddingTop, x + working_width, bottom - this.paddingBottom)
        }
    }

    fun getState(): Int? {
        if (this.active_button == null) {
            return null
        }

        return this.button_map[this.active_button!!]!!
    }

    fun setState(new_state: Int) {
        if (new_state < this.min || new_state > this.max) {
            throw Exception("OutOfBounds")
        }

        for ((button, value) in this.button_map) {
            if (value == new_state) {
                this.set_active_button(button)
                return
            }
        }
    }

    fun populate() {
        for (i in this.min .. this.max) {
            val currentView = TextView(this.context)
            this.addView(currentView)
            // TODO: use dimens.xml (seems to be a bug treating sp as dp)
            currentView.textSize = 24F
            currentView.text = "${get_number_string(i, 12,2)}"
            currentView.setBackgroundColor(this.button_color_bg)
            currentView.setTextColor(this.button_color_fg)
            this.button_map[currentView] = i

            currentView.setOnTouchListener { view: View, _: MotionEvent ->
                this.set_active_button(view)
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