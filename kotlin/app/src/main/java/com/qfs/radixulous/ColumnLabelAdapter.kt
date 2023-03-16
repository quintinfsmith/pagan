package com.qfs.radixulous

import android.content.Context
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        this.recycler.layoutManager = LinearLayoutManager(
            this.recycler.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    fun addColumnLabel(default_width: Int = 1) {
        this.column_widths.add(default_width)
        this.notifyItemInserted(this.column_widths.size - 1)
    }

    fun removeColumnLabel() {
        if (this.column_widths.isNotEmpty()) {
            this.column_widths.removeLast()
            this.notifyItemRemoved(this.column_widths.size)
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
        var beat = holder.bindingAdapterPosition

        var item_view = holder.itemView
        item_view.layoutParams.width = (120 * this.column_widths[beat])
        item_view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    override fun onBindViewHolder(holder: ColumnLabelViewHolder, position: Int) {
        var item_view = holder.itemView as LabelView
        item_view.set_text(position.toString())
    }

    fun set_label_width(beat: Int, width: Int) {
        this.column_widths[beat] = width
        this.notifyItemChanged(beat)
    }

    override fun getItemCount(): Int {
        return this.column_widths.size
    }

    fun scroll(x: Int) {
        this.recycler.scrollBy(x, 0)
    }
}
