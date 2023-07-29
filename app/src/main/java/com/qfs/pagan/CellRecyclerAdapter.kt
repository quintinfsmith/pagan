package com.qfs.pagan

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellRecyclerAdapter(var recycler: CellRecycler): RecyclerView.Adapter<CellViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        return CellViewHolder(CellLayout(this.recycler.context))
    }

    override fun getItemCount(): Int {
        return this.get_opus_manager().get_total_line_count()
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        (holder.itemView as CellLayout).build()
    }
    //-------------------------------------------------------//

    //-------------------------------------------------------//
    fun get_column_adapter(): ColumnRecyclerAdapter {
        return this.recycler.get_column_adapter()
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
