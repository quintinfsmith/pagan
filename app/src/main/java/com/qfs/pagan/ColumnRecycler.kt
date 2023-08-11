package com.qfs.pagan

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ColumnRecycler(context: Context, attrs: AttributeSet? = null): ScrollLockingRecyclerView(context, attrs) {
    init {
        this.layoutManager = TestLayoutManager(context, HORIZONTAL)
        this.itemAnimator = null
        this.setHasFixedSize(true)
        this.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}