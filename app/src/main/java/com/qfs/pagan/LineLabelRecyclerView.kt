package com.qfs.pagan

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager

class LineLabelRecyclerView(context: Context, attrs: AttributeSet? = null) : ScrollLockingRecyclerView(context, attrs) {
    init {
        this.layoutManager = LinearLayoutManager(this.context, VERTICAL, false)
        this.itemAnimator = null
        this.overScrollMode = View.OVER_SCROLL_NEVER
        //this.setHasFixedSize(true)
    }
    // Prevents this from intercepting linelabel touch events (disables manual scrolling)
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return false
    }
    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
    }
}


