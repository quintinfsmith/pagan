package com.qfs.pagan.ContextMenu

import android.view.ViewGroup
import android.widget.ImageView
import com.qfs.pagan.ContextMenuView
import com.qfs.pagan.R

class ContextMenuColumn(primary_parent: ViewGroup, secondary_parent: ViewGroup): ContextMenuView(R.layout.contextmenu_column, null, primary_parent, secondary_parent) {
    lateinit var button_insert: ImageView
    lateinit var button_remove: ImageView
    lateinit var button_adjust: ImageView

    init {
        this.refresh()
    }

    override fun init_properties() {
        this.button_insert = this.primary!!.findViewById(R.id.btnInsertBeat)
        this.button_remove = this.primary.findViewById(R.id.btnRemoveBeat)
        this.button_adjust = this.primary.findViewById(R.id.btnAdjust)
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        this.button_remove.isEnabled = opus_manager.length > 1
    }

    override fun setup_interactions() {
        this.button_insert.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_insert_beat()
        }

        this.button_insert.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_insert_beat()
        }

        this.button_remove.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_remove_beat()
        }

        this.button_remove.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_remove_beat()
        }

        this.button_adjust.setOnClickListener {
            this.get_activity().get_action_interface().adjust_selection()
        }
    }

    fun click_button_remove_beat() {
        this.get_activity().get_action_interface().remove_beat_at_cursor(1)
    }

    fun long_click_button_remove_beat(): Boolean {
        this.get_activity().get_action_interface().remove_beat_at_cursor()
        return true
    }

    fun click_button_insert_beat() {
        this.get_activity().get_action_interface().insert_beat_after_cursor(1)
    }

    fun long_click_button_insert_beat(): Boolean {
        this.get_activity().get_action_interface().insert_beat_after_cursor()
        return true
    }
}
