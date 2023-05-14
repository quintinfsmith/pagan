package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.qfs.pagan.opusmanager.LinksLayer

class LineLabelAdapter(
    private var opus_manager: InterfaceLayer,
    private var recycler: RecyclerView,
    var activity: MainActivity
    ): RecyclerView.Adapter<LineLabelAdapter.LineLabelViewHolder>() {
    class LineLabelRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
        // Prevents this from intercepting linelabel touch events (disables manual scrolling)
        override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
            return false
        }
    }

    // BackLink so I can get the x offset from a view in the view holder
    private var row_count = 0
    private var _dragging_lineLabel: View? = null

    class LabelView(context: Context): LinearLayout(ContextThemeWrapper(context, R.style.line_label_outer)) {
        var viewHolder: LineLabelViewHolder? = null
        private var textView = TextView(ContextThemeWrapper(this.context, R.style.line_label_inner))
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
        // Prevents the child labels from blocking the parent onTouchListener events
        override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
            return true
        }

        fun set_row(channel: Int, line_offset: Int) {
            this.channel = channel
            this.line_offset = line_offset
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
        (this.recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

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

                        this.opus_manager.cursor_select_row(
                            to_channel,
                            if (from_channel == to_channel) {
                                if (from_line < to_line) {
                                    to_line - 1
                                } else {
                                    to_line
                                }
                            } else if (this.opus_manager.channels[to_channel].size == 1) {
                                0
                            } else {
                                to_line
                            }
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

    override fun onBindViewHolder(holder: LineLabelViewHolder, position: Int) {
        val (channel, line_offset) = this.opus_manager.get_std_offset(position)
        val label = this.get_label_text(channel, line_offset)
        (holder.itemView as LabelView).set_text(label)
        (holder.itemView as LabelView).set_row(channel, line_offset)
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
}
