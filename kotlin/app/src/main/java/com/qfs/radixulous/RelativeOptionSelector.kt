package com.qfs.radixulous

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper


class RelativeOptionSelector(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    class InvalidOptionException(option: Int): Exception("Invalid Option selected: $option")
    private var active_button: RelativeOptionSelectorButton? = null
    private var button_map = HashMap<RelativeOptionSelectorButton, Int>()
    private var itemList: List<Int> = listOf(
        R.string.absolute_label,
        R.string.pfx_add,
        R.string.pfx_subtract
    )
    private var hidden_options: MutableSet<Int> = mutableSetOf()
    private var on_change_hook: ((RelativeOptionSelector) -> Unit)? = null

    // TODO: Handle any radix
    class RelativeOptionSelectorButton (
        private var roSelector: RelativeOptionSelector,
        private var value: Int
    ) : androidx.appcompat.widget.AppCompatTextView(
        ContextThemeWrapper(
            roSelector.context,
            R.style.relativeSelector
        )
    ) {
        private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
        private var state_active: Boolean = false

        init {
            this.text = resources.getString(this.value)
            this.gravity = CENTER
            this.setOnClickListener {
                this.roSelector.set_active_button(this)
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

        override fun onLayout(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(isChanged, left, top, right, bottom)
            this.text = resources.getString(this.value)
            this.gravity = CENTER
        }
    }

    init {
        this.populate()
    }

    fun getState(): Int? {
        if (this.active_button == null) {
            return null
        }
        return this.button_map[this.active_button!!]!!
    }

    fun setState(new_state: Int, manual: Boolean = false) {
        if (new_state >= this.itemList.size) {
            throw InvalidOptionException(new_state)
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

    fun clear() {
        this.active_button = null
        this.button_map.clear()
        this.removeAllViews()
    }

    private fun populate() {
        this.itemList.forEachIndexed { i, string_index ->
            val currentView = RelativeOptionSelectorButton(this, string_index)
            this.addView(currentView)
            (currentView.layoutParams as LayoutParams).apply {
                height = 0
                weight = 1f
                width = MATCH_PARENT
            }
            if (i == 1) {
                (currentView.layoutParams as MarginLayoutParams).setMargins(0, 5, 0, 5)
            }

            this.button_map[currentView] = i
        }
    }

    fun setOnChange(hook: (RelativeOptionSelector) -> Unit) {
        this.on_change_hook = hook
    }

    fun set_active_button(view: RelativeOptionSelectorButton) {
        if (this.active_button != view && this.active_button != null) {
            this.active_button!!.setActive(false)
        }
        this.unset_active_button()

        this.active_button = view

        if (this.on_change_hook != null) {
            this.on_change_hook!!(this)
        }
    }

    private fun unset_active_button() {
        if (this.active_button == null) {
            return
        }
        this.active_button!!.setActive(false)
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
    fun unhideOption(index: Int) {
        this.hidden_options.remove(index)
        for ((view, i) in this.button_map) {
            if (i == index) {
                view.visibility = View.VISIBLE
            }
        }

    }
}

