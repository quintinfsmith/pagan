package com.qfs.pagan

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper

class ColumnPlaceholder(var viewHolder: ColumnRecyclerViewHolder, column_width: Int): LinearLayout((viewHolder.itemView.context as ContextThemeWrapper).baseContext) {
    init {
        var item_view = this.viewHolder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)
        (item_view as LinearLayout).orientation = VERTICAL

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