package com.qfs.pagan

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import kotlin.concurrent.thread

class CellPlaceHolder(var view_holder: CellRecyclerViewHolder, column_width: Int): LinearLayout(
    ContextThemeWrapper(view_holder.itemView.context, R.style.placeholder_outer)
) {
    init {
        val item_view = this.view_holder.itemView as ViewGroup
        item_view.removeAllViews()
        item_view.addView(this)

        this.layoutParams.width = column_width
        this.layoutParams.height = resources.getDimension(R.dimen.line_height).toInt()

    }

    private fun replace() {
        try {
            CellLayout(this.view_holder)
        } catch (e: CellRecycler.CellDetachedException) {
            // let it pass
        } catch (e: CellRecycler.ColumnDetachedException) {
            // let it pass
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        thread {
            if (this.view_holder.bindingAdapterPosition != -1) {
                val adapter = (this.view_holder.bindingAdapter as CellRecyclerAdapter)
                val activity = adapter.get_activity()
                activity.runOnUiThread {
                    this.replace()
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}