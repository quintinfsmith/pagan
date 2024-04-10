package com.qfs.pagan

import android.content.Context
import androidx.appcompat.widget.LinearLayoutCompat

abstract class ControlWidget(context: Context, val callback: (Float) -> Unit): LinearLayoutCompat(context, null) {
    abstract fun get_value(): Float
    abstract fun set_value(new_value: Float)
}