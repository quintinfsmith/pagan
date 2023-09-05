package com.qfs.pagan

import androidx.recyclerview.widget.RecyclerView

class VerticalScrollListener(): RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
        super.onScrolled(recyclerView, x, y)
        if ((recyclerView as CellRecycler).is_propagation_locked()) {
            return
        }


        val main_adapter = (recyclerView as CellRecycler).get_column_recycler_adapter() as ColumnRecyclerAdapter
        main_adapter.apply_to_visible_columns {
            if (recyclerView.adapter == it) {
                return@apply_to_visible_columns
            }

            val propagated_recycler = it.recycler
            main_adapter.get_activity().runOnUiThread {
                propagated_recycler.lock_scroll_propagation()
                propagated_recycler.scrollBy(x, y)
                propagated_recycler.unlock_scroll_propagation()
            }
        }
        val editor_table = main_adapter.get_editor_table()!!
        editor_table.line_label_recycler.scrollBy(x, y)
    }
}

