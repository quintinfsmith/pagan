package com.qfs.pagan.contextmenu

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isEmpty
import com.qfs.pagan.ContextMenuWithController
import com.qfs.pagan.R
import com.qfs.pagan.controlwidgets.ControlWidget
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.cursor.OpusManagerCursor

class ContextMenuControlLeaf<T: EffectEvent>(val widget: ControlWidget<T>, primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(
    R.layout.contextmenu_line_ctl_leaf, R.layout.contextmenu_line_ctl_leaf_secondary, primary_container, secondary_container),
    ContextMenuWithController<T> {
    lateinit var widget_wrapper: LinearLayout
    // --------------------------------
    lateinit var button_split: Button
    lateinit var button_insert: Button
    lateinit var button_remove: Button
    lateinit var button_duration: Button
    lateinit var button_unset: Button

    var active_ctl_level: CtlLineLevel? = null
    var active_ctl_type: EffectType? = null
    var active_channel: Int? = null
    var active_line_offset: Int? = null
    var active_position: List<Int>? = null
    var active_beat: Int? = null

    var first_refresh_skipped = false
    init {
        this.init_widget()
        this.refresh()
    }

    override fun get_widget(): ControlWidget<T> {
        return this.widget
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
        this.get_activity().get_action_interface().unset()
    }
    fun click_button_remove() {
        val opus_manager = this.get_opus_manager()
        opus_manager.remove_at_cursor(1)
    }

    fun long_click_button_remove(): Boolean {
        this.get_activity().get_action_interface().unset_root()
        return true
    }

    fun click_button_insert() {
        this.get_activity().get_action_interface().insert_leaf(1)
    }

    fun long_click_button_insert(): Boolean {
        this.get_activity().get_action_interface().insert_leaf()
        return true
    }

    fun click_button_split() {
        this.get_activity().get_action_interface().split(2)
    }

    fun long_click_button_split(): Boolean {
        this.get_activity().get_action_interface().split()
        return true
    }

    override fun refresh() {
        if (!this.first_refresh_skipped) {
            this.first_refresh_skipped = true
            return
        }


        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val current_event = this.get_control_event<T>()

        this.active_beat = cursor.beat
        this.active_position = cursor.get_position()

        this.active_ctl_type = cursor.ctl_type
        this.active_ctl_level = cursor.ctl_level
        this.active_channel = when (cursor.ctl_level) {
            CtlLineLevel.Line,
            CtlLineLevel.Channel -> cursor.channel
            else -> null
        }
        this.active_line_offset = when (cursor.ctl_level) {
            CtlLineLevel.Line -> cursor.line_offset
            else -> null
        }

        if (this.widget_wrapper.isEmpty()) {
            this.init_widget()
        } else {
            this.widget.set_event(current_event, true)
        }

        val ctl_tree = when (cursor.ctl_level!!) {
            CtlLineLevel.Global -> {
                val (actual_beat, actual_position) = opus_manager.controller_global_get_actual_position<EffectEvent>(cursor.ctl_type!!, cursor.beat, cursor.get_position())
                opus_manager.get_global_ctl_tree<T>(
                    cursor.ctl_type!!,
                    actual_beat,
                    actual_position
                )
            }
            CtlLineLevel.Channel -> {
                val (actual_beat, actual_position) = opus_manager.controller_channel_get_actual_position<EffectEvent>(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position())
                opus_manager.get_channel_ctl_tree<T>(
                    cursor.ctl_type!!,
                    cursor.channel,
                    actual_beat,
                    actual_position
                )
            }
            CtlLineLevel.Line -> {
                val (actual_beat_key, actual_position) = opus_manager.controller_line_get_actual_position<EffectEvent>(cursor.ctl_type!!, cursor.get_beatkey(), cursor.get_position())

                opus_manager.get_line_ctl_tree<T>(
                    cursor.ctl_type!!,
                    actual_beat_key,
                    actual_position
                )
            }
        }

        this.button_remove.isEnabled = cursor.get_position().isNotEmpty()
        this.button_unset.isEnabled = ctl_tree.has_event()
        this.button_duration.isEnabled = ctl_tree.has_event()

        this.button_duration.text = if (ctl_tree.has_event()) {
           this.context.getString(R.string.label_duration, ctl_tree.get_event()!!.duration)
        } else {
            ""
        }

        this.widget.set_event(current_event, true)
    }

    fun <T: EffectEvent> get_control_event(): T {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        return when (cursor.ctl_level!!) {
            CtlLineLevel.Global -> opus_manager.get_current_global_controller_event(
                cursor.ctl_type!!,
                cursor.beat,
                cursor.get_position()
            )
            CtlLineLevel.Channel -> opus_manager.get_current_channel_controller_event(
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
        this.get_activity().get_action_interface().set_ctl_duration<T>()
    }

    private fun long_click_button_duration(): Boolean {
        this.get_activity().get_action_interface().set_ctl_duration<T>(1)
        return true
    }

    override fun matches_cursor(cursor: OpusManagerCursor): Boolean {
        return cursor.mode == CursorMode.Single
                && cursor.ctl_type == this.active_ctl_type
                && cursor.position == this.active_position
                && cursor.beat == this.active_beat
                && cursor.ctl_level == this.active_ctl_level
                && when (cursor.ctl_level) {
                    CtlLineLevel.Global  -> true
                    CtlLineLevel.Channel -> this.active_channel == cursor.channel
                    CtlLineLevel.Line -> this.active_channel == cursor.channel && this.active_line_offset == cursor.line_offset
                    else -> false
                }
    }
}
