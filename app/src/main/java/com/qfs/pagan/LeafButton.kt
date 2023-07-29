package com.qfs.pagan

import android.content.Context
import android.view.GestureDetector
import android.view.Gravity.CENTER
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.GestureDetectorCompat
import com.qfs.pagan.opusmanager.OpusEvent

class LeafButton(
    context: Context,
    private var activity: MainActivity,
    private var event: OpusEvent?,
    var position_node: PositionNode,
    is_percussion: Boolean
) : LinearLayout(ContextThemeWrapper(context, R.style.leaf)) {

    // LeafText exists to make the text consider the state of the LeafButton
    class InnerWrapper(context: Context): LinearLayout(context) {
        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 4)
            val parent = this.parent ?: return drawableState
            return (parent as LeafButton).build_drawable_state(drawableState)
        }
    }

    class LeafText(context: Context): androidx.appcompat.widget.AppCompatTextView(context) {
        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 4)
            var parent = this.parent ?: return drawableState
            while (parent !is LeafButton) {
                parent = parent.parent
            }
            return (parent as LeafButton).build_drawable_state(drawableState)
        }
    }

    private val STATE_LINKED = intArrayOf(R.attr.state_linked)
    private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
    private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
    private val STATE_INVALID = intArrayOf(R.attr.state_invalid)

    private var state_active: Boolean = false
    private var state_linked: Boolean = false
    private var state_focused: Boolean = false
    private var state_invalid: Boolean = false
    private var value_wrapper: LinearLayout
    private var value_label_octave: TextView
    private var value_label_offset: TextView
    private var prefix_label: TextView
    private var inner_wrapper: InnerWrapper = InnerWrapper(ContextThemeWrapper(this.context, R.style.leaf_inner))

    init {
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
        this.inner_wrapper.orientation = VERTICAL
        this.value_wrapper = LinearLayout(ContextThemeWrapper(this.context, R.style.leaf_value))
        this.value_wrapper.orientation = HORIZONTAL

        this.value_label_octave = LeafText(ContextThemeWrapper(this.context, R.style.leaf_value_octave))
        this.value_label_offset = LeafText(ContextThemeWrapper(this.context, R.style.leaf_value_offset))
        this.prefix_label = LeafText(ContextThemeWrapper(this.context, R.style.leaf_prefix))
        (this.inner_wrapper as LinearLayout).addView(this.prefix_label)
        (this.inner_wrapper as LinearLayout).addView(this.value_wrapper)
        this.value_wrapper.addView(this.value_label_octave)
        this.value_wrapper.addView(this.value_label_offset)

        this.addView(this.inner_wrapper)
        this.inner_wrapper.layoutParams.apply {
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        if (event != null) {
            this.set_active(true)
        } else {
            this.set_active(false)
        }
        this.set_text(is_percussion)
    }

    // Prevents the child labels from blocking the parent onTouchListener events
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return true
    }

    private fun unset_text() {
        this.prefix_label.visibility = View.GONE
        this.value_label_octave.visibility = View.GONE
        this.value_label_offset.visibility = View.GONE
    }

    private fun set_text(is_percussion: Boolean) {
        if (this.event == null) {
            this.unset_text()
            return
        }

        val event = this.event!!

        var use_note = event.note
        this.prefix_label.text = if (!is_percussion && (event.relative && event.note != 0)) {
            this.prefix_label.visibility = View.VISIBLE
            if (event.note < 0) {
                use_note = 0 - event.note
                this.activity.getString(R.string.pfx_subtract)
            } else {
                this.activity.getString(R.string.pfx_add)
            }
        } else {
            this.prefix_label.visibility = View.GONE
            ""
        }


        if (is_percussion) {
            this.value_label_octave.visibility = View.GONE
            this.value_label_offset.text = this.activity.getString(R.string.percussion_label)
        } else if (event.relative && event.note == 0) {
            this.value_label_octave.visibility = View.GONE
            this.value_label_offset.text = this.activity.getString(R.string.repeat_note)
        } else {
            this.value_label_octave.visibility = View.VISIBLE
            this.value_label_octave.text = get_number_string(use_note / event.radix, event.radix, 1)
            this.value_label_offset.text = get_number_string(use_note % event.radix, event.radix, 1)
        }

        if (event.relative && event.note != 0) {
            (this.prefix_label.layoutParams as LayoutParams).apply {
                height = WRAP_CONTENT
                setMargins(0,-24,0,0)
                gravity = CENTER
            }
            (this.value_wrapper.layoutParams as LayoutParams).apply {
                weight = 1F
                height = 0
                gravity = CENTER
                setMargins(0,-30,0,0)
            }
        } else {
            (this.prefix_label.layoutParams as LayoutParams).apply {
                height = WRAP_CONTENT
                setMargins(0,0,0,0)
                gravity = CENTER
            }
            (this.value_wrapper.layoutParams as LayoutParams).apply {
                weight = 1F
                height = 0
                gravity = CENTER
                setMargins(0,0,0,0)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val line_height = resources.getDimension(R.dimen.line_height)
        this.layoutParams.height = line_height.toInt()
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        // TODO: Stop using state_* and just use the opus_manager to check states
        if (this.state_active) {
            mergeDrawableStates(drawableState, STATE_ACTIVE)
        }
        if (this.state_linked) {
            mergeDrawableStates(drawableState, STATE_LINKED)
        }
        if (this.state_focused) {
            mergeDrawableStates(drawableState, STATE_FOCUSED)
        }
        if (this.state_invalid) {
            mergeDrawableStates(drawableState, STATE_INVALID)
        }
        return drawableState
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 4)
        return this.build_drawable_state(drawableState)
    }

    override fun refreshDrawableState() {
        this.value_label_octave.refreshDrawableState()
        this.value_label_offset.refreshDrawableState()
        this.prefix_label.refreshDrawableState()
        this.inner_wrapper.refreshDrawableState()
        super.refreshDrawableState()
    }

    private fun set_active(value: Boolean) {
        this.state_active = value
        this.refreshDrawableState()
    }

    fun set_linked(value: Boolean) {
        this.state_linked = value
        this.refreshDrawableState()
    }

    fun set_focused(value: Boolean) {
        this.state_focused = value
        this.refreshDrawableState()
    }

    fun set_invalid(value: Boolean) {
        this.state_invalid = value
        this.refreshDrawableState()
    }
}
