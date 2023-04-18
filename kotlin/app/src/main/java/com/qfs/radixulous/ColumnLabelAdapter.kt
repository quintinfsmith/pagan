package com.qfs.radixulous

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

@SuppressLint("ClickableViewAccessibility")
class ColumnLabelAdapter(var main_fragment: MainFragment, var recycler: RecyclerView) : RecyclerView.Adapter<ColumnLabelAdapter.ColumnLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    var column_widths = mutableListOf<Int>()

    class LabelView(context: Context): RelativeLayout(context) {
        var viewHolder: ColumnLabelViewHolder? = null
        var textView: TextView =LayoutInflater.from(this.context).inflate(
            R.layout.table_column_label,
            this,
            false
        ) as TextView

        init {
            this.addView(this.textView)
            this.textView.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        }
        fun set_text(text: String) {
            this.textView.text = text
        }
    }

    class ColumnLabelViewHolder(itemView: LabelView) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.viewHolder = this
        }
    }

    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = TimeableLayoutManager(
            this.recycler.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        //(this.recycler.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
        this.recycler.itemAnimator = null

        var that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeChanged(start: Int, count: Int) {
                    for (i in start until that.itemCount) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i) ?: continue
                        that.adjust_width(viewHolder as ColumnLabelViewHolder)
                    }
                }
                override fun onItemRangeRemoved(start: Int, count: Int) {
                    val end = (that.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    for (i in start .. end) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i + count) ?: continue
                        that.set_text(viewHolder as ColumnLabelViewHolder, i)
                    }
                }
                override fun onItemRangeInserted(start: Int, count: Int) {
                    val end = (that.recycler.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    for (i in start .. end) {
                        val viewHolder = that.recycler.findViewHolderForAdapterPosition(i) ?: continue
                        that.set_text(viewHolder as ColumnLabelViewHolder, i + count)
                    }
                    val visible_start = (that.recycler.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    if (visible_start >= start) {
                        that.recycler.scrollToPosition(start)
                    }
                }
            }
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
            var holder = (it as LabelView).viewHolder ?: return@setOnClickListener
            var beat = holder.bindingAdapterPosition
            this.main_fragment.select_column(beat)
        }

        label.setOnFocusChangeListener { view, is_focused: Boolean ->
            if (is_focused) {
                var holder = (view as LabelView).viewHolder ?: return@setOnFocusChangeListener
                var beat = holder.bindingAdapterPosition
                this.main_fragment.select_column(beat)
            }
        }

        return ColumnLabelViewHolder(label)
    }

    override fun onViewAttachedToWindow(holder: ColumnLabelViewHolder) {
        super.onViewAttachedToWindow(holder)
        this.adjust_width(holder)
    }

    fun adjust_width(holder: ColumnLabelViewHolder) {
        var beat = holder.bindingAdapterPosition

        var item_view = holder.itemView
        item_view.layoutParams.width = (120 * this.column_widths[beat])
        item_view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    override fun onBindViewHolder(holder: ColumnLabelViewHolder, position: Int) {
        this.set_text(holder, position)
    }

    fun set_text(holder: ColumnLabelViewHolder, position: Int) {
        var item_view = holder.itemView as LabelView
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
}
