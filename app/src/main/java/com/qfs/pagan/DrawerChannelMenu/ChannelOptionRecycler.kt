package com.qfs.pagan.DrawerChannelMenu

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChannelOptionRecycler(context: Context, attrs: AttributeSet): RecyclerView(context, attrs) {
    init {
        this.layoutManager = LinearLayoutManager(this.context)
        this.itemAnimator = null
    }

    fun notify_soundfont_changed() {
        (this.adapter as ChannelOptionAdapter?)?.notify_soundfont_changed()
    }
}