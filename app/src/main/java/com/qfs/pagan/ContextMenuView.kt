package com.qfs.pagan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

abstract class ContextMenuView(layout_id_primary: Int, layout_id_secondary: Int?, primary_container: ViewGroup, secondary_container: ViewGroup) {
    val primary: View
    val secondary: View?

    val context = primary_container.context

    init {
        this.primary = LayoutInflater.from(this.context).inflate(layout_id_primary, primary_container, false)
        primary_container.addView(this.primary)
        if (layout_id_secondary != null) {
            this.secondary = LayoutInflater.from(this.context).inflate(layout_id_secondary, secondary_container, false)
            secondary_container.addView(this.secondary)
        } else {
            this.secondary = null
        }




        this.init_properties()
        this.setup_interactions()
        this.refresh()
    }

    abstract fun init_properties()
    abstract fun setup_interactions()
    abstract fun refresh()

    fun get_main(): MainActivity {
        return this.context as MainActivity
    }
    fun get_opus_manager(): OpusLayerInterface {
        return this.get_main().get_opus_manager()
    }
}