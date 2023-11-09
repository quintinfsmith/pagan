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
        for (y in 0 until this.get_opus_manager().get_visible_line_count()) {
            this.insert_cell(y)
        }
    }

    fun insert_cell(y: Int) {
        this.addView(CellLayout(this.context), y)
    }
    fun remove_cell(y: Int) {
        this.removeViewAt(y)
    }
    fun notify_state_change(y: Int) {
        this.remove_cell(y)
        this.insert_cell(y)
        //(this.getChildAt(y) as CellLayout).invalidate_all()
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
