package com.qfs.pagan

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LeftAlignedLayoutManager(val recycler: RecyclerView, orientation: Int, reversed: Boolean): LinearLayoutManager(recycler.context, orientation, reversed) {
    override fun scrollToPosition(position: Int) {
        super.scrollToPositionWithOffset(position, 0)
    }
}