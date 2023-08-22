package com.qfs.pagan

import android.annotation.SuppressLint
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
    var block_attach_callback = false
    init {
        this.adapter = CellRecyclerAdapter(this.get_opus_manager().get_total_line_count(), this.get_line_label_offset())
        this.layoutManager = CellRecyclerLayoutManager(context, this)
        this.addOnScrollListener(this.get_scroll_listener())

        this.lock_scroll_propagation()
        this.itemAnimator = null

        (this.viewHolder.itemView as ViewGroup).removeAllViews()
        (this.viewHolder.itemView as ViewGroup).addView(this)

        val adapter = (this.adapter as CellRecyclerAdapter)
        val editor_table = this.get_editor_table()
        val ll_view_holder = editor_table.line_label_recycler.findViewHolderForAdapterPosition( adapter.initial_offset )
        val layout_offset = ll_view_holder?.itemView?.y?.toInt() ?: 0
        (this.layoutParams as MarginLayoutParams).setMargins(0, layout_offset, 0, 0)

        this.layoutParams.height = MATCH_PARENT
        this.layoutParams.width = WRAP_CONTENT

        this.setHasFixedSize(true)
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.unlock_scroll_propagation()
    }

    override fun onAttachedToWindow() {
        this.visibility = View.INVISIBLE
        super.onAttachedToWindow()
        if (this.block_attach_callback) {
            return
        }

        val that = this
        val adapter = (this.adapter as CellRecyclerAdapter)
        thread {
            (that.context as MainActivity).runOnUiThread {
                that.block_attach_callback = true
                that.lock_scroll_propagation()
                adapter.reset_initial_offset()

                that.scrollBy(0, 0 - (that.layoutParams as MarginLayoutParams).topMargin)
                (this.layoutParams as MarginLayoutParams).setMargins(0, 0, 0, 0)

                (that.viewHolder.itemView as ViewGroup).removeView(that)
                (that.viewHolder.itemView as ViewGroup).addView(that)
                that.block_attach_callback = false
                this.visibility = View.VISIBLE
                adapter.reset_initial_offset()
                that.unlock_scroll_propagation()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.conform_scroll_position()
    }


    //-------------------------------------------------------//
    private fun get_line_label_offset(): Int {
        val editor_table = this.get_editor_table()

        val llmanager = editor_table.line_label_recycler.layoutManager as LinearLayoutManager
        val p = llmanager.findFirstVisibleItemPosition() ?: return 0
        return p
    }

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
        } catch (e: ColumnDetachedException) {
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