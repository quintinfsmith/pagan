package com.qfs.radixulous

import android.content.Context
import android.view.Gravity.CENTER
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.radixulous.opusmanager.OpusEvent
import kotlin.math.abs

class LeafButton: LinearLayout {
    private val STATE_REFLECTED = intArrayOf(R.attr.state_reflected)
    private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
    private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
    private val STATE_INVALID = intArrayOf(R.attr.state_invalid)

    private var state_active: Boolean = false
    private var state_reflected: Boolean = false
    private var state_focused: Boolean = false
    private var state_invalid: Boolean = false
    private var event: OpusEvent?
    private var activity: MainActivity
    private var value_label: TextView
    private var prefix_label: TextView

    constructor(context: Context, activity: MainActivity, event: OpusEvent?, is_percussion: Boolean) : super(ContextThemeWrapper(context, R.style.leaf)) {
        this.activity = activity
        this.event = event
        this.orientation = VERTICAL

        this.value_label = TextView(ContextThemeWrapper(this.context, R.style.leaf_value))
        this.prefix_label = TextView(ContextThemeWrapper(this.context, R.style.leaf_prefix))
        this.addView(this.prefix_label)
        this.addView(this.value_label)

        if (event != null) {
            this.setActive(true)
        } else {
            this.setActive(false)
        }
        this.set_text(is_percussion)

        this.value_label.setOnTouchListener { view, touchEvent ->
            false
        }
        this.prefix_label.setOnTouchListener { view, touchEvent ->
            false
        }

    }


    fun set_text(is_percussion: Boolean) {
        if (this.event == null) {
            this.value_label.text = ""
            return
        }

        var event = this.event!!

        this.prefix_label.text = if (event.relative && !is_percussion) {
            this.prefix_label.visibility = View.VISIBLE
            if (event.note < 0) {
                this.activity.getString(R.string.pfx_subtract)
            } else {
                this.activity.getString(R.string.pfx_add)
            }
        } else {
            this.prefix_label.visibility = View.GONE
            ""
        }
        this.value_label.text = if (is_percussion) {
            ""
        } else {
            get_number_string(event.note, event.radix, 2)
        }


        if (event.relative && !is_percussion) {
            (this.prefix_label.layoutParams as LinearLayout.LayoutParams).apply {
                height = WRAP_CONTENT
                setMargins(0,-24,0,0)
                gravity = CENTER
            }
            (this.value_label.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1F
                height = 0
                gravity = CENTER
                setMargins(0,-30,0,0)
            }
        } else {
            (this.prefix_label.layoutParams as LinearLayout.LayoutParams).apply {
                height = WRAP_CONTENT
                setMargins(0,0,0,0)
                gravity = CENTER
            }
            (this.value_label.layoutParams as LinearLayout.LayoutParams).apply {
                weight = 1F
                height = 0
                gravity = CENTER
                setMargins(0,0,0,0)
            }
        }

    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 3)
        if (this.state_active) {
            mergeDrawableStates(drawableState, STATE_ACTIVE)
        }
        if (this.state_reflected) {
            mergeDrawableStates(drawableState, STATE_REFLECTED)
        }
        if (this.state_focused) {
            mergeDrawableStates(drawableState, STATE_FOCUSED)
        }
        if (this.state_invalid) {
            mergeDrawableStates(drawableState, STATE_INVALID)
        }
        return drawableState
    }

    fun setActive(value: Boolean) {
        this.state_active = value
        refreshDrawableState()
    }

    fun setReflected(value: Boolean) {
        this.state_reflected = value
        refreshDrawableState()
    }

    fun setFocused(value: Boolean) {
        this.state_focused = value
        refreshDrawableState()
    }

    fun setInvalid(value: Boolean) {
        this.state_invalid = value
        refreshDrawableState()
    }
}
