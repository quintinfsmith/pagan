package com.qfs.pagan

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView

class ColumnRecyclerViewHolder(context: Context): RecyclerView.ViewHolder(LinearLayout(context)) {
    init {
        this.itemView.background = context.resources.getDrawable(R.color.blue_sky)
    }
    fun get_cell_recycler(): CellRecycler {
        return (this.itemView as ViewGroup).children.first() as CellRecycler
    }
}
