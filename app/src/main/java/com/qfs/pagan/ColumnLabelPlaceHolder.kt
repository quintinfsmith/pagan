package com.qfs.pagan

import android.view.ViewGroup
import android.widget.LinearLayout

class ColumnLabelPlaceHolder(var viewHolder: ColumnLabelViewHolder, column_width: Int): LinearLayout(viewHolder.itemView.context) {
    init {
        val item_view = this.viewHolder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)
        this.layoutParams.width = column_width
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()

    }
    fun replace() {
        ColumnLabelView(this.viewHolder)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.replace()
    }
}