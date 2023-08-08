package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import kotlin.concurrent.thread

class CellPlaceHolder(var viewHolder: CellRecyclerViewHolder, column_width: Int): LinearLayout(viewHolder.itemView.context) {
    init {
        var item_view = this.viewHolder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)

        this.layoutParams.width = column_width
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()

    }
    fun replace() {
        CellLayout(this.viewHolder)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.replace()
        //thread {
        //    //Thread.sleep(500)
        //    this.viewHolder.get_activity().runOnUiThread {

        //    }
        //}
    }
}