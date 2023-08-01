package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.InterfaceLayer as OpusManager

class LineLabelRecyclerAdapter(editor_table: EditorTable): RecyclerView.Adapter<LineLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    private var _dragging_lineLabel: View? = null
    private var recycler: LineLabelRecyclerView

    init {
        this.recycler = editor_table.line_label_recycler
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(this.recycler.context)
        this.recycler.itemAnimator = null

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    that.refresh()
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.refresh()
                }
            }
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineLabelViewHolder {
        val label = LineLabelView(parent.context)

        //label.setOnClickListener {
        //    this.interact_lineLabel(it as LineLabelView)
        //}

        //label.setOnFocusChangeListener { view, is_focused: Boolean ->
        //    if (is_focused) {
        //        this.interact_lineLabel(view as LineLabelView)
        //    }
        //}

        //label.setOnTouchListener { view: View, touchEvent: MotionEvent ->
        //    if (touchEvent.action == MotionEvent.ACTION_MOVE) {
        //        if (this._dragging_lineLabel == null) {
        //            this._dragging_lineLabel = view
        //            view.startDragAndDrop(
        //                null,
        //                View.DragShadowBuilder(view),
        //                null,
        //                0
        //            )
        //            return@setOnTouchListener true
        //        }
        //    } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
        //        this._dragging_lineLabel = null
        //    }
        //    false
        //}

        //label.setOnDragListener { view: View, dragEvent: DragEvent ->
        //    when (dragEvent.action) {
        //        DragEvent.ACTION_DROP -> {
        //            val from_label =  this._dragging_lineLabel
        //            if (from_label != null && from_label != view) {
        //                val from_channel = (from_label as LineLabelView).channel
        //                val from_line = from_label.line_offset
        //                val to_channel = (view as LineLabelView).channel
        //                val to_line = view.line_offset + 1

        //                this.opus_manager.move_line(
        //                    from_channel,
        //                    from_line,
        //                    to_channel,
        //                    to_line
        //                )

        //            }
        //            this._dragging_lineLabel = null
        //        }
        //        DragEvent.ACTION_DRAG_ENDED -> {
        //            this._dragging_lineLabel = null
        //        }
        //        else -> { }
        //    }
        //    true
        //}

        return LineLabelViewHolder(label)
    }

    override fun onViewAttachedToWindow(holder: LineLabelViewHolder) {
        //val label_view = (holder.itemView as LineLabelView)
        //val opus_manager = this.get_opus_manager()
        //label_view.set_text(this.get_label_text())
    }

    override fun onBindViewHolder(holder: LineLabelViewHolder, position: Int) {
        val opus_manager = this.get_opus_manager()
        val (channel, line_offset) = opus_manager.get_std_offset(position)
        val label = this.get_label_text(channel, line_offset)

        val label_view = (holder.itemView as LineLabelView)
        label_view.set_text(label)
        this.update_label_focus(label_view)
    }

    fun update_label_focus(label_view: LineLabelView) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        //when (cursor.mode) {
        //    Cursor.CursorMode.Row -> {
        //        if (cursor.channel == channel && cursor.line_offset == line_offset) {
        //            label_view.set_focused(true)
        //        } else {
        //            label_view.set_focused(false)
        //        }
        //    }
        //    Cursor.CursorMode.Single -> {
        //        if (cursor.channel == channel && cursor.line_offset == line_offset) {
        //            label_view.set_focused(true)
        //        } else {
        //            label_view.set_focused(false)
        //        }
        //    }
        //    Cursor.CursorMode.Range -> {
        //        val from_key = cursor.range!!.first
        //        val to_key = cursor.range!!.second

        //        val is_focused = if (from_key.channel != to_key.channel) {
        //            if (channel == from_key.channel) {
        //                line_offset >= from_key.line_offset
        //            } else if (channel == to_key.channel) {
        //                line_offset <= to_key.channel
        //            } else {
        //                (from_key.channel + 1 until to_key.channel).contains(channel)
        //            }
        //        } else {
        //            channel == from_key.channel && line_offset in (from_key.line_offset..to_key.line_offset)
        //        }
        //        label_view.set_focused(is_focused)
        //    }
        //    else -> {
        //        label_view.set_focused(false)
        //    }
        //}
    }

    private fun get_label_text(channel: Int, line_offset: Int): String {
        val opus_manager = this.get_opus_manager()
        return if (!opus_manager.is_percussion(channel)) {
            "$channel::$line_offset"
        } else {
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            "!$instrument"
        }
    }

    override fun getItemCount(): Int {
        val opus_manager = this.get_opus_manager()
        return opus_manager.get_total_line_count()
    }

    fun scrollToLine(y: Int) {
        val current_y = this.recycler.computeVerticalScrollOffset()
        this.recycler.scrollBy(0, y - current_y)
    }

    private fun interact_lineLabel(view: LineLabelView) {
        //val rvTable = this.activity.findViewById<RecyclerView>(R.id.rvTable)
        //val adapter = rvTable.adapter as BeatColumnAdapter

        //if (adapter.linking_beat != null) {
        //    try {
        //        if (adapter.linking_beat_b == null) {
        //            this.opus_manager.link_row(
        //                view.channel,
        //                view.line_offset,
        //                adapter.linking_beat!!
        //            )
        //        } else {

        //            this.opus_manager.link_beat_range_horizontally(
        //                view.channel,
        //                view.line_offset,
        //                adapter.linking_beat!!,
        //                adapter.linking_beat_b!!
        //            )
        //        }
        //    } catch (e: Exception) {
        //        if (e is LinksLayer.MixedLinkException) {
        //            this.activity.feedback_msg("Can't Link percussion with non-percussion")
        //        } else {
        //            throw e
        //        }
        //    }
        //    adapter.cancel_linking()
        //}
        //this.opus_manager.cursor_select_row(
        //    view.channel,
        //    view.line_offset
        //)
    }

    fun refresh() {
        val start = (this.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val end = (this.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        // NOTE: padding the start/end since an item may be bound but not visible
        for (i in Integer.max(0, start - 1)..Integer.min(this.itemCount, end + 1)) {
            this.notifyItemChanged(i)
        }
    }

    fun set_cursor_focus(show: Boolean = true) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.mode) {
            Cursor.CursorMode.Range -> {
                val (from_key, to_key) = cursor.range!!
                val offset_y = opus_manager.get_abs_offset(from_key.channel, from_key.line_offset)
                val (diff_y, _) = opus_manager.get_abs_difference(from_key, to_key)
                for (i in offset_y .. offset_y + diff_y) {
                    val viewHolder = this.recycler.findViewHolderForAdapterPosition(i) ?: return
                    val label = viewHolder.itemView as LineLabelView
                }
            }
            Cursor.CursorMode.Single,
            Cursor.CursorMode.Row -> {
                val y_offset = try {
                    opus_manager.get_abs_offset(
                        cursor.channel,
                        cursor.line_offset
                    )
                } catch (e: IndexOutOfBoundsException) {
                    // If the abs_offset can't be found, presumably the label no longer exists
                    // if we are trying to unhighlight that label, mission accomplished implicitly
                    if (show) {
                        throw e
                    } else {
                        return
                    }
                }
                val viewHolder = this.recycler.findViewHolderForAdapterPosition(y_offset) ?: return
            }
            Cursor.CursorMode.Column,
            Cursor.CursorMode.Unset -> { }
        }
    }

    fun get_activity(): MainActivity {
        return this.recycler.context as MainActivity
    }
    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }
}
