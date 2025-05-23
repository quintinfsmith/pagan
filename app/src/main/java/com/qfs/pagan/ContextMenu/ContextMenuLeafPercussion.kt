package com.qfs.pagan.ContextMenu

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.R

class ContextMenuLeafPercussion(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(
    R.layout.contextmenu_cell_percussion, null, primary_container, secondary_container) {
    lateinit var button_split: Button
    lateinit var button_insert: Button
    lateinit var button_unset: Button
    lateinit var button_remove: Button
    lateinit var button_duration: Button

    override fun init_properties() {
        val primary = this.primary!!
        this.button_split = primary.findViewById(R.id.btnSplit)
        this.button_insert = primary.findViewById(R.id.btnInsert)
        this.button_unset = primary.findViewById(R.id.btnUnset)
        this.button_remove = primary.findViewById(R.id.btnRemove)
        this.button_duration = primary.findViewById(R.id.btnDuration)
    }

    override fun refresh() {
        this.button_split.visibility = View.VISIBLE
        this.button_insert.visibility = View.VISIBLE

        val opus_manager = this.get_opus_manager()

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()

        val current_tree_position = opus_manager.get_actual_position(beat_key, position)
        val current_event_tree = opus_manager.get_tree(current_tree_position.first, current_tree_position.second)

        if (current_event_tree.is_event()) {
            val event = current_event_tree.get_event()!!
            (this.button_unset as MaterialButton).setIconResource(R.drawable.unset)
            this.button_duration.text = this.context.getString(R.string.label_duration, event.duration)
        } else {
            (this.button_unset as MaterialButton).setIconResource(R.drawable.set_percussion)
            this.button_duration.text = ""
        }

        this.button_split.isEnabled = true
        this.button_split.isClickable = this.button_split.isEnabled

        this.button_duration.isEnabled = current_event_tree.is_event()
        this.button_duration.isClickable = this.button_duration.isEnabled

        this.button_remove.isEnabled = position.isNotEmpty()
        this.button_remove.isClickable = this.button_remove.isEnabled
    }

    override fun setup_interactions() {
        this.button_duration.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_duration()
        }

        this.button_duration.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_duration()
        }

        this.button_remove.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_remove()
        }

        this.button_remove.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_remove()
        }

        this.button_unset.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_unset()
        }

        this.button_split.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_split()
        }

        this.button_split.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_split()
        }

        this.button_insert.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_insert()
        }

        this.button_insert.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_insert()
        }
    }

    fun click_button_duration() {
        this.get_activity().get_action_interface().set_duration()
    }

    fun click_button_split() {
        this.get_activity().get_action_interface().split(2)
    }

    fun click_button_insert() {
        this.get_activity().get_action_interface().insert_leaf(1)
    }

    fun click_button_remove() {
        this.get_activity().get_action_interface().remove_at_cursor()
    }

    fun long_click_button_split(): Boolean {
        this.get_activity().get_action_interface().split()
        return true
    }

    fun long_click_button_insert(): Boolean {
        this.get_activity().get_action_interface().insert_leaf()
        return true
    }

    fun long_click_button_duration(): Boolean {
        this.get_activity().get_action_interface().set_duration(1)
        return true
    }

    fun long_click_button_remove(): Boolean {
        this.get_activity().get_action_interface().unset_root()
        return true
    }

    private fun click_button_unset() {
        this.get_activity().get_action_interface().toggle_percussion()
    }
}
