package com.qfs.pagan

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.InterfaceLayer as OpusManager

class ColumnRecyclerAdapter(var activity: MainActivity): RecyclerView.Adapter<ColumnViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColumnViewHolder {
        return ColumnViewHolder(parent.context)
    }

    override fun getItemCount(): Int {
        return this.get_opus_manager().opus_beat_count
    }

    override fun onBindViewHolder(holder: ColumnViewHolder, position: Int) {
        // Looks like this isn't needed and causes problems
        (holder.itemView as ViewGroup).removeAllViews()
        (holder.itemView as ViewGroup).addView(CellRecycler(holder.itemView.context, holder))



    }

    override fun onViewAttachedToWindow(holder:ColumnViewHolder) {
    }

    //-------------------------------------------------------//

    fun get_opus_manager(): OpusManager {
        return this.activity.get_opus_manager()
    }
}