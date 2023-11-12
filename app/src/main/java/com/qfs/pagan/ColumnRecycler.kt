package com.qfs.pagan

import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ColumnRecycler(editor_table: EditorTable): RecyclerView(editor_table.context) {
    init {
        this.layoutManager = LeftAlignedLayoutManager(this, HORIZONTAL, false)
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.adapter = ColumnRecyclerAdapter(this, editor_table)
    }
}