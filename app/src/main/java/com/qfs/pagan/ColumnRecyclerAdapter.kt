package com.qfs.pagan

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ColumnRecyclerAdapter(private val _recycler: ColumnRecycler, editor_table: EditorTable): RecyclerView.Adapter<ColumnRecyclerViewHolder>() {
    private val _column_label_recycler: ColumnLabelRecycler
    private var _column_count = 0
    init {
        this._column_label_recycler = editor_table.column_label_recycler

        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(start: Int, count: Int) {
                    this@ColumnRecyclerAdapter.notifyItemChanged(start + count - 1)
                }
                override fun onItemRangeChanged(start: Int, count: Int) { }
            }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnRecyclerViewHolder {
        return ColumnRecyclerViewHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return this._column_count
    }

    override fun onBindViewHolder(holder: ColumnRecyclerViewHolder, position: Int) {
        ColumnLayout(holder)
    }

    override fun onViewAttachedToWindow(holder: ColumnRecyclerViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.itemView.layoutParams.height = MATCH_PARENT
    }

    fun insert_row(index: Int) {
        this.insert_rows(index, 1)
    }

    fun insert_rows(index: Int, count: Int) {
        this.apply_and_notify_remaining { _: Int, column_layout: ColumnLayout ->
            column_layout.insert_cells(index, count)
        }
    }

    fun remove_row(index: Int) {
        this.remove_rows(index, 1)
    }

    private fun apply_and_notify_remaining(callback: (Int, ColumnLayout) -> Unit) {
        var minimum_visible = this.itemCount
        var maximum_visible = 0

        for (beat in 0 until this.itemCount) {
            val viewHolder = this._recycler.findViewHolderForAdapterPosition(beat) ?: continue
            if ((viewHolder.itemView as ViewGroup).childCount == 0) {
                continue
            }
            minimum_visible = min(beat, minimum_visible)
            maximum_visible = max(beat, maximum_visible)
        }

        // Need to notify The recycler FIRST, since the updates may change the
        // widths of the visible columns and therefore, the number of columns that need to be notified
        this.notifyItemRangeChanged(0, minimum_visible)
        this.notifyItemRangeChanged(maximum_visible + 1, this.itemCount)

        this._apply_to_visible_columns { beat: Int, column_layout: ColumnLayout ->
            callback(beat, column_layout)
        }
    }

    fun remove_rows(index: Int, count: Int) {
        this.apply_and_notify_remaining { _: Int, column_layout: ColumnLayout ->
            column_layout.remove_cells(index, count)
        }
    }

    //-------------------------------------------------------//
    private fun _apply_to_visible_columns(callback: (Int, ColumnLayout) -> Unit) {
        for (i in 0 until this.itemCount) {
            val viewHolder = this._recycler.findViewHolderForAdapterPosition(i) ?: continue
            if ((viewHolder.itemView as ViewGroup).childCount == 0) {
                continue
            }

            val item = (viewHolder.itemView as ViewGroup).getChildAt(0)
            if (item is ColumnLayout) {
                callback(i, item)
            }
        }
    }

    fun add_column(index: Int) {
        this.add_columns(index, 1)
    }
    fun add_columns(index: Int, count: Int) {
        this._column_count += count
        this.notifyItemRangeInserted(index, count)
        this.notifyItemRangeChanged(index + count, count)

    }

    fun remove_column(index: Int) {
        /* KLUDGE ALERT:
            for some reason, the ColumnRecyclerAdapter and the ColumnLabel Adapter don't automatically scroll to the same place when removing an item.
            everything aroun column_count -= 1 and notifyItemRemoved is just to keep the two recyclers aligned
         */
        val layout_manager = this._recycler.layoutManager as LinearLayoutManager
        val item_position = layout_manager.findFirstVisibleItemPosition()
        val item = layout_manager.findViewByPosition(item_position)
        val original_offset = item?.x

        this.notifyItemRemoved(index)
        this._column_count -= 1

        if (original_offset != null) {
            layout_manager.scrollToPositionWithOffset(item_position, original_offset.toInt())
        }
    }

    //-------------------------------------------------------//
    private fun _get_activity(): MainActivity {
        return this._recycler.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this._get_activity().get_opus_manager()
    }

    fun get_editor_table(): EditorTable? {
        return this._recycler.editor_table
    }

    private fun _get_column_layout(beat: Int): ColumnLayout? {
        val view_holder = this._recycler.findViewHolderForAdapterPosition(beat) ?: return null
        return (view_holder as ColumnRecyclerViewHolder).get_column_layout()
    }

    fun clear() {
        val count = this._column_count
        for (x in 0 until count) {
            val column_layout = this._get_column_layout(x) ?: continue
            column_layout.clear()
        }
        this._column_count = 0
        this.notifyItemRangeRemoved(0, count)
    }

    fun notify_cell_changed(y: Int, x: Int, state_only: Boolean = false) {
        val column_layout = this._get_column_layout(x)
        if (column_layout == null) {
            this.notifyItemChanged(x)
        } else {
            column_layout.notifyItemChanged(y, state_only)
        }
    }

    fun notify_row_changed(y: Int, state_only: Boolean = false) {
        if (state_only) {
            this._apply_to_visible_columns { _: Int, column_layout: ColumnLayout ->
                column_layout.notifyItemChanged(y, true)
            }
        } else {
            this.apply_and_notify_remaining { _: Int, column_layout: ColumnLayout ->
                column_layout.notifyItemChanged(y, false)
            }
        }
    }

    fun notify_column_state_changed(x: Int) {
        val column_layout = this._get_column_layout(x) ?: return
        column_layout.notifyItemRangeChanged(0, column_layout.childCount, true)
    }
}
