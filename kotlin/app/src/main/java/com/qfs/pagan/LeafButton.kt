package com.qfs.pagan

import android.content.Context
import android.view.GestureDetector
import android.view.Gravity.CENTER
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.GestureDetectorCompat
import com.qfs.pagan.opusmanager.OpusEvent

class LeafButton(
    context: Context,
    private var activity: MainActivity,
    private var event: OpusEvent?,
    is_percussion: Boolean
) : LinearLayout(ContextThemeWrapper(context, R.style.leaf)), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private var mDetector: GestureDetectorCompat
    private var double_tap_callback: ((event: MotionEvent) -> Boolean)? = null
    private var single_tap_callback: ((event: MotionEvent) -> Boolean)? = null


    private val STATE_REFLECTED = intArrayOf(R.attr.state_reflected)
    private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
    private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
    private val STATE_INVALID = intArrayOf(R.attr.state_invalid)

    private var state_active: Boolean = false
    private var state_reflected: Boolean = false
    private var state_focused: Boolean = false
    private var state_invalid: Boolean = false
    private var value_wrapper: LinearLayout
    private var value_label_octave: TextView
    private var value_label_offset: TextView
    private var prefix_label: TextView

    init {
        this.orientation = VERTICAL
        this.value_wrapper = LinearLayout(ContextThemeWrapper(this.context, R.style.leaf_value))
        this.value_wrapper.orientation = HORIZONTAL

        this.value_label_octave = TextView(ContextThemeWrapper(this.context, R.style.leaf_value_octave))
        this.value_label_offset = TextView(ContextThemeWrapper(this.context, R.style.leaf_value_offset))
        this.prefix_label = TextView(ContextThemeWrapper(this.context, R.style.leaf_prefix))
        this.addView(this.prefix_label)
        this.addView(this.value_wrapper)
        this.value_wrapper.addView(this.value_label_octave)
        this.value_wrapper.addView(this.value_label_offset)
        if (event != null) {
            this.set_active(true)
        } else {
            this.set_active(false)
        }
        this.set_text(is_percussion)

        this.mDetector = GestureDetectorCompat(this.context, this)
        this.mDetector.setOnDoubleTapListener(this)

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

    private fun set_active(value: Boolean) {
        this.state_active = value
        refreshDrawableState()
    }

    fun set_reflected(value: Boolean) {
        this.state_reflected = value
        refreshDrawableState()
    }

    fun set_focused(value: Boolean) {
        this.state_focused = value
        refreshDrawableState()
    }

    fun set_invalid(value: Boolean) {
        this.state_invalid = value
        refreshDrawableState()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (this.mDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onShowPress(p0: MotionEvent?) {
        //TODO("Not yet implemented")
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onLongPress(p0: MotionEvent?) {
        //TODO("Not yet implemented")
    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    override fun onSingleTapConfirmed(p0: MotionEvent): Boolean {
        return if (this.single_tap_callback != null) {
            this.single_tap_callback!!(p0)
        } else {
            false
        }
    }

    override fun onDoubleTap(p0: MotionEvent): Boolean {
        return if (this.double_tap_callback != null) {
            this.double_tap_callback!!(p0)
        } else {
            false
        }
    }

    override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
        //TODO("Not yet implemented")
        return false
    }

    fun setOnDoubleTapListener(callback: (event: MotionEvent) -> Boolean) {
        this.double_tap_callback = callback
    }
    fun setOnSingleTapListener(callback: (event: MotionEvent) -> Boolean) {
        this.single_tap_callback = callback
    }
}
