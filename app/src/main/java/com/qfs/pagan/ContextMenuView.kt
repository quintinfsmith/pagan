package com.qfs.pagan

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat

open class ContextMenuView(layout_id: Int, context: Context, attrs: AttributeSet? = null): LinearLayoutCompat(context, attrs) {
    init {
        val view = LayoutInflater.from(this.context).inflate(layout_id, this, false)
        this.addView(view)
        this.init_properties()
        this.setup_interactions()
        this.refresh()
    }

    open fun init_properties() { }
    open fun setup_interactions() { }
    open fun refresh() { }

    fun get_main(): MainActivity {
        return this.context as MainActivity
    }
    fun get_opus_manager(): OpusLayerInterface {
        return this.get_main().get_opus_manager()
    }
}