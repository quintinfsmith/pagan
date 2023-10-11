package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.View
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
    private var _hidden_options: MutableSet<Int> = mutableSetOf()
    private var _on_change_hook: ((RelativeOptionSelector) -> Unit)? = null

    // TODO: Handle any radix
    class RelativeOptionSelectorButton (
        private var _relative_option_selector: RelativeOptionSelector,
        private var _value: Int
    ) : androidx.appcompat.widget.AppCompatTextView(
        ContextThemeWrapper(
            _relative_option_selector.context,
            R.style.relativeSelector
        )
    ) {
        private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
        private var _state_active: Boolean = false

        init {
            this.text = resources.getString(this._value)
            this.gravity = CENTER
            this.setOnClickListener {
                this._relative_option_selector.set_active_button(this)
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

        override fun onLayout(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(isChanged, left, top, right, bottom)
            this.text = resources.getString(this._value)
            this.gravity = CENTER
        }
    }

    init {
        this.populate()
    }

    fun getState(): Int? {
        if (this._active_button == null) {
            return null
        }
        return this._button_map[this._active_button!!]!!
    }

    fun setState(new_state: Int, manual: Boolean = false) {
        if (new_state >= this._item_list.size) {
            throw InvalidOptionException(new_state)
        }

        for ((button, value) in this._button_map) {
            if (value == new_state) {
                this.set_active_button(button)
                if (manual) {
                    button.setActive(true)
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
            val currentView = RelativeOptionSelectorButton(this, string_index)
            this.addView(currentView)
            if (this.orientation == HORIZONTAL) {
                (currentView.layoutParams as LayoutParams).apply {
                    height = MATCH_PARENT
                    weight = 1f
                    width = 41
                }
                if (i == 1) {
                    (currentView.layoutParams as MarginLayoutParams).setMargins(5, 0, 5, 0)
                }
            } else {
                (currentView.layoutParams as LayoutParams).apply {
                    height = 0
                    weight = 1f
                    width = MATCH_PARENT
                }
                if (i == 1) {
                    (currentView.layoutParams as MarginLayoutParams).setMargins(0, 5, 0, 5)
                }
            }

            this._button_map[currentView] = i
        }
    }

    fun setOnChange(hook: (RelativeOptionSelector) -> Unit) {
        this._on_change_hook = hook
    }

    fun set_active_button(view: RelativeOptionSelectorButton) {
        if (this._active_button != view && this._active_button != null) {
            this._active_button!!.setActive(false)
        }
        this.unset_active_button()

        this._active_button = view

        if (this._on_change_hook != null) {
            this._on_change_hook!!(this)
        }
    }

    private fun unset_active_button() {
        if (this._active_button == null) {
            return
        }
        this._active_button!!.setActive(false)
        this._active_button = null
    }


    fun hide_option(index: Int) {
        this._hidden_options.add(index)
        for ((view, i) in this._button_map) {
            if (i == index) {
                view.visibility = View.GONE
            }
        }
    }
    fun unhide_option(index: Int) {
        this._hidden_options.remove(index)
        for ((view, i) in this._button_map) {
            if (i == index) {
                view.visibility = View.VISIBLE
            }
        }

    }
}

