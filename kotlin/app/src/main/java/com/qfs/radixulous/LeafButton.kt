package com.qfs.radixulous

import android.content.Context
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
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
    private var opus_manager: OpusManager

    constructor(context: Context, activity: MainActivity, event: OpusEvent?, opus_manager: OpusManager) : super(ContextThemeWrapper(context, R.style.leaf)) {
        this.activity = activity
        this.event = event
        this.opus_manager = opus_manager

        if (event != null) {
            this.setActive(true)
        } else {
            this.setActive(false)
        }
    }

    override fun onLayout(is_changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(is_changed, left, top, right, bottom)
        this.set_text()
    }

    fun set_text() {
        if (this.event == null) {
            this.text = ""
            return
        }


        var event = this.event!!

        this.text = if (this.opus_manager.is_percussion(event.channel)) {
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
