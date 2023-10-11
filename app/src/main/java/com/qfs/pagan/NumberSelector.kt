package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import kotlin.math.roundToInt


class NumberSelector(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    var min: Int = 0
    var max: Int = 1
    private var button_map = HashMap<NumberSelectorButton, Int>()
    private var active_button: NumberSelectorButton? = null
    private var on_change_hook: ((NumberSelector) -> Unit)? = null


    // TODO: Handle any radix
    class NumberSelectorButton(private var numberSelector: NumberSelector, var value: Int):
        LinearLayout(ContextThemeWrapper( numberSelector.context, R.style.numberSelectorButton )) {
        private var bkp_text: String = get_number_string(this.value, 12,1)
        private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
        private var state_active: Boolean = false
        private val text_view = TextView(
            ContextThemeWrapper(
                numberSelector.context,
                if (numberSelector.orientation == VERTICAL) {
                    R.style.numberSelectorTextVertical
                } else {
                    R.style.numberSelectorTextHorizontal
                }
            )
        )

        init {
            this.text_view.text = this.bkp_text
            this.addView(this.text_view)
            this.text_view.layoutParams.width = MATCH_PARENT
            this.text_view.layoutParams.height = MATCH_PARENT
            if (numberSelector.orientation != VERTICAL) {
                var padding = (resources.displayMetrics.density * 3f).toInt()
                this.setPadding(0, padding, 0, padding)
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
        val orientation = this.orientation
        for (i in this.min .. this.max) {
            val currentView = NumberSelectorButton(this, i)
            if (this.orientation == VERTICAL) {
                this.addView(currentView, 0)
            } else {
                this.addView(currentView)
            }

            (currentView.layoutParams as LinearLayout.LayoutParams).apply {
                if (orientation == VERTICAL) {
                    width = MATCH_PARENT
                    height = 0
                } else {
                    height = MATCH_PARENT
                    width = 0
                }
                weight = 1F
                gravity = CENTER
            }

            (currentView.layoutParams as MarginLayoutParams).apply {
                var padding = resources.getDimension(R.dimen.normal_padding).roundToInt()
                if (orientation == VERTICAL) {
                    if (i != this@NumberSelector.max) {
                        setMargins(0, padding, 0, 0)
                    }
                } else {
                    if (i != this@NumberSelector.min) {
                        setMargins(padding, 0, 0, 0)
                    }
                }
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
