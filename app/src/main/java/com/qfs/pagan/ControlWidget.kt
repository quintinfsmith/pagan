package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import com.qfs.pagan.opusmanager.OpusControlEvent

abstract class ControlWidget<T: OpusControlEvent>(context: Context, var working_event: T, var is_initial_event: Boolean, val layout_id: Int, val callback: (T) -> Unit): LinearLayoutCompat(context, null) {
    abstract fun on_set(event: T)
    abstract fun on_inflated()
    internal lateinit var inner: View

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.inner = LayoutInflater.from(context)
            .inflate(
                this.layout_id,
                this.parent as ViewGroup?,
                false
            )
        this.addView(this.inner)

        this.on_inflated()
    }

    fun get_event(): T {
        return this.working_event
    }
    fun set_event(event: T, surpress_callback: Boolean = false) {
        this.working_event = event
        this.on_set(event)
        if (!surpress_callback) {
            this.callback(event)
        }
    }

    fun get_activity(): MainActivity {
        var working_context = this.context
        while (working_context !is MainActivity) {
            working_context = (working_context as ContextThemeWrapper).baseContext
        }
        return working_context
    }
}