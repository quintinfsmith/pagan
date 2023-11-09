package com.qfs.pagan

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.InterfaceLayer as OpusManager

class CellRecycler(context: Context): RecyclerView(context) {
    init{
        val opus_manager = (this.context as MainActivity).get_opus_manager()
        val layout_manager = GridLayoutManager(
            this.context,
            opus_manager.beat_count,
            HORIZONTAL,
            false
        )
        Log.d("AAA", "SCROLLX: ${layout_manager.canScrollHorizontally()}")
        Log.d("AAA", "SCROLLY: ${layout_manager.canScrollVertically()}")
        this.layoutManager = layout_manager

        this.adapter = CellAdapter(this)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    fun get_opus_manager(): OpusManager {
        return (this.context as MainActivity).get_opus_manager()
    }
}
