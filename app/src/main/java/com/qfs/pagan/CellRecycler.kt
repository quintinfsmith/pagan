package com.qfs.pagan

import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class CellRecycler(context: Context, var viewHolder: ColumnRecyclerViewHolder): RecyclerView(context) {
    class ColumnDetachedException: Exception()

    private var _scroll_propagation_locked = false

    init {
        this.visibility = View.INVISIBLE
        this.adapter = CellRecyclerAdapter()
        this.layoutManager = LinearLayoutManager(context, VERTICAL, false)
        this.addOnScrollListener(this.get_scroll_listener())
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        this.layoutParams.height = MATCH_PARENT
        this.layoutParams.width = MATCH_PARENT
        //(this.adapter as CellRecyclerAdapter).update_width()

        this.conform_scroll_position()
    }

    //-------------------------------------------------------//
    fun conform_scroll_position() {
        thread {
            val column_adapter = this.get_column_recycler_adapter()
            column_adapter.get_activity().runOnUiThread {
                this.lock_scroll_propagation()
                try {
                    val position = this.get_scroll_listener().absolute_y
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
    fun is_propagation_locked(): Boolean {
        return this._scroll_propagation_locked
    }
    fun lock_scroll_propagation() {
        this._scroll_propagation_locked = true
    }
    fun unlock_scroll_propagation() {
        this._scroll_propagation_locked = false
    }

    //-------------------------------------------------------//
    fun get_beat(): Int {
        return this.viewHolder!!.bindingAdapterPosition
    }
    fun get_column_recycler_adapter(): ColumnRecyclerAdapter {
        if (this.viewHolder.bindingAdapter == null) {
            throw ColumnDetachedException()
        }
        return this.viewHolder!!.bindingAdapter as ColumnRecyclerAdapter
    }
    fun get_editor_table(): EditorTable {
        return this.get_column_recycler_adapter().get_editor_table()
    }
    fun get_scroll_listener(): VerticalScrollListener {
        return this.get_editor_table().vertical_scroll_listener
    }
}