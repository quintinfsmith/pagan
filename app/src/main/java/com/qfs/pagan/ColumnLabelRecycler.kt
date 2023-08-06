package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView

class ColumnLabelRecycler(var editor_table: EditorTable, context: Context, attrs: AttributeSet): ScrollLockingRecyclerView(context, attrs) {
    init {
        this.itemAnimator = null
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.width = MATCH_PARENT
        this.layoutParams.height = WRAP_CONTENT
        this.setHasFixedSize(true)
    }
}