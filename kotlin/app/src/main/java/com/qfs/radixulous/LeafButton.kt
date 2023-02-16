package com.qfs.radixulous

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.radixulous.opusmanager.OpusEvent
import kotlin.math.abs

class LeafButton: androidx.appcompat.widget.AppCompatTextView {
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

    constructor(context: Context, activity: MainActivity, event: OpusEvent?, is_percussion:Boolean = false) : super(ContextThemeWrapper(context, R.style.leaf)) {
        this.activity = activity
        this.event = event
        if (event != null) {
            this.setActive(true)

            var event = this.event!!
            this.text = if (is_percussion) {
                ""
            } else if (event.relative) {
                if (event.note == 0 || event.note % event.radix != 0) {
                    val prefix = if (event.note < 0) {
                        this.activity.getString(R.string.pfx_subtract)
                    } else {
                        this.activity.getString(R.string.pfx_add)
                    }
                    "$prefix${get_number_string(abs(event.note), event.radix, 1)}"
                } else {
                    val prefix = if (event.note < 0) {
                        this.activity.getString(R.string.pfx_log)
                    } else {
                        this.activity.getString(R.string.pfx_pow)
                    }
                    "$prefix${get_number_string(abs(event.note) / event.radix, event.radix, 1)}"
                }
            } else {
                get_number_string(event.note, event.radix, 2)
            }
        } else {
            this.setActive(false)
            this.text = this.activity.getString(R.string.empty_note)
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
