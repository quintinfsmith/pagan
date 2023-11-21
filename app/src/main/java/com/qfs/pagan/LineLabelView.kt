package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.AppCompatTextView
import com.qfs.pagan.opusmanager.LinksLayer
import com.qfs.pagan.InterfaceLayer as OpusManager

class LineLabelView(context: Context, var channel: Int, var line_offset: Int): AppCompatTextView(ContextThemeWrapper(context, R.style.line_label)),
    View.OnTouchListener {
    companion object {
        private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
        private val STATE_CHANNEL_EVEN = intArrayOf(R.attr.state_channel_even)
    }

    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    private var _update_queued = false
    init {
        this.setOnClickListener {
            this.on_click()
        }

        this.setOnTouchListener(this)

        this.setOnDragListener { view: View, dragEvent: DragEvent ->
            val adapter = (view.parent.parent as LineLabelColumnLayout)
            when (dragEvent.action) {
                DragEvent.ACTION_DROP -> {
                    if (adapter.is_dragging()) {
                        val (from_channel, from_line) = adapter.dragging_position!!
                        val (to_channel, to_line) = (view as LineLabelView).get_row()
                        val opus_manager = this.get_opus_manager()
                        if (from_channel != to_channel || from_line != to_line) {
                            opus_manager.move_line(
                                from_channel,
                                from_line,
                                to_channel,
                                to_line
                            )
                        }
                    }
                    adapter.stop_dragging()
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    adapter.stop_dragging()
                }
                DragEvent.ACTION_DRAG_STARTED -> { }
                else -> { }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val line_height = resources.getDimension(R.dimen.line_height)
        this.layoutParams.height = line_height.toInt()
        this.layoutParams.width = WRAP_CONTENT
        this.set_text()
    }

    private fun get_label_text(): String {
        val opus_manager = this.get_opus_manager()
        return if (!opus_manager.is_percussion(this.channel)) {
            "${this.channel}::${this.line_offset}"
        } else {
            val instrument = opus_manager.get_percussion_instrument(this.line_offset)
            "!$instrument"
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this._update_queued = true
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
        return this.build_drawable_state(drawableState)
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null) {
            return drawableState
        }
        val opus_manager = this.get_opus_manager()
        if (this.channel % 2 == 0) {
            mergeDrawableStates(drawableState, LineLabelView.STATE_CHANNEL_EVEN)
        }

        when (opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Row -> {
                if (opus_manager.cursor.channel == this.channel && opus_manager.cursor.line_offset == this.line_offset) {
                    mergeDrawableStates(drawableState, LineLabelView.STATE_FOCUSED)
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = opus_manager.cursor.range!!
                if ((this.channel > first.channel && this.channel < second.channel) || (this.channel == first.channel && this.line_offset >= first.line_offset) || (this.channel == second.channel && this.line_offset <= second.line_offset)) {
                    mergeDrawableStates(drawableState, LineLabelView.STATE_FOCUSED)
                }
            }
            else -> { }
        }

        return drawableState
    }

    fun set_text() {
        val text = this.get_label_text()
        this.text = text
        this.contentDescription = text
    }

    fun get_opus_manager(): OpusManager {
        return (this.parent.parent as LineLabelColumnLayout).get_opus_manager()
    }

    fun get_row(): Pair<Int, Int> {
        return Pair(this.channel, this.line_offset)
    }

    fun get_position(): Int {
        var parent = this.parent as ViewGroup
        return parent.indexOfChild(this)
    }

    override fun onTouch(view: View?, touchEvent: MotionEvent?): Boolean {
        val column_layout = this.parent.parent as LineLabelColumnLayout

        return if (touchEvent == null) {
            true
        } else if (touchEvent.action == MotionEvent.ACTION_MOVE) {
            val (channel, line_offset) = (view as LineLabelView).get_row()
            if (!column_layout.is_dragging()) {
                column_layout.set_dragging_line(channel, line_offset)
                view.startDragAndDrop(
                    null,
                    DragShadowBuilder(view),
                    null,
                    0
                )
            }
            true
        } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
            column_layout.stop_dragging()
            true
        } else {
            performClick()
        }
    }

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }


    private fun on_click() {
        val opus_manager = this.get_opus_manager()
        val (channel, line_offset) = this.get_row()

        val cursor = opus_manager.cursor
        if (cursor.is_linking_range()) {
            val first_key = cursor.range!!.first
            try {
                if (this.get_activity().configuration.link_mode) {
                    opus_manager.link_beat_range_horizontally(
                        channel,
                        line_offset,
                        first_key,
                        cursor.range!!.second
                    )
                } else {
                    opus_manager.overwrite_beat_range_horizontally(
                        channel,
                        line_offset,
                        first_key,
                        cursor.range!!.second
                    )
                }
            } catch (e: LinksLayer.BadRowLink) {
                // TODO: Feedback
                //(this.context as MainActivity).feedback_msg("Can only row-link from first beat")
            }
            cursor.is_linking = false
            opus_manager.cursor_select(first_key, opus_manager.get_first_position(first_key))
        } else if (cursor.is_linking) {
            val beat_key = opus_manager.cursor.get_beatkey()
            try {
                if (this.get_activity().configuration.link_mode) {
                    opus_manager.link_row(channel, line_offset, beat_key)
                } else {
                    opus_manager.overwrite_row(channel, line_offset, beat_key)
                }
            } catch (e: LinksLayer.BadRowLink) {
                // TODO: Feedback
                //(this.context as MainActivity).feedback_msg("Can only row-link from first beat")
            }
            cursor.is_linking = false
            opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
        } else {
            opus_manager.cursor_select_row(channel, line_offset)
        }
    }
}