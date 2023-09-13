package com.qfs.pagan

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import kotlin.concurrent.thread

class CellPlaceHolder(var viewHolder: CellRecyclerViewHolder, column_width: Int): LinearLayout(
    ContextThemeWrapper(viewHolder.itemView.context, R.style.placeholder_outer)
) {
    init {
        val item_view = this.viewHolder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)

        this.layoutParams.width = column_width
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()

    }

    fun replace() {
        try {
            CellLayout(this.viewHolder)
        } catch (e: CellRecycler.CellDetachedException) {
            // let it pass
        } catch (e: CellRecycler.ColumnDetachedException) {
            // let it pass
        }

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        thread {
            // Kludge
            Thread.sleep(10)
            if (this.viewHolder.bindingAdapterPosition != -1) {
                var adapter = (this.viewHolder.bindingAdapter as CellRecyclerAdapter)
                var activity = adapter.get_activity()
                activity.runOnUiThread {
                    this.replace()
                }
            }
        }

    }
}