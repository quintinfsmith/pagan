package com.qfs.pagan

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusChannel
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellRecyclerAdapter(): RecyclerView.Adapter<CellRecyclerViewHolder>() {
    val width_map = mutableListOf<Int>()
    lateinit var recycler: CellRecycler
    var initializing_width_map = false

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recycler = recyclerView as CellRecycler
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellRecyclerViewHolder {
        return CellRecyclerViewHolder(this.recycler.context)
    }

    override fun getItemCount(): Int {
        return this.get_opus_manager().get_total_line_count()
    }

    override fun onViewAttachedToWindow(holder:CellRecyclerViewHolder) {
        var cell_layout = (holder.itemView as CellLayout)
        cell_layout.build()
    }
    override fun onBindViewHolder(holder: CellRecyclerViewHolder, position: Int) {
        var cell_layout = (holder.itemView as CellLayout)
        cell_layout.viewHolder = holder
    }

    //-------------------------------------------------------//

    fun update_width() {
        this.recycler.layoutParams.width = this.get_target_width()
        this.get_column_adapter().column_label_recycler.adapter?.notifyItemChanged(this.get_beat())
    }

    fun get_target_width(): Int {
        if (this.width_map.size != this.itemCount) {
            this.init_width_map()
        }
        val resources = this.recycler.resources
        return (this.width_map.max() * resources.getDimension(R.dimen.base_leaf_width)).toInt()
    }

    fun init_width_map() {
        this.initializing_width_map = true
        val opus_manager = this.get_opus_manager()
        val beat = this.get_beat()
        opus_manager.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                val tree = opus_manager.get_beat_tree(BeatKey(i, j, beat))
                if (tree.is_leaf()) {
                    this.width_map.add(1)
                } else {
                    this.width_map.add(tree.get_max_child_weight() * tree.size)
                }
            }
        }
        this.initializing_width_map = false
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
}
