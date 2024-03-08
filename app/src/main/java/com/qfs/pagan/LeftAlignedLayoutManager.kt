package com.qfs.pagan

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LeftAlignedLayoutManager(private val _recycler: RecyclerView, orientation: Int, reversed: Boolean): LinearLayoutManager(_recycler.context, orientation, reversed) {
    override fun scrollToPosition(position: Int) {
        super.scrollToPositionWithOffset(position, 0)
    }

    override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
        if (this.orientation == HORIZONTAL) {
            var p = this.findLastVisibleItemPosition()
            var w = this._recycler.findViewHolderForAdapterPosition(p)?.itemView?.measuredWidth ?: 0
            extraLayoutSpace[1] = w
            p = this.findFirstVisibleItemPosition()
            w = this._recycler.findViewHolderForAdapterPosition(p)?.itemView?.measuredWidth ?: 0
            extraLayoutSpace[0] = w
        } else {
            super.calculateExtraLayoutSpace(state, extraLayoutSpace)
        }
    }
}