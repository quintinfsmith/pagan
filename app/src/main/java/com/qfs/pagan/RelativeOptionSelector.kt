package com.qfs.pagan

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper


class RelativeOptionSelector(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    class InvalidOptionException(option: Int): Exception("Invalid Option selected: $option")
    private var _active_button: RelativeOptionSelectorButton? = null
    private var _button_map = HashMap<RelativeOptionSelectorButton, Int>()
    private var _item_list: List<Int> = listOf(
        R.string.absolute_label,
        R.string.pfx_add,
        R.string.pfx_subtract
    )
    private var _on_change_hook: ((RelativeOptionSelector) -> Unit)? = null

    @SuppressLint("ViewConstructor")
    class RelativeOptionSelectorButton (
        private var _relative_option_selector: RelativeOptionSelector,
        private var _value: Int
    ) : androidx.appcompat.widget.AppCompatTextView(
        ContextThemeWrapper(
            _relative_option_selector.context,
            R.style.relativeSelector
        )
    ) {
        private var _state_active: Boolean = false

        init {
            this.text = resources.getString(this._value)
            this.setOnClickListener {
                this._relative_option_selector.set_active_button(this)
                this.set_active(true)
            }
        }

        override fun onCreateDrawableState(extra_space: Int): IntArray? {
            val drawable_state = super.onCreateDrawableState(extra_space + 1)
            val new_state = mutableListOf<Int>()

            if (this._state_active) {
                new_state.add(R.attr.state_active)
            }

            mergeDrawableStates(drawable_state, new_state.toIntArray())
            return drawable_state
        }

        fun set_active(value: Boolean) {
            this._state_active = value
            refreshDrawableState()
        }

        override fun onLayout(is_changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(is_changed, left, top, right, bottom)
            this.text = resources.getString(this._value)
            this.gravity = CENTER
        }
    }

    init {
        this.populate()
    }

    fun get_state(): Int? {
        if (this._active_button == null) {
            return null
        }
        return this._button_map[this._active_button!!]!!
    }

    fun set_state(new_state: Int, manual: Boolean = false, suppress: Boolean = false) {
        for ((button, value) in this._button_map) {
            if (value == new_state) {
                this.set_active_button(button, suppress)
                if (manual) {
                    button.set_active(true)
                }
                return
            }
        }
    }

    fun clear() {
        this._active_button = null
        this._button_map.clear()
        this.removeAllViews()
    }

    private fun populate() {
        this._item_list.forEachIndexed { i, string_index ->
            RelativeOptionSelectorButton(this, string_index).let { current_view ->
                this.addView(current_view)
                if (this.orientation == HORIZONTAL) {
                    (current_view.layoutParams as LayoutParams).height = MATCH_PARENT
                    (current_view.layoutParams as LayoutParams).weight = 1f
                    (current_view.layoutParams as LayoutParams).width = 41
                    if (i == 1) {
                        (current_view.layoutParams as MarginLayoutParams).setMargins(5, 0, 5, 0)
                    }
                } else {
                    (current_view.layoutParams as LayoutParams).height = 0
                    (current_view.layoutParams as LayoutParams).weight = 1f
                    (current_view.layoutParams as LayoutParams).width = MATCH_PARENT
                    if (i == 1) {
                        (current_view.layoutParams as MarginLayoutParams).setMargins(0, 5, 0, 5)
                    }
                }

                this._button_map[current_view] = i
            }
        }
    }

    fun unset_on_change() {
        this._on_change_hook = null
    }

    fun set_on_change(hook: (RelativeOptionSelector) -> Unit) {
        this._on_change_hook = hook
    }

    fun set_active_button(view: RelativeOptionSelectorButton, suppress_callback: Boolean = false) {
        if (this._active_button != view && this._active_button != null) {
            this._active_button!!.set_active(false)
        }
        this.unset_active_button()

        this._active_button = view

        if (!suppress_callback && this._on_change_hook != null) {
            this._on_change_hook!!(this)
        }
    }

    private fun unset_active_button() {
        if (this._active_button == null) {
            return
        }
        this._active_button!!.set_active(false)
        this._active_button = null
    }
}

