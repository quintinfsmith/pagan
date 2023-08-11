package com.qfs.pagan

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.RecyclerView

class ColumnLabelRecycler(context: Context, attrs: AttributeSet? = null): ScrollLockingRecyclerView(context, attrs) {
    init {
        this.itemAnimator = null
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.width = MATCH_PARENT
        this.layoutParams.height = WRAP_CONTENT
        this.overScrollMode = View.OVER_SCROLL_NEVER
    }
    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}