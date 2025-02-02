package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isEmpty
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent

class ContextMenuControlLeaf<T: OpusControlEvent>(val widget: ControlWidget<T>, primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_line_ctl_leaf, R.layout.contextmenu_line_ctl_leaf_secondary, primary_container, secondary_container) {
    lateinit var widget_wrapper: LinearLayout
    // --------------------------------
    lateinit var button_split: ImageView
    lateinit var button_insert: ImageView
    lateinit var button_remove: ImageView
    lateinit var button_duration: TextView
    lateinit var button_unset: ImageView

    init {
        this.init_widget()
        this.refresh()
    }


    override fun init_properties() {
        this.widget_wrapper = this.secondary!! as LinearLayout
        val primary = this.primary!!
        this.button_split = primary.findViewById(R.id.btnSplit)
        this.button_insert = primary.findViewById(R.id.btnInsert)
        this.button_remove = primary.findViewById(R.id.btnRemove)
        this.button_duration = primary.findViewById(R.id.btnDuration)
        this.button_unset = primary.findViewById(R.id.btnUnset)
    }

    fun _widget_callback(event: T) {
        val opus_manager = this.get_opus_manager()
        opus_manager.set_event_at_cursor(event)
    }

    fun init_widget() {
        this.widget_wrapper.removeAllViews()
        this.widget_wrapper.addView(this.widget as View)
        (this.widget as View).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.widget as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun setup_interactions() {
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
    }

    fun click_button_unset() {
        this.get_main().get_action_interface().unset()
    }
    fun click_button_remove() {
        val opus_manager = this.get_opus_manager()
        opus_manager.remove_at_cursor(1)
    }

    fun long_click_button_remove(): Boolean {
        this.get_main().get_action_interface().unset_root()
        return false
    }

    fun click_button_insert() {
        this.get_main().get_action_interface().insert_leaf(1)
    }

    fun long_click_button_insert(): Boolean {
        this.get_main().get_action_interface().insert_leaf()
        return false
    }

    fun click_button_split() {
        this.get_main().get_action_interface().split(2)
    }

    fun long_click_button_split(): Boolean {
        this.get_main().get_action_interface().split()
        return false
    }

    override fun refresh() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val current_event = this.get_control_event<T>()

        if (this.widget_wrapper.isEmpty()) {
            this.init_widget()
        } else {
            this.widget.set_event(current_event, true)
        }

        val ctl_tree = when (cursor.ctl_level!!) {
            CtlLineLevel.Global -> {
                val (actual_beat, actual_position) = opus_manager.controller_global_get_actual_position<OpusControlEvent>(cursor.ctl_type!!, cursor.beat, cursor.get_position())
                opus_manager.get_global_ctl_tree<T>(
                    cursor.ctl_type!!,
                    actual_beat,
                    actual_position
                )
            }
            CtlLineLevel.Channel -> {
                val (actual_beat, actual_position) = opus_manager.controller_channel_get_actual_position<OpusControlEvent>(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position())
                opus_manager.get_channel_ctl_tree<T>(
                    cursor.ctl_type!!,
                    cursor.channel,
                    actual_beat,
                    actual_position
                )
            }
            CtlLineLevel.Line -> {
                val (actual_beat_key, actual_position) = opus_manager.controller_line_get_actual_position<OpusControlEvent>(cursor.ctl_type!!, cursor.get_beatkey(), cursor.get_position())

                opus_manager.get_line_ctl_tree<T>(
                    cursor.ctl_type!!,
                    actual_beat_key,
                    actual_position
                )
            }
        }

        this.button_remove.isEnabled = cursor.get_position().isNotEmpty()
        this.button_unset.isEnabled = ctl_tree.is_event()
        this.button_duration.isEnabled = ctl_tree.is_event()

        this.button_duration.text = if (ctl_tree.is_event()) {
           this.context.getString(R.string.label_duration, ctl_tree.get_event()!!.duration)
        } else {
            ""
        }
        if (cursor.ctl_type != ControlEventType.Tempo) {
            this.button_duration.visibility = View.VISIBLE
        } else {
            this.button_duration.visibility = View.GONE
        }

        this.widget.set_event(current_event, true)
    }

    fun <T: OpusControlEvent> get_control_event(): T {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        return when (cursor.ctl_level!!) {
            CtlLineLevel.Global -> opus_manager.get_current_global_controller_event(
                cursor.ctl_type!!,
                cursor.beat,
                cursor.get_position()
            )
            CtlLineLevel.Channel ->
                opus_manager.get_current_channel_controller_event(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.beat,
                    cursor.get_position()
                )
            CtlLineLevel.Line -> {
                val beat_key = cursor.get_beatkey()
                opus_manager.get_current_line_controller_event(
                    cursor.ctl_type!!,
                    beat_key,
                    cursor.get_position()
                )
            }
        }
    }

    private fun click_button_duration() {
        this.get_main().get_action_interface().set_ctl_duration<T>()
    }

    private fun long_click_button_duration(): Boolean {
        this.get_main().get_action_interface().set_ctl_duration<T>(1)
        return true
    }
}
