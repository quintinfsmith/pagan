package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView

class ColumnRecyclerViewHolder(context: Context): RecyclerView.ViewHolder(LinearLayout(context)) {
    fun get_cell_recycler(): CellRecycler {
        return (this.itemView as ViewGroup).children.first() as CellRecycler
    }
}
