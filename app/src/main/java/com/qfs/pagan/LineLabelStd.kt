package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import kotlin.math.pow
import kotlin.math.sqrt

class LineLabelStd(context: Context, var channel: Int, var line_offset: Int): AppCompatTextView(ContextThemeWrapper(context, R.style.line_label)) {
    val click_threshold_millis = 250
    val click_threshold_pixels = 5
    var press_position: Pair<Float, Float>? = null
    var press_timestamp: Long = 0
    init {
        this.setOnClickListener {
            this.on_click()
        }

        this.setOnTouchListener { view: View?, touchEvent: MotionEvent? ->
            this.touch_callback(view, touchEvent)
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
        val cursor = opus_manager.cursor
        when (cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Line -> {
                val line_selected = ((cursor.ctl_level == CtlLineLevel.Line || cursor.ctl_level == null) && cursor.line_offset == this.line_offset)
                val channel_ctl_selected = (cursor.ctl_level == CtlLineLevel.Channel)
                if (cursor.channel == this.channel && (line_selected || channel_ctl_selected)) {
                    new_state.add(R.attr.state_focused)
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = cursor.get_ordered_range()!!
                val abs_y_start = opus_manager.get_instrument_line_index(first.channel, first.line_offset)
                val abs_y_end = opus_manager.get_instrument_line_index(second.channel, second.line_offset)
                val this_y = opus_manager.get_instrument_line_index(this.channel, this.line_offset)
                if ((abs_y_start .. abs_y_end).contains(this_y)) {
                    new_state.add(R.attr.state_focused)
                }
            }
            OpusManagerCursor.CursorMode.Channel -> {
                if (cursor.channel == this.channel) {
                    new_state.add(R.attr.state_focused)
                }
            }
            else -> { }
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    private fun on_click() {
        val opus_manager = this.get_opus_manager()

        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range()) {
            val (first_key, second_key) = cursor.range!!
            if (first_key != second_key) {
                try {
                    opus_manager.overwrite_beat_range_horizontally(
                        this.channel,
                        this.line_offset,
                        first_key,
                        cursor.range!!.second
                    )
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    // No Feedback.  feels Redundant
                }
            } else {
                try {
                    opus_manager.overwrite_line(this.channel, this.line_offset, first_key)
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    // No Feedback.  feels Redundant
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

    private fun _set_colors() {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map
        val states = arrayOf<IntArray>(
            intArrayOf(
                R.attr.state_focused,
            ),
            intArrayOf(
                -R.attr.state_focused,
                -R.attr.state_channel_even
            ),
            intArrayOf(
                -R.attr.state_focused,
                R.attr.state_channel_even
            )
        )

        this.setTextColor(
            ColorStateList(
                states,
                intArrayOf(
                    color_map[ColorMap.Palette.SelectionText],
                    color_map[ColorMap.Palette.ChannelOddText],
                    color_map[ColorMap.Palette.ChannelEvenText]
                )
            )
        )
    }

    fun touch_callback(view: View?, touchEvent: MotionEvent?): Boolean {
        var parent = view?.parent ?: return false
        while (parent !is LineLabelColumnLayout) {
            parent = parent.parent
        }

        val column_layout = parent as LineLabelColumnLayout

        return if (touchEvent == null) {
            true
        } else if (touchEvent.action == MotionEvent.ACTION_MOVE) {
            if (!column_layout.is_dragging()) {
                val d = sqrt((touchEvent.x - this.press_position!!.first).pow(2f) + (touchEvent.y - this.press_position!!.second).pow(2f))
                if (d > this.click_threshold_pixels) {
                    column_layout.set_dragging_line(this.channel, this.line_offset)
                    (view!!.parent as LineLabelView).startDragAndDrop(
                        null,
                        DragShadowBuilder(view),
                        null,
                        0
                    )
                }
            }
            true
        } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
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

    fun get_opus_manager(): OpusLayerInterface {
        return (this.parent as LineLabelView).get_opus_manager()
    }

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }
}
