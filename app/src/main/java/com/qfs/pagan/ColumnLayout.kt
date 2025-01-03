package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ColumnLayout(private var _view_holder: ColumnRecyclerViewHolder): LinearLayout((_view_holder.itemView.context as ContextThemeWrapper).baseContext) {
    class ColumnDetachedException: Exception()
    private var _populated = false
    var column_width_factor = 1
    init {
        this.orientation = VERTICAL
        (this._view_holder.itemView as ViewGroup).removeAllViews()
        (this._view_holder.itemView as ViewGroup).addView(this)

        this.layoutParams.height = MATCH_PARENT
        this.layoutParams.width = WRAP_CONTENT
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.column_width_factor = this._get_editor_table().get_column_width(this._view_holder.bindingAdapterPosition)
        this._populate()
    }

    private fun _populate() {
        this._populated = true
        val opus_manager = this.get_opus_manager()
        for (y in 0 until opus_manager.get_row_count()) {
            this.addView(CellLayout(this, y), y)
        }
    }

    fun get_item_count(): Int {
        return this.childCount
    }


    fun insert_cells(y: Int, count: Int) {
        for (i in y until y + count) {
            this.addView(CellLayout(this, i), i)
        }

        this.notifyItemRangeChanged(0, y)
        this.notifyItemRangeChanged(y + count, this.get_item_count() - (y + count))
    }

    fun remove_cells(y: Int, count: Int) {
        this.removeViews(y, count)
        this.notifyItemRangeChanged(0, y)
        this.notifyItemRangeChanged(y, this.get_item_count() - y)
    }

    fun notifyItemChanged(y: Int, state_only: Boolean = false) {
        this.notifyItemRangeChanged(y, 1, state_only)
    }

    fun notifyItemRangeChanged(y: Int, count: Int, state_only: Boolean = false) {
        for (i in 0 until count) {
            if (state_only && this.get_item_count() > y + i) {
                (this.getChildAt(y + i) as CellLayout).invalidate_all()
            } else if (this.get_item_count() > y + i) {
                this.rebind(i + y)
            }
        }
    }

    private fun rebind(index: Int) {
        if (index >= this.get_item_count()) {
            return
        }

        this.removeViewAt(index)
        this.addView(CellLayout(this, index), index)
    }

    fun clear() {
        this.removeAllViews()
    }

    fun get_opus_manager(): OpusManager {
        return (this._view_holder.bindingAdapter as ColumnRecyclerAdapter).get_opus_manager()
    }

    fun get_beat(): Int {
        return this._view_holder.bindingAdapterPosition
    }

    private fun _get_column_recycler_adapter(): ColumnRecyclerAdapter {
        if (this._view_holder.bindingAdapter == null) {
            throw ColumnDetachedException()
        }
        return this._view_holder.bindingAdapter as ColumnRecyclerAdapter
    }
    private fun _get_editor_table(): EditorTable {
        return this._get_column_recycler_adapter().get_editor_table()!!
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }
}
