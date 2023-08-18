package com.qfs.pagan

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager

class ColumnRecycler(context: Context): ScrollLockingRecyclerView(context) {
    init {
        this.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}