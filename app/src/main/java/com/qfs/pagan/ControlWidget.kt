package com.qfs.pagan

import android.content.Context
import androidx.appcompat.widget.LinearLayoutCompat
import com.qfs.pagan.opusmanager.OpusControlEvent

abstract class ControlWidget<T: OpusControlEvent>(context: Context, var working_event: T, var is_initial_event: Boolean, val callback: (T) -> Unit): LinearLayoutCompat(context, null) {
    abstract fun on_set(event: T)

    fun get_event(): T {
        return this.working_event
    }
    fun set_event(event: T) {
        this.working_event = event
        this.on_set(event)
    }

}