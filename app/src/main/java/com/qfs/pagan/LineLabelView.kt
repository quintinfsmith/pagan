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

class LineLabelView(var viewHolder: RecyclerView.ViewHolder): LinearLayout(ContextThemeWrapper(viewHolder.itemView.context, R.style.line_label_outer)) {
    class InnerView(context: Context): androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(context, R.style.line_label_inner)) {
        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 1)
            return if (this.parent == null) {
                drawableState
            } else {
                (this.parent as LineLabelView).build_drawable_state(drawableState)
            }
        }
    }
    private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)

    private var textView = InnerView(context)
    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    var update_queued = false
    init {
        this.addView(this.textView)
        (this.viewHolder.itemView as ViewGroup).removeAllViews()
        (this.viewHolder.itemView as ViewGroup).addView(this)

        this.setOnClickListener {
            val opus_manager = this.get_opus_manager()
            val (channel, line_offset) = this.get_row()
            val cursor = opus_manager.opusManagerCursor
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
                val beat_key = opus_manager.opusManagerCursor.get_beatkey()
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


        this.setOnTouchListener { view: View, touchEvent: MotionEvent ->
            var adapter = (view as LineLabelView).get_adapter()
            if (touchEvent.action == MotionEvent.ACTION_MOVE) {
                var (channel, line_offset) = view.get_std_position()
                if (!adapter.is_dragging()) {
                    adapter.set_dragging_line(channel, line_offset)
                    view.startDragAndDrop(
                        null,
                        View.DragShadowBuilder(view),
                        null,
                        0
                    )
                    return@setOnTouchListener true
                }
            } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
                adapter.stop_dragging()
            }
            false
        }

        this.setOnDragListener { view: View, dragEvent: DragEvent ->
            val adapter = (view as LineLabelView).get_adapter()
            when (dragEvent.action) {
                DragEvent.ACTION_DROP -> {
                    if (adapter.is_dragging()) {
                        val (from_channel, from_line) = adapter.dragging_position!!
                        val (to_channel, to_line) = view.get_std_position()
                        val opus_manager = this.get_opus_manager()
                        opus_manager.move_line(
                            from_channel,
                            from_line,
                            to_channel,
                            to_line
                        )

                    }
                    adapter.stop_dragging()
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    adapter.stop_dragging()
                }
                else -> { }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.textView.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
        val line_height = resources.getDimension(R.dimen.line_height)
        this.layoutParams.height = line_height.toInt()
        this.layoutParams.width = WRAP_CONTENT
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this.update_queued = true
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        return this.build_drawable_state(drawableState)
    }

    // Prevents the child labels from blocking the parent onTouchListener events
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return true
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        val opus_manager = this.get_opus_manager()
        val (channel, line_offset) = this.get_row()
        when (opus_manager.opusManagerCursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Row -> {
                if (opus_manager.opusManagerCursor.channel == channel && opus_manager.opusManagerCursor.line_offset == line_offset) {
                    mergeDrawableStates(drawableState, STATE_FOCUSED)
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = opus_manager.opusManagerCursor.range!!
                if ((channel > first.channel && channel < second.channel) || (channel == first.channel && line_offset >= first.line_offset) || (channel == second.channel && line_offset <= second.line_offset)) {
                    mergeDrawableStates(drawableState, STATE_FOCUSED)
                }
            }
            else -> { }
        }
        return drawableState
    }

    fun set_text(text: String) {
        this.textView.text = text
        this.contentDescription = text
    }
    fun get_opus_manager(): OpusManager {
        return (this.viewHolder.bindingAdapter as LineLabelRecyclerAdapter).get_opus_manager()
    }

    fun get_adapter(): LineLabelRecyclerAdapter {
        return this.viewHolder.bindingAdapter as LineLabelRecyclerAdapter
    }

    fun get_row(): Pair<Int, Int> {
        val opus_manager = this.get_opus_manager()
        return opus_manager.get_std_offset(this.get_position())
    }

    fun get_position(): Int {
        return this.viewHolder.bindingAdapterPosition
    }
    fun get_std_position(): Pair<Int, Int> {
        return this.get_opus_manager().get_std_offset(this.get_position())
    }
}