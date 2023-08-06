package com.qfs.pagan

import androidx.recyclerview.widget.RecyclerView

class HorizontalScrollListener(val other_recycler: ScrollLockingRecyclerView): RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, x: Int, y: Int) {
        super.onScrolled(recyclerView, x, y)

        if (!(recyclerView as ScrollLockingRecyclerView).is_propagation_locked()) {
            this.other_recycler.lock_scroll_propagation()
            this.other_recycler.scrollBy(x, 0)
            this.other_recycler.unlock_scroll_propagation()
        }
    }
}
