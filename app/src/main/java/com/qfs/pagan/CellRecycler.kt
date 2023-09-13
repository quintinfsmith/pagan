package com.qfs.pagan

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlin.concurrent.thread
import com.qfs.pagan.InterfaceLayer as OpusManager

@SuppressLint("ViewConstructor")
class CellRecycler(var viewHolder: ColumnRecyclerViewHolder): ScrollLockingRecyclerView((viewHolder.itemView.context as ContextThemeWrapper).baseContext) {
    class ColumnDetachedException: Exception()
    class CellDetachedException: Exception()
    init {
        this.adapter = CellRecyclerAdapter(this.get_opus_manager().get_total_line_count())
        this.layoutManager = CellRecyclerLayoutManager(context, this)
        this.addOnScrollListener(this.get_scroll_listener())

        this.lock_scroll_propagation()
        this.itemAnimator = null

        (this.viewHolder.itemView as ViewGroup).removeAllViews()
        (this.viewHolder.itemView as ViewGroup).addView(this)

        this.layoutParams.height = MATCH_PARENT
        this.layoutParams.width = WRAP_CONTENT

        this.setHasFixedSize(true)
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.unlock_scroll_propagation()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        this.conform_scroll_position()
    }

    //-------------------------------------------------------//

    fun conform_scroll_position() {
        this.lock_scroll_propagation()
        try {
            val editor_table = this.get_editor_table()
            val line_label_recycler = editor_table.line_label_recycler
            val label_position = line_label_recycler.computeVerticalScrollOffset()
            val current_position = this.computeVerticalScrollOffset()
            val delta = label_position - current_position
            if (delta != 0) {
                this.scrollBy(0, delta)
            }
            this.visibility = View.VISIBLE
        } catch (e: ColumnDetachedException) {
            // Happens when scrolling quickly and the recycler is detached before it can adjust
        } catch (e: CellDetachedException) {
            // Happens when scrolling quickly and the recycler is detached before it can adjust
        }
        this.unlock_scroll_propagation()
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
        return this.get_column_recycler_adapter().get_editor_table()!!
    }
    fun get_scroll_listener(): VerticalScrollListener {
        return this.get_editor_table().vertical_scroll_listener
    }

}