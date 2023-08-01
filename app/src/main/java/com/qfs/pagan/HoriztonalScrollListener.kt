package com.qfs.pagan

import androidx.recyclerview.widget.RecyclerView

class HorizontalScrollListener: RecyclerView.OnScrollListener() {
    var absolute_x = 0
    override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
        super.onScrolled(recyclerView, x, y)
        val adapter = recyclerView.adapter as ColumnRecyclerAdapter
        this.absolute_x += x
        adapter.column_label_recycler.scrollBy(x, 0)
    }
}
