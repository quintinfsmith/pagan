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
    var active_button: RelativeOptionSelectorButton? = null
    var button_map = HashMap<RelativeOptionSelectorButton, Int>()
    var itemList: List<Int> = listOf(
        R.string.pfx_add,
        R.string.pfx_subtract,
        R.string.pfx_pow,
        R.string.pfx_log
    )
    private var hidden_options: MutableSet<Int> = mutableSetOf()
    var on_change_hook: ((RelativeOptionSelector) -> Unit)? = null

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.populate()
    }
    override fun onLayout(isChanged: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(isChanged, left, top, right, bottom)

        var visible_count = (this.childCount - this.hidden_options.size)
        if (visible_count > 0) {
            var width = ((right - left) - (this.paddingLeft + this.paddingRight)) / visible_count
            for (i in 0 until this.childCount) {
                this.getChildAt(i).apply {
                    layoutParams.width = width
                }.requestLayout()
            }
        }
    }

    fun getState(): Int? {
        if (this.active_button == null) {
            return null
        }
        return this.button_map[this.active_button!!]!!
    }

    fun setState(new_state: Int, manual: Boolean = false) {
        if (new_state >= this.itemList.size) {
            throw Exception("Not an option")
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

    fun populate() {
        this.itemList.forEachIndexed { i, string_index ->
            var position = when (i) {
                0 -> { 0 }
                this.itemList.size - 1 -> { 2 }
                else -> { 1 }
            }

            val currentView = RelativeOptionSelectorButton(this, position, string_index)
            this.addView(currentView)
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

    fun unset_active_button() {
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
}

class RelativeOptionSelectorButton(var roSelector: RelativeOptionSelector, var position: Int, var value: Int): androidx.appcompat.widget.AppCompatTextView(roSelector.context) {
    private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
    var state_active: Boolean = false
    init {
        // TODO: Handle any radix
        this.text = resources.getString(this.value)
        this.gravity = CENTER

        this.background = when (this.value) {
            0 -> {
                resources.getDrawable(R.drawable.ns_start)
            }
            2 -> {
                resources.getDrawable(R.drawable.ns_end)
            }
            else -> {
                resources.getDrawable(R.drawable.ns_middle)
            }
        }

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
}
