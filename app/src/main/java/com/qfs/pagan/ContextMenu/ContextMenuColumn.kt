package com.qfs.pagan.ContextMenu

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.qfs.pagan.R

class ContextMenuColumn(primary_parent: ViewGroup, secondary_parent: ViewGroup): ContextMenuView(R.layout.contextmenu_column, null, primary_parent, secondary_parent) {
    lateinit var button_insert: ImageView
    lateinit var button_remove: ImageView
    lateinit var button_adjust: ImageView
    lateinit var button_tag: ImageView
    lateinit var button_untag: ImageView

    init {
        this.refresh()
    }

    override fun init_properties() {
        this.button_insert = this.primary!!.findViewById(R.id.btnInsertBeat)
        this.button_remove = this.primary.findViewById(R.id.btnRemoveBeat)
        this.button_adjust = this.primary.findViewById(R.id.btnAdjust)
        this.button_tag = this.primary.findViewById(R.id.btnTag)
        this.button_untag = this.primary.findViewById(R.id.btnUnTag)
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        this.button_remove.isEnabled = opus_manager.length > 1
        if (opus_manager.is_beat_tagged(opus_manager.cursor.beat)) {
            this.button_untag.visibility = View.VISIBLE
        } else {
            this.button_untag.visibility = View.GONE
        }
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

        this.button_tag.setOnClickListener {
            this.get_activity().get_action_interface().tag_column()
        }
        this.button_untag.setOnClickListener {
            this.get_activity().get_action_interface().untag_column()
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
