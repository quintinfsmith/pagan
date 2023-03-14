package com.qfs.radixulous

import android.content.Context
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColumnLabelAdapter(var main_fragment: MainFragment, var recycler: RecyclerView) : RecyclerView.Adapter<ColumnLabelAdapter.ColumnLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    var column_widths = mutableListOf<Int>()
    class LabelView(context: Context): LinearLayout(context) {
        var viewHolder: ColumnLabelViewHolder? = null
        var textView: TextView = LayoutInflater.from(this.context).inflate(
            R.layout.table_column_label,
            this,
            false
        ) as TextView

        init {
            this.addView(textView)
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
        this.recycler.layoutManager = LinearLayoutManager(
            this.recycler.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    fun addColumnLabel() {
        this.column_widths.add(1)
        this.notifyItemInserted(this.column_widths.size - 1)
    }

    fun removeColumnLabel(i: Int) {
        if (i < this.column_widths.size) {
            this.column_widths.removeAt(i)
            this.notifyItemRemoved(i)
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

    override fun onBindViewHolder(holder: ColumnLabelViewHolder, position: Int) {
        var item_view = holder.itemView as LabelView
        item_view.set_text(position.toString())
        item_view.textView.width = (120 * this.column_widths[position]) - 10
    }

    fun set_label_width(beat: Int, width: Int) {
        this.column_widths[beat] = width
        this.notifyItemChanged(beat)
    }

    override fun getItemCount(): Int {
        return this.column_widths.size
    }

    fun scrollToX(x: Int) {
        var current_x = this.recycler.computeHorizontalScrollOffset()
        this.recycler.scrollBy(x - current_x, 0)
    }
}
