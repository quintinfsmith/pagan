package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration.getLongPressTimeout
import androidx.appcompat.widget.AppCompatTextView
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import kotlin.concurrent.thread
import kotlin.math.ceil
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

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
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

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    private fun on_click() {
        val opus_manager = this.get_opus_manager()

        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range()) {
            val (first_key, second_key) = cursor.get_ordered_range()!!
            if (first_key != second_key) {
                try {
                    opus_manager.overwrite_beat_range_horizontally(
                        this.channel,
                        this.line_offset,
                        first_key,
                        second_key
                    )
                } catch (e: OpusLayerBase.MixedInstrumentException) {
                    opus_manager.cursor_select_line(this.channel, this.line_offset)
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    opus_manager.cursor_select_line(this.channel, this.line_offset)
                }
            } else {
                try {
                    opus_manager.overwrite_line(this.channel, this.line_offset, first_key)
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    opus_manager.cursor_select_line(this.channel, this.line_offset)
                }
            }
        } else {
            if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == this.channel && cursor.line_offset == this.line_offset && cursor.ctl_level == null) {
                opus_manager.cursor_select_channel(this.channel)
            } else {
                opus_manager.cursor_select_line(this.channel, this.line_offset)
            }
        }
    }

    private fun on_long_click(): Boolean {
        val activity = this.get_activity()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range()) {
            val (first_key, second_key) = cursor.get_ordered_range()!!
            val default_count = ceil((opus_manager.beat_count.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
            activity.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                if (first_key != second_key) {
                    try {
                        opus_manager.overwrite_beat_range_horizontally(
                            this.channel,
                            this.line_offset,
                            first_key,
                            second_key,
                            repeat
                        )
                    } catch (e: OpusLayerBase.MixedInstrumentException) {
                        opus_manager.cursor_select_line(this.channel, this.line_offset)
                    } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                        opus_manager.cursor_select_line(this.channel, this.line_offset)
                    }
                } else {
                    try {
                        opus_manager.overwrite_line(this.channel, this.line_offset, first_key, repeat)
                    } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                        opus_manager.cursor_select_line(this.channel, this.line_offset)
                    }
                }
            }
        } else {
            if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == this.channel && cursor.line_offset == this.line_offset && cursor.ctl_level == null) {
                opus_manager.cursor_select_channel(this.channel)
            } else {
                opus_manager.cursor_select_line(this.channel, this.line_offset)
            }
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
            val instrument = opus_manager.get_percussion_instrument(this.line_offset)
            "!$instrument"
        }

        this.text = text
        this.contentDescription = text
    }

    fun get_activity(): MainActivity {
        return (this.parent as LineLabelView).get_activity()
    }
    fun get_opus_manager(): OpusLayerInterface {
        return (this.parent as LineLabelView).get_opus_manager()
    }
}
