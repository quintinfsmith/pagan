package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.qfs.pagan.opusmanager.LinksLayer

class LineLabelRecyclerView(context: Context, attrs: AttributeSet) : ScrollLockingRecyclerView(context, attrs) {
    init {
        this.itemAnimator = null
        this.setHasFixedSize(true)
    }
    // Prevents this from intercepting linelabel touch events (disables manual scrolling)
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return false
    }
}


