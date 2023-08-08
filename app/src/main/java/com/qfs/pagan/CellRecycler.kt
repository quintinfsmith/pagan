package com.qfs.pagan

import android.annotation.SuppressLint
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread
import com.qfs.pagan.InterfaceLayer as OpusManager

@SuppressLint("ViewConstructor")
class CellRecycler(var viewHolder: ColumnRecyclerViewHolder): ScrollLockingRecyclerView(ContextThemeWrapper(viewHolder.itemView.context, R.style.column)) {
    class ColumnDetachedException: Exception()

    init {
        this.visibility = View.INVISIBLE
        this.adapter = CellRecyclerAdapter()
        this.layoutManager = LinearLayoutManager(context, VERTICAL, false)
        this.addOnScrollListener(this.get_scroll_listener())
        this.itemAnimator = null

        (this.viewHolder.itemView as ViewGroup).removeAllViews()
        (this.viewHolder.itemView as ViewGroup).addView(this)
        for (y in 0 until this.get_opus_manager().get_total_line_count()) {
            (this.adapter as CellRecyclerAdapter).insert_cell(y)
        }
        this.layoutParams.height = MATCH_PARENT
        this.layoutParams.width = WRAP_CONTENT
        this.setHasFixedSize(true)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.conform_scroll_position()
    }
    //-------------------------------------------------------//
    fun conform_scroll_position() {
        thread {
            val column_adapter = this.get_column_recycler_adapter()
            column_adapter.get_activity().runOnUiThread {
                this.lock_scroll_propagation()
                try {
                    val editor_table = this.get_editor_table()
                    val line_label_recycler = editor_table.line_label_recycler
                    val position = line_label_recycler.computeVerticalScrollOffset()
                    val delta = position - this.computeVerticalScrollOffset()
                    this.scrollBy(0, delta)
                } catch (e: ColumnDetachedException) {
                    // Happens when scrolling quickly and the recycler is detached before it can adjust
                }
                this.unlock_scroll_propagation()
                this.visibility = View.VISIBLE
            }
        }
    }

    //-------------------------------------------------------//
    fun get_opus_manager(): OpusManager {
        return (this.viewHolder.bindingAdapter as ColumnRecyclerAdapter).get_opus_manager()
    }
    fun get_beat(): Int {
        return this.viewHolder.bindingAdapterPosition
    }
    fun get_column_recycler_adapter(): ColumnRecyclerAdapter {
        if (this.viewHolder.bindingAdapter == null) {
            throw ColumnDetachedException()
        }
        return this.viewHolder.bindingAdapter as ColumnRecyclerAdapter
    }
    fun get_editor_table(): EditorTable {
        return this.get_column_recycler_adapter().get_editor_table()
    }
    fun get_scroll_listener(): VerticalScrollListener {
        return this.get_editor_table().vertical_scroll_listener
    }
}