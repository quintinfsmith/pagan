package com.qfs.radixulous

import android.content.Context
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RowLabelAdapter(var main_fragment: MainFragment, var recycler: RecyclerView) : RecyclerView.Adapter<RowLabelAdapter.RowLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    private var row_count = 0

    class LabelView(context: Context): LinearLayout(context) {
        var viewHolder: RowLabelViewHolder? = null

        var textView: TextView = LayoutInflater.from(this.context).inflate(
            R.layout.table_line_label,
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

    class RowLabelViewHolder(itemView: LabelView) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.viewHolder = this
        }
    }

    init {
        this.recycler.adapter = this
        this.recycler.layoutManager = LinearLayoutManager(this.recycler.context)
    }

    fun addRowLabel() {
        this.row_count += 1
        this.notifyItemInserted(this.row_count - 1)
    }

    fun removeRowLabel(i: Int) {
        if (this.row_count > 0) {
            this.row_count -= 1
            this.notifyItemRemoved(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowLabelViewHolder {
        val label = LabelView(parent.context)

        label.setOnClickListener {
             this.interact_rowLabel(it)
        }

        label.setOnFocusChangeListener { view, is_focused: Boolean ->
            if (is_focused) {
                this.interact_rowLabel(view)
            }
        }

        return RowLabelViewHolder(label)
    }

    override fun onBindViewHolder(holder: RowLabelViewHolder, position: Int) {
        var label = this.main_fragment.get_label_text(position)
        (holder.itemView as LabelView).set_text(label)
    }

    override fun getItemCount(): Int {
        return this.row_count
    }

    fun scrollToY(y: Int) {
        var current_y = this.recycler.computeVerticalScrollOffset()
        this.recycler.scrollBy(0, y - current_y)
    }

    private fun interact_rowLabel(view: View) {
        this.main_fragment.set_active_row(this.get_y(view))
    }

    private fun get_y(view: View): Int {
        var abs_y: Int = 0
        val label_column = view.parent!! as ViewGroup
        for (i in 0 until label_column.childCount) {
            if (label_column.getChildAt(i) == view) {
                abs_y = i
                break
            }
        }
        return abs_y
    }
}
