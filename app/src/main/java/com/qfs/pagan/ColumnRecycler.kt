package com.qfs.pagan

import android.view.View

class ColumnRecycler(editor_table: EditorTable): ScrollLockingRecyclerView(editor_table.context) {
    init {
        this.layoutManager = LeftAlignedLayoutManager(this, HORIZONTAL, false)
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.adapter = ColumnRecyclerAdapter(this, editor_table)
    }
}