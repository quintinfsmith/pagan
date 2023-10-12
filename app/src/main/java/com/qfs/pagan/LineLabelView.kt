package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.LinksLayer
import com.qfs.pagan.InterfaceLayer as OpusManager

class LineLabelView(var view_holder: RecyclerView.ViewHolder): LinearLayout(ContextThemeWrapper(view_holder.itemView.context, R.style.line_label_outer)),
    View.OnTouchListener {
    class InnerView(context: Context): androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(context, R.style.line_label_inner)) {
        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 2)
            return if (this.parent == null) {
                drawableState
            } else {
                (this.parent as LineLabelView).build_drawable_state(drawableState)
            }
        }
    }
    companion object {
        private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
        private val STATE_CHANNEL_EVEN = intArrayOf(R.attr.state_channel_even)
    }

    private var _text_view = InnerView(context)
    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    private var _update_queued = false
    init {
        this.addView(this._text_view)
        (this.view_holder.itemView as ViewGroup).removeAllViews()
        (this.view_holder.itemView as ViewGroup).addView(this)
        this.setOnClickListener {
            this.on_click()
        }

        this.setOnTouchListener(this)

        this.setOnDragListener { view: View, dragEvent: DragEvent ->
            val adapter = (view as LineLabelView).get_adapter()
            when (dragEvent.action) {
                DragEvent.ACTION_DROP -> {
                    if (adapter.is_dragging()) {
                        val (from_channel, from_line) = adapter.dragging_position!!
                        val (to_channel, to_line) = view.get_std_position()
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
        this._text_view.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        val line_height = resources.getDimension(R.dimen.line_height)
        this.layoutParams.height = line_height.toInt()
        this.layoutParams.width = WRAP_CONTENT
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this._update_queued = true
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
        return this.build_drawable_state(drawableState)
    }

    // Prevents the child labels from blocking the parent onTouchListener events
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return true
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        val opus_manager = this.get_opus_manager()
        val (channel, line_offset) = this.get_row()
        if (channel % 2 == 0) {
            mergeDrawableStates(drawableState, LineLabelView.STATE_CHANNEL_EVEN)
        }

        when (opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Row -> {
                if (opus_manager.cursor.channel == channel && opus_manager.cursor.line_offset == line_offset) {
                    mergeDrawableStates(drawableState, LineLabelView.STATE_FOCUSED)
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = opus_manager.cursor.range!!
                if ((channel > first.channel && channel < second.channel) || (channel == first.channel && line_offset >= first.line_offset) || (channel == second.channel && line_offset <= second.line_offset)) {
                    mergeDrawableStates(drawableState, LineLabelView.STATE_FOCUSED)
                }
            }
            else -> { }
        }

        return drawableState
    }

    fun set_text(text: String) {
        this._text_view.text = text
        this.contentDescription = text
    }
    fun get_opus_manager(): OpusManager {
        return (this.view_holder.bindingAdapter as LineLabelRecyclerAdapter).get_opus_manager()
    }

    fun get_adapter(): LineLabelRecyclerAdapter {
        return this.view_holder.bindingAdapter as LineLabelRecyclerAdapter
    }

    fun get_row(): Pair<Int, Int> {
        val opus_manager = this.get_opus_manager()
        return opus_manager.get_std_offset(this.get_position())
    }

    fun get_position(): Int {
        return this.view_holder.bindingAdapterPosition
    }

    fun get_std_position(): Pair<Int, Int> {
        return this.get_opus_manager().get_std_offset(this.get_position())
    }

    override fun onTouch(view: View?, touchEvent: MotionEvent?): Boolean {
        val adapter = (view as LineLabelView).get_adapter()

        return if (touchEvent == null) {
            true
        } else if (touchEvent.action == MotionEvent.ACTION_MOVE) {
            val (channel, line_offset) = view.get_std_position()
            if (!adapter.is_dragging()) {
                adapter.set_dragging_line(channel, line_offset)
                view.startDragAndDrop(
                    null,
                    DragShadowBuilder(view),
                    null,
                    0
                )
            }
            true
        } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
            adapter.stop_dragging()
            true
        } else {
            performClick()
        }
    }

    private fun on_click() {
        val opus_manager = this.get_opus_manager()
        val (channel, line_offset) = this.get_row()

        val cursor = opus_manager.cursor
        if (cursor.is_linking_range()) {
            val first_key = cursor.range!!.first
            try {
                opus_manager.link_beat_range_horizontally(
                    channel,
                    line_offset,
                    first_key,
                    cursor.range!!.second
                )
            } catch (e: LinksLayer.BadRowLink) {
                // TODO: Feedback
                //(this.context as MainActivity).feedback_msg("Can only row-link from first beat")
            }
            cursor.is_linking = false
            opus_manager.cursor_select(first_key, opus_manager.get_first_position(first_key))
        } else if (cursor.is_linking) {
            val beat_key = opus_manager.cursor.get_beatkey()
            try {
                opus_manager.link_row(channel, line_offset, beat_key)
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