package com.qfs.pagan

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellRecyclerViewHolder(context: Context): RecyclerView.ViewHolder(LinearLayout(context)) {
    var bound_and_attached = false
    init {
        this.setIsRecyclable(false)
    }

    fun get_adapter(): CellRecyclerAdapter {
        return this.bindingAdapter as CellRecyclerAdapter
    }

    fun get_cell_layout(): CellLayout {
        return (this.itemView as ViewGroup).children.first() as CellLayout
    }

    fun get_activity(): MainActivity {
        return this.get_adapter().get_column_adapter().get_activity()
    }

    fun get_opus_manager(): OpusManager {
        return (this.bindingAdapter as CellRecyclerAdapter).get_opus_manager()
    }

    fun get_beat(): Int {
        return this.get_adapter().get_beat()
    }

    fun get_y(): Int {
        return this.bindingAdapterPosition
    }

    fun get_std_offset(): Pair<Int, Int> {
        val opus_manager = this.get_opus_manager()
        val y = this.get_y()
        return opus_manager.get_std_offset(y)
    }

    fun get_beat_key(): BeatKey {
        val (channel, line_offset) = this.get_std_offset()
        return BeatKey(channel, line_offset, this.get_beat())
    }

    fun get_beat_tree(): OpusTree<OpusEvent> {
        val opus_manager = this.get_opus_manager()
        val beat_key = this.get_beat_key()
        return opus_manager.get_beat_tree(beat_key)
    }

    fun is_percussion(): Boolean {
        val opus_manager = this.get_opus_manager()
        val (channel, _) = this.get_std_offset()
        return opus_manager.is_percussion(channel)
    }
}
