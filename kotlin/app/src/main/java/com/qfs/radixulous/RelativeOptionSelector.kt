package com.qfs.radixulous

import android.content.Context
import android.opengl.Visibility
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.widget.LinearLayout
import android.widget.TextView
import java.lang.Integer.min


class RelativeOptionSelector: LinearLayout {
    var active_button: View? = null
    var active_color_fg: Int = 0
    var active_color_bg: Int = 0
    var button_color_fg: Int = 0
    var button_color_bg: Int = 0
    var button_map = HashMap<View, Int>()
    var itemList: List<Int> = listOf(
        R.string.pfx_add,
        R.string.pfx_subtract,
        R.string.pfx_pow,
        R.string.pfx_log
    )
    private var hidden_options: MutableSet<Int> = mutableSetOf()
    var on_change_hook: ((RelativeOptionSelector) -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.NumberSelector, 0, 0).apply {
            try {
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
        val size = this.itemList.size - this.hidden_options.size
        val margin = 5
        val _width = (this.width - (this.paddingLeft + this.paddingRight))
        val inner_width = (_width - ((size - 1) * margin)) / size
        val remainder = _width % inner_width

        var i = 0
        for (j in 0 until this.childCount) {
            val button = this.getChildAt(j)
            if (this.hidden_options.contains(j)) {
                button.visibility = View.GONE
                continue
            } else {
                button.visibility = View.VISIBLE
            }

            var x = (i * (margin + inner_width)) + this.paddingLeft
            var working_width = inner_width
            if (i < remainder) {
                working_width += 1
            }

            x += min(remainder, i)
            (button as TextView).gravity = CENTER
            button.layout(x, this.paddingTop, x + working_width, bottom - this.paddingBottom)
            i += 1
        }
    }

    fun getState(): Int? {
        if (this.active_button == null) {
            return null
        }
        return this.button_map[this.active_button!!]!!
    }

    fun setState(new_state: Int) {
        if (new_state >= this.itemList.size) {
            throw Exception("Not an option")
        }

        for ((button, value) in this.button_map) {
            if (value == new_state) {
                this.set_active_button(button)
                return
            }
        }
    }

    fun clear() {
        this.active_button = null
        this.button_map.clear()
        this.removeAllViews()
    }

    fun populate() {
        this.itemList.forEachIndexed { i, string_index ->
            val currentView = TextView(this.context)
            this.addView(currentView)

            // TODO: use dimens.xml (seems to be a bug treating sp as dp)
            currentView.textSize = 24F
            currentView.text = resources.getString(string_index)
            currentView.setBackgroundColor(this.button_color_bg)
            currentView.setTextColor(this.button_color_fg)
            this.button_map[currentView] = i

            currentView.setOnTouchListener { view: View, motionEvent: MotionEvent ->
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    this.set_active_button(view)
                    if (this.on_change_hook != null) {
                        this.on_change_hook!!(this)
                    }
                }
                true
            }
        }
    }

    fun setOnChange(hook: (RelativeOptionSelector) -> Unit) {
        this.on_change_hook = hook
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

    fun hideOption(index: Int) {
        this.hidden_options.add(index)
        for ((view, i) in this.button_map) {
            if (i == index) {
                view.visibility = View.GONE
            }
        }
    }
}