package com.qfs.radixulous

import android.content.Context
import android.view.*
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColumnLabelAdapter(var main_fragment: MainFragment, var recycler: RecyclerView) : RecyclerView.Adapter<ColumnLabelAdapter.ColumnLabelViewHolder>() {
    // BackLink so I can get the x offset from a view in the view holder
    private var column_count = 0
    class LabelView(context: Context): androidx.appcompat.widget.AppCompatTextView(ContextThemeWrapper(context, R.style.column_label)) {
        var viewHolder: ColumnLabelViewHolder? = null
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
        this.column_count += 1
        this.notifyItemInserted(this.column_count - 1)
    }
    fun removeColumnLabel() {
        this.column_count -= 1
        this.notifyItemRemoved(this.column_count)
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
        (holder.itemView as LabelView).text = position.toString()
        println("SET LABEL: $position")
    }

    fun set_label_width(beat: Int, width: Int) {
        var holder = this.recycler.findViewHolderForAdapterPosition(beat) ?: return
        var item_view = holder.itemView
        val param = item_view.layoutParams as ViewGroup.LayoutParams
        param.width = width
    }

    override fun getItemCount(): Int {
        return this.column_count
    }
}
