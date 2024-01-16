package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.children
import kotlin.math.roundToInt


class NumberSelector(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    var min: Int = 0
    var max: Int = 1
    var button_theme: Int = 0
    var radix: Int = 10
    private var _button_map = HashMap<NumberSelectorButton, Int>()
    private var _active_button: NumberSelectorButton? = null
    private var _on_change_hook: ((NumberSelector) -> Unit)? = null

    class NumberSelectorButton(private var _number_selector: NumberSelector, var value: Int):
        androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(_number_selector.context, R.style.numberSelectorButton)) {
        companion object {
            private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
        }

        private var _bkp_text: String = get_number_string(this.value, this._number_selector.radix,1)
        private var _state_active: Boolean = false

        init {
            this.text = this._bkp_text

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

        override fun drawableStateChanged() {
            super.drawableStateChanged()
            var state = this._number_selector.button_theme

            for (item in this.drawableState) {
                state += when (item) {
                    R.attr.state_active -> 2
                    else -> 0
                }
            }

            val activity = (this.context as ContextThemeWrapper).baseContext as MainActivity
            val palette = activity.view_model.palette!!
            val background = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)
            val stroke = (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_stroke)
            when (state) {
                0 -> {
                    background.setTint(palette.button)
                    stroke.setTint(palette.button_stroke)
                    this.setTextColor(palette.button_text)
                }
                1 -> {
                    background.setTint(palette.button_alt)
                    stroke.setTint(palette.button_alt_stroke)
                    this.setTextColor(palette.button_alt_text)
                }
                else -> {
                    background.setTint(palette.button_selected)
                    stroke.setTint(palette.button_selected_stroke)
                    this.setTextColor(palette.button_selected_text)
                }
            }
        }

        fun setActive(value: Boolean) {
            this._state_active = value
            refreshDrawableState()
        }
    }

    init {
        var styled_attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberSelector, 0, 0)
        try {
            this.button_theme = styled_attributes.getInteger(R.styleable.NumberSelector_button_theme, 0)
        } finally {
            styled_attributes.recycle()
        }

        styled_attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.Ranged, 0, 0)
        try {
            this.max = styled_attributes.getInteger(R.styleable.Ranged_max, 2)
            this.min = styled_attributes.getInteger(R.styleable.Ranged_min, 0)
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
        val margin = resources.getDimension(R.dimen.number_selector_spacing).roundToInt()
        for (i in 0 .. ((this.max - this.min) / 12)) {
            val new_linear_layout = LinearLayout(this.context)
            this.addView(new_linear_layout)

            if (this.orientation == HORIZONTAL) {
                new_linear_layout.layoutParams.width = resources.getDimension(R.dimen.base_leaf_width).roundToInt()
                new_linear_layout.layoutParams.height = MATCH_PARENT
                new_linear_layout.orientation = VERTICAL
                if (i != 0) {
                    new_linear_layout.setPadding(margin, 0, 0, 0)
                }
            } else {
                new_linear_layout.layoutParams.height = resources.getDimension(R.dimen.line_height).roundToInt()
                new_linear_layout.layoutParams.width = MATCH_PARENT
                new_linear_layout.orientation = HORIZONTAL
                if (i != 0) {
                    new_linear_layout.setPadding(0, margin, 0, 0)
                }
            }
        }

        for (i in this.min .. this.max) {
            val j = if (this.orientation == VERTICAL) {
                this.childCount - 1 - ((i - this.min) % this.childCount)
            } else {
                ((i - this.min) % this.childCount)
            }

            val currentView = NumberSelectorButton(this, i)
            if (this.orientation == HORIZONTAL) {
                (this.getChildAt(j) as ViewGroup).addView(currentView, 0)
            } else {
                (this.getChildAt(j) as ViewGroup).addView(currentView)
            }

            val layout_params = (currentView.layoutParams as LinearLayout.LayoutParams)
            layout_params.weight = 1F
            layout_params.gravity = CENTER
            if (orientation == HORIZONTAL) {
                layout_params.width = MATCH_PARENT
                layout_params.height = resources.getDimension(R.dimen.line_height).roundToInt()
            } else {
                layout_params.height = MATCH_PARENT
                layout_params.width = 0
            }
            this._button_map[currentView] = i
        }

        this.children.forEachIndexed { i: Int, row: View ->
            for (j in (row as ViewGroup).childCount - 1 downTo 1) {
                val space = Space(row.context)
                row.addView(space, j)

                if (this.orientation == HORIZONTAL) {
                    space.layoutParams.height = context.resources.getDimension(R.dimen.number_selector_spacing).roundToInt()
                    space.layoutParams.width = MATCH_PARENT
                } else {
                    space.layoutParams.width = context.resources.getDimension(R.dimen.number_selector_spacing).roundToInt()
                    space.layoutParams.height = MATCH_PARENT
                }
            }
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
