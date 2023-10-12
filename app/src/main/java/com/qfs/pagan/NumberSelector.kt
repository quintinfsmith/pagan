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
    var radix: Int = 12
    private var _button_map = HashMap<NumberSelectorButton, Int>()
    private var _active_button: NumberSelectorButton? = null
    private var _on_change_hook: ((NumberSelector) -> Unit)? = null


    // TODO: Handle any radix
    class NumberSelectorButton(private var _number_selector: NumberSelector, var value: Int):
        LinearLayout(ContextThemeWrapper( _number_selector.context, R.style.numberSelectorButton )) {
        private var _bkp_text: String = get_number_string(this.value, this._number_selector.radix,1)
        private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
        private var _state_active: Boolean = false
        private val _text_view = TextView(
            ContextThemeWrapper(
                _number_selector.context,
                if (_number_selector.orientation == VERTICAL) {
                    R.style.numberSelectorTextVertical
                } else {
                    R.style.numberSelectorTextHorizontal
                }
            )
        )

        init {
            this._text_view.text = this._bkp_text
            this.addView(this._text_view)
            this._text_view.layoutParams.width = MATCH_PARENT
            this._text_view.layoutParams.height = MATCH_PARENT
            if (this._number_selector.orientation != VERTICAL) {
                val padding = (resources.displayMetrics.density * 3f).toInt()
                this.setPadding(0, padding, 0, padding)
            }

            this.setOnClickListener {
                this._number_selector.set_active_button(this)
                this.setActive(true)
            }
        }

        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            if (this._state_active) {
                mergeDrawableStates(drawableState, STATE_ACTIVE)
            }
            return drawableState
        }


        fun setActive(value: Boolean) {
            this._state_active = value
            refreshDrawableState()
        }
    }

    init {
        val styled_attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberSelector, 0, 0)
        try {
            this.max = styled_attributes.getInteger(R.styleable.NumberSelector_max, 2)
            this.min = styled_attributes.getInteger(R.styleable.NumberSelector_min, 0)
            this.radix = styled_attributes.getInteger(R.styleable.NumberSelector_radix, 12)
        } finally {
           styled_attributes.recycle()
        }
        this.populate()
    }

    fun getState(): Int? {
        if (this._active_button == null) {
            return null
        }
        return this._active_button!!.value
    }

    fun setState(new_state: Int, manual: Boolean = false, surpress_callback: Boolean = false) {
        if (new_state < this.min || new_state > this.max) {
            throw IndexOutOfBoundsException()
        }

        for ((button, value) in this._button_map) {
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
        val original_value = this._button_map[this._active_button]

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
        this._active_button = null
        this._button_map.clear()
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

            val layout_params = (currentView.layoutParams as LinearLayout.LayoutParams)
            layout_params.weight = 1F
            layout_params.gravity = CENTER
            if (orientation == VERTICAL) {
                layout_params.width = MATCH_PARENT
                layout_params.height = 0
            } else {
                layout_params.height = MATCH_PARENT
                layout_params.width = 0
            }

            val padding = resources.getDimension(R.dimen.normal_padding).roundToInt()
            if (orientation == VERTICAL && i != this@NumberSelector.max) {
                (layout_params as MarginLayoutParams).setMargins(0, padding, 0, 0)
            } else if (orientation != VERTICAL && i != this@NumberSelector.min) {
                (layout_params as MarginLayoutParams).setMargins(padding, 0, 0, 0)
            }

            this._button_map[currentView] = i
        }
    }

    fun setOnChange(hook: (NumberSelector) -> Unit) {
        this._on_change_hook = hook
    }

    fun set_active_button(view: NumberSelectorButton, surpress_callback: Boolean = false) {
        if (this._active_button != view && this._active_button != null) {
            this._active_button!!.setActive(false)
        }
        this.unset_active_button()

        this._active_button = view

        if (!surpress_callback && this._on_change_hook != null) {
            this._on_change_hook!!(this)
        }
    }

    fun unset_active_button() {
        if (this._active_button == null) {
            return
        }
        this._active_button!!.setActive(false)
        this._active_button = null
    }
}
