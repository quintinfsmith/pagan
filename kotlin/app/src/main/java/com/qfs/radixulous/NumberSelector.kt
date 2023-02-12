package com.qfs.radixulous

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity.CENTER
import android.widget.LinearLayout


class NumberSelector: LinearLayout {
    var min: Int = 0
    var max: Int = 1
    var button_map = HashMap<NumberSelectorButton, Int>()
    var active_button: NumberSelectorButton? = null
    var on_change_hook: ((NumberSelector) -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.NumberSelector, 0, 0).apply {
            try {
                max = getInteger(R.styleable.NumberSelector_max, 2)
                min = getInteger(R.styleable.NumberSelector_min, 0)
            } finally {
                recycle()
            }
        }
        this.populate()
    }

    override fun onLayout(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(isChanged, left, top, right, bottom)

        var width = ((right - left) - (this.paddingLeft + this.paddingRight)) / this.childCount
        var request = false
        for (i in 0 until this.childCount) {
            var view = this.getChildAt(i)
            view.layoutParams.width = width
            // TODO: This, the right way. i'm getting warnings
            view.requestLayout()
        }
    }

    fun getState(): Int? {
        if (this.active_button == null) {
            return null
        }
        return this.active_button!!.value
    }

    fun setState(new_state: Int, manual: Boolean = false) {
        if (new_state < this.min || new_state > this.max) {
            throw Exception("OutOfBounds")
        }

        for ((button, value) in this.button_map) {
            if (value == new_state) {
                this.set_active_button(button)
                if (manual) {
                    button.setActive(true)
                }
                return
            }
        }
    }

    fun set_max(new_max: Int) {
        this.clear()
        this.max = new_max
        this.populate()
    }

    fun set_min(new_min: Int) {
        this.clear()
        this.min = new_min
        this.populate()
    }

    fun setRange(new_min: Int, new_max: Int) {
        var original_value = this.button_map[this.active_button]

        this.clear()
        this.min = new_min
        this.max = new_max
        this.populate()

        if (original_value != null) {
            if (original_value >= this.min && original_value <= this.max) {
                this.setState(original_value)
            } else if (original_value < this.min) {
                this.setState(this.min)
            } else {
                this.setState(this.max)
            }
        }
    }

    fun clear() {
        this.active_button = null
        this.button_map.clear()
        this.removeAllViews()
    }

    fun populate() {
        for (i in this.min .. this.max) {
            val currentView = NumberSelectorButton(this, i)
            this.addView(currentView)
            this.button_map[currentView] = i
        }
    }
    fun setOnChange(hook: (NumberSelector) -> Unit) {
        this.on_change_hook = hook
    }

    fun set_active_button(view: NumberSelectorButton) {
        if (this.active_button != view && this.active_button != null) {
            this.active_button!!.setActive(false)
        }
        this.unset_active_button()

        this.active_button = view


        if (this.on_change_hook != null) {
            this.on_change_hook!!(this)
        }
    }

    fun unset_active_button() {
        if (this.active_button == null) {
            return
        }
        this.active_button!!.setActive(false)
        this.active_button = null
    }
}

class NumberSelectorButton(var numberSelector: NumberSelector, var value: Int): androidx.appcompat.widget.AppCompatTextView(numberSelector.context) {
    private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
    var state_active: Boolean = false
    init {
        // TODO: Handle any radix
        this.text = "${get_number_string(this.value, 12,2)}"
        this.gravity = CENTER
        this.background = when (this.value) {
            this.numberSelector.min -> {
                resources.getDrawable(R.drawable.ns_start)
            }
            this.numberSelector.max -> {
                resources.getDrawable(R.drawable.ns_end)
            }
            else -> {
                resources.getDrawable(R.drawable.ns_middle)
            }
        }

        this.setOnClickListener {
            this.numberSelector.set_active_button(this)
            this.setActive(true)
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (this.state_active) {
            mergeDrawableStates(drawableState, STATE_ACTIVE)
        }
        return drawableState
    }

    fun setActive(value: Boolean) {
        this.state_active = value
        refreshDrawableState()
    }
}