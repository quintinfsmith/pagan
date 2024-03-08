package com.qfs.pagan

import android.view.ViewGroup
import android.widget.LinearLayout

class ColumnLabelPlaceHolder(private var _view_holder: ColumnLabelViewHolder, column_width: Int): LinearLayout(_view_holder.itemView.context) {
    init {
        val item_view = this._view_holder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)
        this.layoutParams.width = column_width
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()

    }
    private fun _replace() {
        ColumnLabelView(this._view_holder)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this._replace()
    }
}