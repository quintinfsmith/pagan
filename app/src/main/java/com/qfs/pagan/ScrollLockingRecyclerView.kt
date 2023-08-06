package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

open class ScrollLockingRecyclerView(context: Context, attrs: AttributeSet? = null): RecyclerView(context, attrs) {
    private var _scroll_propagation_locked = false

    fun is_propagation_locked(): Boolean {
        return this._scroll_propagation_locked
    }
    fun lock_scroll_propagation() {
        this._scroll_propagation_locked = true
    }
    fun unlock_scroll_propagation() {
        this._scroll_propagation_locked = false
    }

    //-------------------------------------------------------//

}