package com.qfs.pagan

import android.content.Context
import androidx.appcompat.widget.LinearLayoutCompat
import com.qfs.pagan.opusmanager.OpusControlEvent

abstract class ControlWidget(context: Context, val callback: (OpusControlEvent) -> Unit): LinearLayoutCompat(context, null) {
    abstract fun get_event(): OpusControlEvent
    abstract fun set_event(event: OpusControlEvent)
}