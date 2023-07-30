package com.qfs.pagan

import android.content.Context
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CellRecycler(context: Context, var viewHolder: ColumnViewHolder): RecyclerView(context) {
    init {
        this.adapter = CellRecyclerAdapter()
        this.layoutManager = LinearLayoutManager(context, VERTICAL, false)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = MATCH_PARENT
        (this.adapter as CellRecyclerAdapter).update_width()
    }

    //-------------------------------------------------------//


    //-------------------------------------------------------//
    fun get_beat(): Int {
        return this.viewHolder!!.bindingAdapterPosition
    }

    fun get_column_adapter(): ColumnRecyclerAdapter {
        return this.viewHolder!!.bindingAdapter as ColumnRecyclerAdapter
    }
}