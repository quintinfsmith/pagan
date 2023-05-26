package com.qfs.pagan

import android.content.Context
import android.view.*
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColumnLabelAdapter(private var opus_manager: InterfaceLayer, var recycler: RecyclerView, var activity: MainActivity) : RecyclerView.Adapter<ColumnLabelAdapter.ColumnLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    var column_widths = mutableListOf<Int>()

    class LabelView(context: Context): RelativeLayout(ContextThemeWrapper(context, R.style.column_label_outer)) {
        var viewHolder: ColumnLabelViewHolder? = null
        private var textView = LineLabelRecyclerView.LineLabelAdapter.LabelView.InnerView(context)
        init {
            this.addView(this.textView)
            this.textView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }

        //override fun onAttachedToWindow() {
        //    val margin = resources.getDimension(R.dimen.normal_padding).toInt()
        //    (this.layoutParams as MarginLayoutParams).setMargins(margin,0,margin,0)
        //    this.layoutParams.width = resources.getDimension(R.dimen.base_leaf_width).toInt()
        //}

        fun set_text(text: String) {
            this.textView.text = text
        }

        fun set_focused(value: Boolean) {
            this.textView.set_focused(value)
            this.refreshDrawableState()
        }

    }

    class ColumnLabelViewHolder(itemView: LabelView) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.viewHolder = this
        }
    }

    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(
            this.recycler.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        //(this.recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        this.recycler.itemAnimator = null

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeChanged(start: Int, count: Int) {
                    for (i in start until that.itemCount) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i) ?: continue
                        that.adjust_width(viewHolder as ColumnLabelViewHolder)
                        that.update_label_focus(viewHolder.itemView as LabelView)
                    }
                }
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    val end = (that.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    for (i in start .. end) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i + count) ?: continue
                        that.set_text(viewHolder as ColumnLabelViewHolder, i)
                        that.update_label_focus(viewHolder.itemView as LabelView)
                    }
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    val end = (that.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    for (i in start .. end) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i) ?: continue
                        that.set_text(viewHolder as ColumnLabelViewHolder, i + count)
                        that.update_label_focus(viewHolder.itemView as LabelView)
                    }
                    val visible_start = (that.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (visible_start >= start) {
                        that.recycler.scrollToPosition(start)
                    }
                }
            }
        )
    }

    fun update_label_focus(label_view: LabelView) {
        val holder  = label_view.viewHolder ?: return
        val beat = holder.bindingAdapterPosition
        val cursor = this.opus_manager.cursor
        label_view.set_focused(
            when (cursor.mode) {
                Cursor.CursorMode.Column -> {
                    cursor.beat == beat
                }
                Cursor.CursorMode.Single -> {
                    cursor.beat == beat
                }
                Cursor.CursorMode.Range -> {
                    val from_key = cursor.range!!.first
                    val to_key = cursor.range!!.second

                    if (from_key.beat != to_key.beat) {
                        (from_key.beat .. to_key.beat).contains(beat)
                    } else {
                        beat == from_key.beat
                    }
                }
                else -> {
                    false
                }
            }
        )
    }

    fun addColumnLabel(position: Int) {
        if (position < this.column_widths.size) {
            this.column_widths.add(1)
            this.notifyItemInserted(this.column_widths.size - 1)
        } else {
            while (this.column_widths.size <= position) {
                this.column_widths.add(1)
                this.notifyItemInserted(this.column_widths.size - 1)
            }
        }
    }

    fun removeColumnLabel(index: Int) {
        if (index < this.column_widths.size) {
            this.column_widths.removeAt(index)
            this.notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnLabelViewHolder {
        val label = LabelView(parent.context)

        label.setOnClickListener {
            val holder = (it as LabelView).viewHolder ?: return@setOnClickListener
            val beat = holder.bindingAdapterPosition

            val rvBeatTable = this.activity.findViewById<RecyclerView>(R.id.rvBeatTable)
            val adapter = rvBeatTable.adapter as BeatColumnAdapter
            if (adapter.linking_beat != null) {
                adapter.cancel_linking()
            }

            this.opus_manager.cursor_select_column(beat)
        }

        label.setOnFocusChangeListener { view, is_focused: Boolean ->
            if (is_focused) {
                val holder = (view as LabelView).viewHolder ?: return@setOnFocusChangeListener
                val beat = holder.bindingAdapterPosition
                this.opus_manager.cursor_select_column(beat)
            }
        }

        return ColumnLabelViewHolder(label)
    }

    override fun onViewAttachedToWindow(holder: ColumnLabelViewHolder) {
        super.onViewAttachedToWindow(holder)
        this.adjust_width(holder)
    }

    fun adjust_width(holder: ColumnLabelViewHolder) {
        val beat = holder.bindingAdapterPosition

        val item_view = holder.itemView
        val resources = this.recycler.resources
        item_view.layoutParams.width = (resources.getDimension(R.dimen.base_leaf_width) * this.column_widths[beat].toFloat()).toInt()
        item_view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

    }

    override fun onBindViewHolder(holder: ColumnLabelViewHolder, position: Int) {
        this.set_text(holder, position)
    }

    fun set_text(holder: ColumnLabelViewHolder, position: Int) {
        val item_view = holder.itemView as LabelView
        item_view.set_text(position.toString())
    }

    fun set_label_width(beat: Int, width: Int) {
        if (this.column_widths[beat] != width) {
            this.column_widths[beat] = width
            this.notifyItemChanged(beat)
        }
    }

    override fun getItemCount(): Int {
        return this.column_widths.size
    }

    fun scroll(x: Int) {
        this.recycler.scrollBy(x, 0)
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
