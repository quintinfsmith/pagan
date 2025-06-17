package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qfs.pagan.Activity.ActivityEditor

class ChannelOptionRecycler(context: Context, attrs: AttributeSet): RecyclerView(context, attrs) {
    init {
        this.layoutManager = LinearLayoutManager(this.context as ActivityEditor)
        this.itemAnimator = null
    }

}