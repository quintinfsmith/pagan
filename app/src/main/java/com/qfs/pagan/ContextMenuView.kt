package com.qfs.pagan

import android.view.LayoutInflater
import android.view.ViewGroup

abstract class ContextMenuView(layout_id_primary: Int?, layout_id_secondary: Int?, primary_container: ViewGroup, secondary_container: ViewGroup) {
    val primary: ViewGroup?
    val secondary: ViewGroup?

    val context = primary_container.context

    init {
        if (layout_id_secondary != null) {
            this.secondary = LayoutInflater.from(this.context).inflate(layout_id_secondary, secondary_container, false) as ViewGroup
            secondary_container.addView(this.secondary)
        } else {
            this.secondary = null
        }

        if (layout_id_primary != null) {
            this.primary = LayoutInflater.from(this.context).inflate(layout_id_primary, primary_container, false) as ViewGroup
            primary_container.addView(this.primary)
        } else {
            this.primary = null
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
