package com.qfs.pagan

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellRecyclerAdapter(): RecyclerView.Adapter<CellRecyclerViewHolder>() {
    var cell_count = 0
    lateinit var recycler: CellRecycler

    init {
        val that = this
        this.registerAdapterDataObserver(
            object: RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(start: Int, count: Int) {
                    that.notifyItemChanged(start + count)
                }
                //override fun onItemRangeChanged(start: Int, count: Int) { }
            }
        )
    }
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recycler = recyclerView as CellRecycler
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellRecyclerViewHolder {
        return CellRecyclerViewHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return this.cell_count
    }

    override fun onViewAttachedToWindow(holder:CellRecyclerViewHolder) {
        holder.itemView.layoutParams.width = MATCH_PARENT
    }

    override fun onBindViewHolder(holder: CellRecyclerViewHolder, position: Int) {
        CellLayout(holder)
    }
    //-------------------------------------------------------//


    fun insert_cell(index: Int) {
        this.cell_count += 1
        this.notifyItemInserted(index)
    }

    fun remove_cell(index: Int) {
        this.cell_count -= 1
        this.notifyItemRemoved(index)
    }

    //-------------------------------------------------------//
    fun get_column_adapter(): ColumnRecyclerAdapter {
        return this.recycler.get_column_recycler_adapter()
    }

    fun get_beat(): Int {
        return this.recycler.get_beat()
    }

    fun get_opus_manager(): OpusManager {
        return this.get_column_adapter().get_opus_manager()
    }

    fun get_beat_tree(channel: Int, line_offset: Int): OpusTree<OpusEvent> {
        val opus_manager = this.get_opus_manager()
        val beat_key = BeatKey(channel, line_offset, this.get_beat())
        return opus_manager.get_beat_tree(beat_key)
    }

    fun get_column_width(): Int {
        var editor_table = this.recycler.get_editor_table()
        var weight = editor_table.get_column_width(this.get_beat())
        val resources = this.recycler.resources
        return (weight * resources.getDimension(R.dimen.base_leaf_width)).toInt()
    }
}
