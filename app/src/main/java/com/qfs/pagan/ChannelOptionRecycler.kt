package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChannelOptionRecycler(context: Context, attrs: AttributeSet): RecyclerView(context, attrs) {
    init {
        this.layoutManager = LinearLayoutManager(this.context as MainActivity)
        ChannelOptionAdapter((this.context as MainActivity).get_opus_manager(), this)
        this.itemAnimator = null
    }
}