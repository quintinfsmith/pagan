package com.qfs.pagan

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager

class ColumnRecycler(editor_table: EditorTable): ScrollLockingRecyclerView(editor_table.context) {
    init {
        this.layoutManager = LeftAlignedLayoutManager(this, HORIZONTAL, false)
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
        this.adapter = ColumnRecyclerAdapter(this, editor_table)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}