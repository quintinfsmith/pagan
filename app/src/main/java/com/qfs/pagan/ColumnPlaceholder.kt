package com.qfs.pagan

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout

class ColumnPlaceholder(var viewHolder: ColumnRecyclerViewHolder, column_width: Int): LinearLayout(viewHolder.itemView.context) {
    init {
        var item_view = this.viewHolder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)

        this.layoutParams.width = column_width
        this.layoutParams.height = MATCH_PARENT

    }
    fun replace() {
        CellRecycler(this.viewHolder)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.replace()
    }
}