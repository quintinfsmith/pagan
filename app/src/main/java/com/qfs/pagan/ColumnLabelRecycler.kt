package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColumnLabelRecycler(context: Context, attrs: AttributeSet? = null): RecyclerView(context, attrs) {
    init {
        this.layoutManager = LeftAlignedLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.width = MATCH_PARENT
        this.layoutParams.height = WRAP_CONTENT
    }
}