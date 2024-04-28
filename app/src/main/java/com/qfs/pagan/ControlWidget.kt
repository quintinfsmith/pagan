package com.qfs.pagan

import android.content.Context
import androidx.appcompat.widget.LinearLayoutCompat
import com.qfs.pagan.opusmanager.OpusControlEvent

abstract class ControlWidget<T: OpusControlEvent>(context: Context, val callback: (T) -> Unit): LinearLayoutCompat(context, null) {
    abstract fun get_event(): T
    abstract fun set_event(event: T)
}