package com.qfs.pagan

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnRecyclerAdapter(var activity: MainActivity): RecyclerView.Adapter<ColumnViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnViewHolder {
        val beat_cell_recycler = CellRecycler(parent.context)
        return ColumnViewHolder(beat_cell_recycler)
    }

    override fun getItemCount(): Int {
        return this.get_opus_manager().opus_beat_count
    }

    override fun onBindViewHolder(holder: ColumnViewHolder, position: Int) {
        (holder.itemView as CellRecycler).build()
    }

    //-------------------------------------------------------//

    fun get_opus_manager(): OpusManager {
        return this.activity.get_opus_manager()
    }
}