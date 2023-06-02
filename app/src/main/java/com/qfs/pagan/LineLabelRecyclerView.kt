package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.qfs.pagan.opusmanager.LinksLayer

class LineLabelRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    // Prevents this from intercepting linelabel touch events (disables manual scrolling)
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return false
    }
    class LineLabelAdapter(
        private var opus_manager: InterfaceLayer,
        private var recycler: RecyclerView,
        var activity: MainActivity
    ): RecyclerView.Adapter<LineLabelAdapter.LineLabelViewHolder>() {
        // BackLink so I can get the x offset from a view in the view holder
        private var row_count = 0
        private var _dragging_lineLabel: View? = null

        class LabelView(context: Context): LinearLayout(ContextThemeWrapper(context, R.style.line_label_outer)) {
            class InnerView(context: Context): androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(context, R.style.line_label_inner)) {
                private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
                var state_focused: Boolean = false

                override fun onCreateDrawableState(extraSpace: Int): IntArray? {
                    val drawableState = super.onCreateDrawableState(extraSpace + 1)
                    if (this.state_focused) {
                        mergeDrawableStates(drawableState, STATE_FOCUSED)
                    }
                    return drawableState
                }

                fun set_focused(value: Boolean) {
                    this.state_focused = value
                    this.refreshDrawableState()
                }
            }
            var viewHolder: LineLabelViewHolder? = null
            private var textView = InnerView(context)
            /*
             * update_queued exists to handle the liminal state between being detached and being destroyed
             * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
             */
            var update_queued = false
            var channel = -1
            var line_offset = -1

            init {
                this.addView(textView)
            }
            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                this.textView.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()
                this.layoutParams.height = WRAP_CONTENT
            }
            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                this.update_queued = true
            }
            // Prevents the child labels from blocking the parent onTouchListener events
            override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
                return true
            }

            fun set_row(channel: Int, line_offset: Int) {
                this.channel = channel
                this.line_offset = line_offset
            }
            fun set_focused(value: Boolean) {
                this.textView.set_focused(value)
                this.refreshDrawableState()
            }

            fun set_text(text: String) {
                this.textView.text = text
            }
        }

        class LineLabelViewHolder(itemView: LabelView) : RecyclerView.ViewHolder(itemView) {
            init {
                itemView.viewHolder = this
            }
        }

        init {
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

        fun addLineLabel() {
            this.row_count += 1
            this.notifyItemInserted(this.row_count - 1)
        }

        fun removeLineLabel(i: Int) {
            if (this.row_count > 0) {
                this.row_count -= 1
                this.notifyItemRemoved(i)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineLabelViewHolder {
            val label = LabelView(parent.context)

            label.setOnClickListener {
                this.interact_lineLabel(it as LabelView)
            }

            label.setOnFocusChangeListener { view, is_focused: Boolean ->
                if (is_focused) {
                    this.interact_lineLabel(view as LabelView)
                }
            }

            label.setOnTouchListener { view: View, touchEvent: MotionEvent ->
                if (touchEvent.action == MotionEvent.ACTION_MOVE) {
                    if (this._dragging_lineLabel == null) {
                        this._dragging_lineLabel = view
                        view.startDragAndDrop(
                            null,
                            View.DragShadowBuilder(view),
                            null,
                            0
                        )
                        return@setOnTouchListener true
                    }
                } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
                    this._dragging_lineLabel = null
                }
                false
            }

            label.setOnDragListener { view: View, dragEvent: DragEvent ->
                when (dragEvent.action) {
                    DragEvent.ACTION_DROP -> {
                        val from_label =  this._dragging_lineLabel
                        if (from_label != null && from_label != view) {
                            val from_channel = (from_label as LabelView).channel
                            val from_line = from_label.line_offset
                            val to_channel = (view as LabelView).channel
                            val to_line = view.line_offset + 1

                            this.opus_manager.move_line(
                                from_channel,
                                from_line,
                                to_channel,
                                to_line
                            )

                        }
                        this._dragging_lineLabel = null
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        this._dragging_lineLabel = null
                    }
                    else -> { }
                }
                true
            }

            return LineLabelViewHolder(label)
        }

        override fun onViewAttachedToWindow(holder: LineLabelViewHolder) {
            val label_view = (holder.itemView as LabelView)

            // Redraw Items that were detached but not destroyed
            if (label_view.update_queued) {
                this.update_label_focus(label_view)
                label_view.update_queued = false
            }
        }

        override fun onBindViewHolder(holder: LineLabelViewHolder, position: Int) {
            val (channel, line_offset) = this.opus_manager.get_std_offset(position)
            val label = this.get_label_text(channel, line_offset)
            val label_view = (holder.itemView as LabelView)
            label_view.set_text(label)
            label_view.set_row(channel, line_offset)
            this.update_label_focus(label_view)
        }

        fun update_label_focus(label_view: LabelView) {
            val channel = label_view.channel
            val line_offset = label_view.line_offset
            val cursor = this.opus_manager.cursor
            when (cursor.mode) {
                Cursor.CursorMode.Row -> {
                    if (cursor.channel == channel && cursor.line_offset == line_offset) {
                        label_view.set_focused(true)
                    } else {
                        label_view.set_focused(false)
                    }
                }
                Cursor.CursorMode.Single -> {
                    if (cursor.channel == channel && cursor.line_offset == line_offset) {
                        label_view.set_focused(true)
                    } else {
                        label_view.set_focused(false)
                    }
                }
                Cursor.CursorMode.Range -> {
                    val from_key = cursor.range!!.first
                    val to_key = cursor.range!!.second

                    val is_focused = if (from_key.channel != to_key.channel) {
                        if (channel == from_key.channel) {
                            line_offset >= from_key.line_offset
                        } else if (channel == to_key.channel) {
                            line_offset <= to_key.channel
                        } else {
                            (from_key.channel + 1 until to_key.channel).contains(channel)
                        }
                    } else {
                        channel == from_key.channel && line_offset in (from_key.line_offset..to_key.line_offset)
                    }
                    label_view.set_focused(is_focused)
                }
                else -> {
                    label_view.set_focused(false)
                }
            }
        }

        private fun get_label_text(channel: Int, line_offset: Int): String {
            return if (!this.opus_manager.is_percussion(channel)) {
                "$channel::$line_offset"
            } else {
                val instrument = this.opus_manager.get_percussion_instrument(line_offset)
                "!$instrument"
            }
        }

        override fun getItemCount(): Int {
            return this.row_count
        }

        fun scrollToLine(y: Int) {
            val current_y = this.recycler.computeVerticalScrollOffset()
            this.recycler.scrollBy(0, y - current_y)
        }

        private fun interact_lineLabel(view: LabelView) {
            val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
            val adapter = rvBeatTable.adapter as BeatColumnAdapter

            if (adapter.linking_beat != null) {
                try {
                    if (adapter.linking_beat_b == null) {
                        this.opus_manager.link_row(
                            view.channel,
                            view.line_offset,
                            adapter.linking_beat!!
                        )
                    } else {

                        this.opus_manager.link_beat_range_horizontally(
                            view.channel,
                            view.line_offset,
                            adapter.linking_beat!!,
                            adapter.linking_beat_b!!
                        )
                    }
                } catch (e: Exception) {
                    if (e is LinksLayer.MixedLinkException) {
                        this.activity.feedback_msg("Can't Link percussion with non-percussion")
                    } else {
                        throw e
                    }
                }
                adapter.cancel_linking()
            }
            this.opus_manager.cursor_select_row(
                view.channel,
                view.line_offset
            )
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
            val cursor = this.opus_manager.cursor
            when (cursor.mode) {
                Cursor.CursorMode.Range -> {
                    val (from_key, to_key) = cursor.range!!
                    val offset_y = this.opus_manager.get_abs_offset(from_key.channel, from_key.line_offset)
                    val (diff_y, _) = this.opus_manager.get_abs_difference(from_key, to_key)
                    for (i in offset_y .. offset_y + diff_y) {
                        val viewHolder = this.recycler.findViewHolderForAdapterPosition(i) ?: return
                        val label = viewHolder.itemView as LabelView
                        label.set_focused(show)
                        label.invalidate()
                    }
                }
                Cursor.CursorMode.Single,
                Cursor.CursorMode.Row -> {
                    val y_offset = this.opus_manager.get_abs_offset(cursor.channel, cursor.line_offset)
                    val viewHolder = this.recycler.findViewHolderForAdapterPosition(y_offset) ?: return
                    val label = viewHolder.itemView as LabelView
                    label.set_focused(show)
                    label.invalidate()
                }
                Cursor.CursorMode.Column,
                Cursor.CursorMode.Unset -> { }
            }
        }
    }
}


