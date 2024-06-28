package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.ColorMap.Palette


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
                this.setActive(true)
            }
        }
        private fun _setup_colors() {
            var context = this.context
            while (context !is MainActivity) {
                context = (context as ContextThemeWrapper).baseContext
            }
            val color_map = context.view_model.color_map

            val states = arrayOf<IntArray>(
                intArrayOf(R.attr.state_active),
                intArrayOf(
                    -R.attr.state_active,
                    R.attr.state_alternate
                ),
                intArrayOf(
                    -R.attr.state_active,
                    -R.attr.state_alternate
                )
            )
            for (i in 0 until (this.background as StateListDrawable).stateCount) {
                ((this.background as StateListDrawable).getStateDrawable(i) as LayerDrawable).findDrawableByLayerId(R.id.tintable_background)?.setTintList(
                    ColorStateList(
                        states,
                        intArrayOf(
                            color_map[Palette.Selection],
                            color_map[Palette.ButtonAlt],
                            color_map[Palette.Button]
                        )
                    )
                )
            }

            this.setTextColor(
                ColorStateList(
                    states,
                    intArrayOf(
                        color_map[Palette.SelectionText],
                        color_map[Palette.ButtonAltText],
                        color_map[Palette.ButtonText]
                    )
                )
            )
        }


        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            val new_state = mutableListOf<Int>()

            if (this._state_active) {
                new_state.add(R.attr.state_active)
            }

            mergeDrawableStates(drawableState, new_state.toIntArray())
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

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            this._setup_colors()
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

    fun setState(new_state: Int, manual: Boolean = false, surpress_callback: Boolean = false) {
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
                (currentView.layoutParams as LayoutParams).height = MATCH_PARENT
                (currentView.layoutParams as LayoutParams).weight = 1f
                (currentView.layoutParams as LayoutParams).width = 41
                if (i == 1) {
                    (currentView.layoutParams as MarginLayoutParams).setMargins(5, 0, 5, 0)
                }
            } else {
                (currentView.layoutParams as LayoutParams).height = 0
                (currentView.layoutParams as LayoutParams).weight = 1f
                (currentView.layoutParams as LayoutParams).width = MATCH_PARENT
                if (i == 1) {
                    (currentView.layoutParams as MarginLayoutParams).setMargins(0, 5, 0, 5)
                }
            }

            this._button_map[currentView] = i
        }
    }

    fun unsetOnChange() {
        this._on_change_hook = null
    }

    fun setOnChange(hook: (RelativeOptionSelector) -> Unit) {
        this._on_change_hook = hook
    }

    fun set_active_button(view: RelativeOptionSelectorButton, surpress_callback: Boolean = false) {
        if (this._active_button != view && this._active_button != null) {
            this._active_button!!.setActive(false)
        }
        this.unset_active_button()

        this._active_button = view

        if (!surpress_callback && this._on_change_hook != null) {
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
}

