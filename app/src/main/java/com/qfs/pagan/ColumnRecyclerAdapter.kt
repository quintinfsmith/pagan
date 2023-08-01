package com.qfs.pagan

import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnRecyclerAdapter(editor_table: EditorTable): RecyclerView.Adapter<ColumnRecyclerViewHolder>() {
    val recycler: ColumnRecycler
    val column_label_recycler: ColumnLabelRecycler
    init {
        this.column_label_recycler = editor_table.column_label_recycler
        this.recycler = editor_table.main_recycler
        this.recycler.adapter = this

        //val that = this
        //this.registerAdapterDataObserver(
        //    object: RecyclerView.AdapterDataObserver() {
        //        override fun onItemRangeRemoved(start: Int, count: Int) {
        //            that.column_label_recycler.adapter?.notifyItemRangeRemoved(start, count)
        //        }
        //        override fun onItemRangeInserted(start: Int, count: Int) {
        //            that.column_label_recycler.adapter?.notifyItemRangeInserted(start, count)
        //        }
        //        override fun onItemRangeChanged(start: Int, count: Int) {
        //            that.column_label_recycler.adapter?.notifyItemRangeChanged(start, count)
        //        }
        //    }
        //)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnRecyclerViewHolder {
        return ColumnRecyclerViewHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return this.get_opus_manager().opus_beat_count
    }

    override fun onBindViewHolder(holder: ColumnRecyclerViewHolder, position: Int) {
        // Looks like this isn't needed and causes problems
        (holder.itemView as ViewGroup).removeAllViews()
        val cell_recycler = CellRecycler(holder.itemView.context, holder)
        (holder.itemView as ViewGroup).addView(cell_recycler)
    }

    override fun onViewAttachedToWindow(holder:ColumnRecyclerViewHolder) {
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
        var view_holder = this.recycler.findViewHolderForAdapterPosition(beat) ?: return null
        return (view_holder as ColumnRecyclerViewHolder).get_cell_recycler()
    }

    //-------------------------------------------------------//
    fun get_leaf_view(beat_key: BeatKey, position: List<Int>): LeafButton? { return null }
    fun scroll_to_position(column: Int) { }
    fun scroll_to_position(beat_key: BeatKey, position: List<Int>) {}
}