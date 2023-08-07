package com.qfs.pagan

import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnRecyclerAdapter(editor_table: EditorTable): RecyclerView.Adapter<ColumnRecyclerViewHolder>() {
    val recycler: ColumnRecycler
    val column_label_recycler: ColumnLabelRecycler
    var column_count = 0
    init {
        this.column_label_recycler = editor_table.column_label_recycler
        this.recycler = editor_table.main_recycler
        this.recycler.adapter = this

        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.notifyItemChanged(start + count - 1)
                }
            }
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnRecyclerViewHolder {
        return ColumnRecyclerViewHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return this.column_count
    }

    override fun onBindViewHolder(holder: ColumnRecyclerViewHolder, position: Int) {
        CellRecycler(holder)
    }
    override fun onViewAttachedToWindow(holder: ColumnRecyclerViewHolder) {
        holder.itemView.layoutParams.height = MATCH_PARENT
    }
    override fun onViewDetachedFromWindow(holder: ColumnRecyclerViewHolder) {

    }

    //-------------------------------------------------------//
    fun apply_to_visible_columns(callback: (CellRecyclerAdapter) -> Unit) {
        for (i in 0 until this.itemCount) {
            val viewHolder = this.recycler.findViewHolderForAdapterPosition(i) ?: continue
            if ((viewHolder.itemView as ViewGroup).childCount == 0) {
                continue
            }

            val cell_recycler = (viewHolder.itemView as ViewGroup).children.first() as CellRecycler
            val adapter = cell_recycler.adapter!! as CellRecyclerAdapter
            callback(adapter)
        }
    }
    fun add_column(index: Int) {
        this.column_count += 1
        this.notifyItemInserted(index)
    }

    fun remove_column(index: Int) {
        this.column_count -= 1
        this.notifyItemRemoved(index)
    }

    //-------------------------------------------------------//
    fun get_activity(): MainActivity {
        return this.recycler.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun get_editor_table(): EditorTable {
        return this.recycler.editor_table
    }

    fun get_cell_recycler(beat: Int): CellRecycler? {
        val view_holder = this.recycler.findViewHolderForAdapterPosition(beat) ?: return null
        return (view_holder as ColumnRecyclerViewHolder).get_cell_recycler()
    }

    fun clear() {
        var count = this.column_count
        for (x in 0 until count) {
            var cell_recycler = this.get_cell_recycler(x) ?: continue
            (cell_recycler.adapter!! as CellRecyclerAdapter).clear()
        }
        this.column_count = 0
        this.notifyItemRangeRemoved(0, count)
    }

    //fun get_views_in_purgatory(): List<CellRecyclerViewHolder> {
    //    val output = mutableListOf<CellRecyclerViewHolder>()
    //}

    //-------------------------------------------------------//
    fun get_leaf_view(beat_key: BeatKey, position: List<Int>): LeafButton? { return null }
    fun scroll_to_position(column: Int) { }
    fun scroll_to_position(beat_key: BeatKey, position: List<Int>) {}
}