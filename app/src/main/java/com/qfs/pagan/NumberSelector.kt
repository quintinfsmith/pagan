package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.BOTTOM
import android.view.Gravity.CENTER
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper


class NumberSelector(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    var min: Int = 0
    var max: Int = 1
    private var button_map = HashMap<NumberSelectorButton, Int>()
    private var active_button: NumberSelectorButton? = null
    private var on_change_hook: ((NumberSelector) -> Unit)? = null

    class NumberSelectorButton// TODO: Handle any radix
        (private var numberSelector: NumberSelector, var value: Int) :
        androidx.appcompat.widget.AppCompatTextView(
            ContextThemeWrapper(
                numberSelector.context,
                if (numberSelector.orientation == VERTICAL) {
                    R.style.numberSelectorVertical
                } else {
                    R.style.numberSelectorHorizontal
                }
            )
        ) {
        private var bkp_text: String = get_number_string(this.value, 12,1)
        private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
        private var state_active: Boolean = false

        init {
            this.text = this.bkp_text
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


        override fun onLayout(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            this.text = this.bkp_text
            this.gravity = BOTTOM
            super.onLayout(isChanged, left, top, right, bottom)
        }

        fun setActive(value: Boolean) {
            this.state_active = value
            refreshDrawableState()
        }
    }

    init {
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
        when (this.orientation) {
            VERTICAL -> {
                this.layout_vertical(isChanged, left, top, right, bottom)
            }
            HORIZONTAL -> {
                this.layout_horizontal(isChanged, left, top, right, bottom)
            }
        }
    }

    fun layout_horizontal(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (this.childCount > 0) {
            val scale = resources.displayMetrics.density
            val margin = (2 * scale + 0.5f).toInt()

            var available_space = ((right - left) - (this.paddingLeft + this.paddingRight))
            available_space -= (this.childCount - 1) * margin

            val width = available_space / this.childCount
            var remainder = available_space % this.childCount
            var total_width = 0

            for (i in 0 until this.childCount) {
                val view = this.getChildAt(i)
                var working_width = width

                if (remainder > 0) {
                    working_width += 1
                    remainder -= 1
                }

                val offset = this.paddingLeft + (i * margin) + total_width
                view.layout(
                    offset,
                    0,
                    offset + working_width,
                    bottom
                )

                total_width += working_width
            }
        }
    }

    fun layout_vertical(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (this.childCount > 0) {
            val scale = resources.displayMetrics.density
            val margin = (2 * scale + 0.5f).toInt()

            var available_space = ((bottom - top) - (this.paddingTop + this.paddingBottom))
            available_space -= (this.childCount - 1) * margin

            val height = available_space / this.childCount
            var remainder = available_space % this.childCount
            var total_height = 0

            for (i in 0 until this.childCount) {
                val view = this.getChildAt(i)
                var working_height = height

                if (remainder > 0) {
                    working_height += 1
                    remainder -= 1
                }

                val offset = this.paddingTop + (i * margin) + total_height
                view.layout(
                    0,
                    offset,
                    right,
                    offset + working_height
                )

                total_height += working_height
            }
        }
    }

    fun getState(): Int? {
        if (this.active_button == null) {
            return null
        }
        return this.active_button!!.value
    }

    fun setState(new_state: Int, manual: Boolean = false, surpress_callback: Boolean = false) {
        if (new_state < this.min || new_state > this.max) {
            throw IndexOutOfBoundsException()
        }

        for ((button, value) in this.button_map) {
            if (value == new_state) {
                this.set_active_button(button, surpress_callback)
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

    fun set_range(new_min: Int, new_max: Int) {
        val original_value = this.button_map[this.active_button]

        this.clear()
        this.min = new_min
        this.max = new_max
        this.populate()

        if (original_value != null) {
            val new_state = if ((original_value >= this.min) && (original_value <= this.max)) {
                original_value
            } else if (original_value < this.min) {
                this.min
            } else {
                this.max
            }
            this.setState(new_state, manual = true, surpress_callback = true)
        }
    }

    private fun clear() {
        this.active_button = null
        this.button_map.clear()
        this.removeAllViews()
    }

    private fun populate() {
        for (i in this.min .. this.max) {
            val currentView = NumberSelectorButton(this, i)
            if (this.orientation == VERTICAL) {
                this.addView(currentView, 0)
            } else {
                this.addView(currentView)
            }
            this.button_map[currentView] = i
        }
    }
    fun setOnChange(hook: (NumberSelector) -> Unit) {
        this.on_change_hook = hook
    }

    fun set_active_button(view: NumberSelectorButton, surpress_callback: Boolean = false) {
        if (this.active_button != view && this.active_button != null) {
            this.active_button!!.setActive(false)
        }
        this.unset_active_button()

        this.active_button = view

        if (!surpress_callback && this.on_change_hook != null) {
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
