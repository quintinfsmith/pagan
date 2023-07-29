package com.qfs.pagan

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CellRecycler(context: Context): RecyclerView(context) {
    var viewHolder: ColumnViewHolder? = null
    init {
        this.adapter = CellRecyclerAdapter(this)
        this.layoutManager = LinearLayoutManager(context, VERTICAL, false)
    }

    //-------------------------------------------------------//
    fun clear() {


    }
    fun build() {
        this.clear()

    }
    //-------------------------------------------------------//
    fun get_beat(): Int {
        return this.viewHolder!!.bindingAdapterPosition
    }

    fun get_column_adapter(): ColumnRecyclerAdapter {
        return this.viewHolder!!.bindingAdapter as ColumnRecyclerAdapter
    }
}