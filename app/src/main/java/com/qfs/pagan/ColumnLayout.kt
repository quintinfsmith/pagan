package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnLayout(var view_holder: ColumnRecyclerViewHolder): LinearLayout((view_holder.itemView.context as ContextThemeWrapper).baseContext) {
    class ColumnDetachedException: Exception()
    init {
        this.orientation = VERTICAL
        (this.view_holder.itemView as ViewGroup).removeAllViews()
        (this.view_holder.itemView as ViewGroup).addView(this)

        this.layoutParams.height = MATCH_PARENT
        this.layoutParams.width = WRAP_CONTENT

        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.populate()
    }

    fun populate() {
        val opus_manager = this.get_opus_manager()
        for (y in 0 until opus_manager.get_visible_line_count()) {
            this.addView(CellLayout(this.context))
        }
    }

    fun insert_cells(y: Int, count: Int) {
        for (i in y until y + count) {
            this.addView(CellLayout(this.context), i)
        }
    }

    fun insert_cell(y: Int) {
        this.insert_cells(y, 1)
    }

    fun remove_cell(y: Int) {
        this.removeViewAt(y)
        this.notifyItemRangeChanged(y,this.childCount - 1 - y)
    }

    fun remove_cells(y: Int, count: Int) {
        this.removeViews(y, count)
        //this.notifyItemRangeChanged(y,this.childCount)
    }

    fun notifyItemChanged(y: Int, state_only: Boolean = false) {
        this.notifyItemRangeChanged(y, 1, state_only)
    }

    fun notifyItemRangeChanged(y: Int, count: Int, state_only: Boolean = false) {
        for (i in 0 until count) {
            if (state_only) {
                (this.getChildAt(y + i) as CellLayout).invalidate_all()
            } else {
                this.rebind(i + y)
            }
        }
    }

    fun rebind(index: Int) {
        this.removeViewAt(index)
        this.addView(CellLayout(this.context), index)
    }

    fun clear() {
        this.removeAllViews()
    }

    fun get_opus_manager(): OpusManager {
        return (this.view_holder.bindingAdapter as ColumnRecyclerAdapter).get_opus_manager()
    }

    fun get_beat(): Int {
        return this.view_holder.bindingAdapterPosition
    }

    fun get_column_recycler_adapter(): ColumnRecyclerAdapter {
        if (this.view_holder.bindingAdapter == null) {
            throw ColumnDetachedException()
        }
        return this.view_holder.bindingAdapter as ColumnRecyclerAdapter
    }
    fun get_editor_table(): EditorTable {
        return this.get_column_recycler_adapter().get_editor_table()!!
    }
}
