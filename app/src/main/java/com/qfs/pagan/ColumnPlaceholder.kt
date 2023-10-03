package com.qfs.pagan

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import kotlin.concurrent.thread

class ColumnPlaceholder(var viewHolder: ColumnRecyclerViewHolder, column_width: Int): LinearLayout(ContextThemeWrapper((viewHolder.itemView.context as ContextThemeWrapper).baseContext, R.style.placeholder_outer)) {
    init {
        val item_view = this.viewHolder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)
        (item_view as LinearLayout).orientation = VERTICAL

        this.layoutParams.width = column_width
        this.layoutParams.height = MATCH_PARENT
    }

    private fun replace() {
        try {
            CellRecycler(this.viewHolder)
        } catch (e: NullPointerException) {
            // Tried to replace after the viewHolder was detached
            // TODO: Log
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        thread {
            // Kludge
            Thread.sleep(50)
            if (this.viewHolder.bindingAdapterPosition != -1) {
                val adapter = (this.viewHolder.bindingAdapter as ColumnRecyclerAdapter)
                val activity = adapter.get_activity()
                activity.runOnUiThread {
                    this.replace()
                }
            }
        }

    }
}