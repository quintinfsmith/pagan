package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColumnRecycler(var editor_table: EditorTable, context: Context, attrs: AttributeSet): ScrollLockingRecyclerView(context, attrs) {
    init {
        this.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        this.itemAnimator = null
        //this.setHasFixedSize(true)
    }

}