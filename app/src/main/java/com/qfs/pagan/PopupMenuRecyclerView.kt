package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class PopupMenuRecyclerView(context: Context, attrs: AttributeSet): RecyclerView(context, attrs) {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (this.adapter == null) {
            return
        }
        val default_position =
            (this.adapter!! as PopupMenuRecyclerAdapter<*>).get_default_position() ?: return

        this.scrollToPosition(default_position)
    }
}