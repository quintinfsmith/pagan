package com.qfs.pagan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration.getLongPressTimeout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toColorInt
import androidx.core.graphics.toColorLong
import com.qfs.pagan.Activity.ActivityEditor
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sqrt

class LineLabelStd(context: Context, var channel: Int, var line_offset: Int): AppCompatTextView(ContextThemeWrapper(context, R.style.line_label)) {
    val click_threshold_millis = 250
    val click_threshold_pixels = 5
    var press_position: Pair<Float, Float>? = null
    var press_timestamp: Long = 0
    val long_click_duration: Long = getLongPressTimeout().toLong()
    var flag_long_click_cancelled: Boolean = false
    init {
        this.setOnClickListener {
            this.on_click()
        }
        this.setOnTouchListener { view: View?, touchEvent: MotionEvent? ->
            this.touch_callback(view, touchEvent)
        }
    }

    private fun cancel_long_click() {
        this.flag_long_click_cancelled = true
    }

    private fun dispatch_long_clicker() {
        this.flag_long_click_cancelled = false
        Thread.sleep(this.long_click_duration)
        if (!this.flag_long_click_cancelled) {
            this.get_activity().runOnUiThread {
                this.on_long_click()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = this.resources.getDimension(R.dimen.line_height).toInt()
        this.layoutParams.width = this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        this.set_text()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val line = this.get_opus_manager().get_all_channels()[this.channel].lines[this.line_offset]
        val line_color = line.color
        if (line_color != null) {
            val stripe_stroke = resources.getDimension(R.dimen.stroke_leaf)
            val line_height = resources.getDimension(R.dimen.line_height)
            val paint = Paint()
            paint.color = line_color.toColorLong().toColorInt()
            canvas.drawRect(
                this.width.toFloat() / 3f,
                (line_height * 1F / 16F),
                this.width.toFloat() - resources.getDimension(R.dimen.stroke_leaf),
                line_height * 4F / 16F,
                paint
            )
        }
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 3)
        return this._build_drawable_state(drawableState)
    }

    private fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null) {
            return drawableState
        }

        val opus_manager = this.get_opus_manager()

        val new_state = mutableListOf<Int>()
        if (opus_manager.is_line_selected(channel, line_offset)) {
            new_state.add(R.attr.state_focused)
        } else if (opus_manager.is_line_selected_secondary(channel, line_offset)) {
            new_state.add(R.attr.state_focused_secondary)
        }

        if (this.is_muted()) {
            new_state.add(R.attr.state_muted)
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    private fun on_click() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val tracker = this.get_activity().get_action_interface()
        if (cursor.is_selecting_range()) {
            tracker.repeat_selection_std(this.channel, this.line_offset, -1)
        } else {
            tracker.cursor_select_line_std(this.channel, this.line_offset)
        }
    }

    private fun on_long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val tracker = this.get_activity().get_action_interface()
        if (cursor.is_selecting_range()) {
            tracker.repeat_selection_std(this.channel, this.line_offset)
        } else {
            tracker.cursor_select_line_std(this.channel, this.line_offset)
        }

        return false
    }

    fun touch_callback(view: View?, touchEvent: MotionEvent?): Boolean {
        this.cancel_long_click()
        var parent = view?.parent ?: return false
        while (parent !is LineLabelColumnLayout) {
            parent = parent.parent
        }

        val column_layout = parent

        return if (touchEvent == null) {
            true
        } else if (touchEvent.action == MotionEvent.ACTION_MOVE) {
            if (!column_layout.is_dragging()) {
                val d = sqrt((touchEvent.x - this.press_position!!.first).pow(2f) + (touchEvent.y - this.press_position!!.second).pow(2f))
                if (d > this.click_threshold_pixels) {
                    column_layout.set_dragging_line(this.channel, this.line_offset)
                    (view.parent as LineLabelView).startDragAndDrop(
                        null,
                        DragShadowBuilder(view),
                        null,
                        0
                    )
                }
            }
            true
        } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
            thread {
                this@LineLabelStd.dispatch_long_clicker()
            }

            column_layout.stop_dragging()
            this.press_timestamp = System.currentTimeMillis()
            this.press_position = Pair(touchEvent.x, touchEvent.y)
            true
        } else if (touchEvent.action == MotionEvent.ACTION_UP) {
            val hold_time = System.currentTimeMillis() - this.press_timestamp
            if (hold_time < this.click_threshold_millis && !column_layout.is_dragging()) {
                performClick()
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    fun set_text() {
        val opus_manager = this.get_opus_manager()

        val text = if (!opus_manager.is_percussion(this.channel)) {
            "${this.channel}::${this.line_offset}"
        } else {
            val instrument = opus_manager.get_percussion_instrument(this.channel, this.line_offset)
            "!$instrument"
        }

        this.text = text
        this.contentDescription = text
        this.refreshDrawableState()
    }

    fun get_activity(): ActivityEditor {
        return (this.parent as LineLabelView).get_activity()
    }
    fun get_opus_manager(): OpusLayerInterface {
        return (this.parent as LineLabelView).get_opus_manager()
    }

    fun is_muted(): Boolean {
        val opus_manager = this.get_opus_manager()
        val channel = opus_manager.get_channel(this.channel)
        return channel.muted || channel.get_line(this.line_offset).muted
    }
}
