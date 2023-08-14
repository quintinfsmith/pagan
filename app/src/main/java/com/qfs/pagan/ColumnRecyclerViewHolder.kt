package com.qfs.pagan

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView

class ColumnRecyclerViewHolder(context: Context): RecyclerView.ViewHolder(LinearLayout(context)) {
    init {
        this.setIsRecyclable(false)
    }
    fun get_cell_recycler(): CellRecycler? {
        return if ((this.itemView as ViewGroup).childCount > 0) {
            val item = (this.itemView as ViewGroup).getChildAt(0)
            if (item is CellRecycler) {
                item
            } else {
                null
            }
        } else {
            null
        }
    }
}
