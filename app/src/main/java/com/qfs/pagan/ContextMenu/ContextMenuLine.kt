package com.qfs.pagan.ContextMenu

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Space
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.ContextMenuWithController
import com.qfs.pagan.ControlWidget.ControlWidget
import com.qfs.pagan.ControlWidget.ControlWidgetVolume
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.ControlEventType
import com.qfs.pagan.structure.opusmanager.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.OpusControlEvent
import com.qfs.pagan.structure.opusmanager.OpusManagerCursor
import com.qfs.pagan.structure.opusmanager.OpusVolumeEvent

class ContextMenuLine(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(
    R.layout.contextmenu_row, R.layout.contextmenu_row_secondary, primary_container, secondary_container),
    ContextMenuWithController<OpusVolumeEvent> {
    lateinit var button_insert: Button
    lateinit var button_adjust: Button
    lateinit var button_remove: Button
    lateinit var button_choose_percussion: Button
    lateinit var button_toggle_volume_control: Button
    lateinit var button_mute: Button
    lateinit var widget_volume: ControlWidgetVolume
    lateinit var spacer: Space

    override fun init_properties() {
        val primary = this.primary!!
        this.button_toggle_volume_control = primary.findViewById(R.id.btnToggleChannelCtl)
        this.button_insert = primary.findViewById(R.id.btnInsertLine)
        this.button_remove = primary.findViewById(R.id.btnRemoveLine)
        this.button_adjust = primary.findViewById(R.id.btnAdjust)
        this.button_choose_percussion = primary.findViewById(R.id.btnChoosePercussion)
        this.spacer = primary.findViewById<Space>(R.id.spacer)
        this.button_mute = this.secondary!!.findViewById(R.id.btnMuteLine)

        this.widget_volume = ControlWidgetVolume(
            OpusVolumeEvent(0F),
            CtlLineLevel.Line,
            true,
            this.context
        ) { event: OpusControlEvent ->
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            opus_manager.controller_line_set_initial_event(
                ControlEventType.Volume,
                cursor.channel,
                cursor.line_offset,
                event
            )
        }

        this.secondary.addView(this.widget_volume)
        (this.widget_volume as View).layoutParams.width = 0
        ((this.widget_volume as View).layoutParams as LinearLayout.LayoutParams).weight = 1f

        (this.widget_volume as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun refresh() {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw OpusManagerCursor.InvalidModeException(cursor.mode, OpusManagerCursor.CursorMode.Line)
        }

        val channel = cursor.channel
        val line_offset = cursor.line_offset

        if (!opus_manager.is_percussion(channel)) {
            this.button_choose_percussion.visibility = View.GONE
            (this.spacer.layoutParams as LinearLayout.LayoutParams).weight = 100f
        } else {
            this.button_choose_percussion.visibility = View.VISIBLE
            val instrument = opus_manager.get_percussion_instrument(channel, line_offset)
            main.populate_active_percussion_names(cursor.channel, false)
            this.button_choose_percussion.text = if (this.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                this.context.getString(
                    R.string.label_short_percussion,
                    instrument
                )
            } else {
                this.context.getString(
                    R.string.label_choose_percussion,
                    instrument,
                    main.get_drum_name(cursor.channel, instrument) ?: this.context.getString(R.string.drum_not_found)
                )
            }
            (this.spacer.layoutParams as LinearLayout.LayoutParams).weight = 1f
        }

        this.button_adjust.visibility = if (opus_manager.is_percussion(channel)) {
            View.GONE
        } else {
            View.VISIBLE
        }

        val working_channel = opus_manager.get_channel(channel)
        this.button_remove.isEnabled = working_channel.size > 1

        var show_control_toggle = false
        for (ctl_type in OpusLayerInterface.Companion.line_controller_domain) {
            if (opus_manager.is_line_ctl_visible(ctl_type, cursor.channel, cursor.line_offset)) {
                continue
            }
            show_control_toggle = true
            break
        }

        if (!show_control_toggle) {
            this.button_toggle_volume_control.visibility = View.GONE
        } else {
            this.button_toggle_volume_control.visibility = View.VISIBLE
        }

        // TODO: I don't like how I'm doing this. Should be a custom button?
        (this.button_mute as MaterialButton).setIconResource(
            if (opus_manager.get_channel(channel).get_line(line_offset).muted) {
                R.drawable.mute
            } else {
                R.drawable.unmute
            }
        )

        // Show the volume control regardless of if line control is visible. redundancy is probably better.
        val controller = working_channel.lines[line_offset].controllers.get_controller<OpusVolumeEvent>(ControlEventType.Volume)
        this.widget_volume.set_event(controller.initial_event, true)
        this.widget_volume.visibility = View.VISIBLE
    }

    override fun setup_interactions() {
        this.button_adjust.setOnClickListener {
            this.get_activity().get_action_interface().adjust_selection()
        }

        this.button_choose_percussion.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.interact_btnChoosePercussion()
        }

        this.button_choose_percussion.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            val line = opus_manager.get_all_channels()[cursor.channel].lines[cursor.line_offset]
            this.get_activity().dialog_color_picker(line.color ?: this.get_activity().getColor(R.color.leaf_main).toInt()) { color: Int? ->
                opus_manager.set_line_color(cursor.channel, cursor.line_offset, color)
            }

            true
        }

        this.button_insert.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }
            this.long_click_button_insert_line()
        }

        this.button_insert.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_insert_line()
        }

        this.button_remove.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_remove_line()
        }

        this.button_remove.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }

            this.long_click_button_remove_line()
        }

        this.button_toggle_volume_control.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.get_activity().get_action_interface().show_hidden_line_controller()
            //this.click_button_toggle_volume_control()
        }

        this.button_mute.setOnClickListener {
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor

            val is_mute = opus_manager.get_channel(cursor.channel).get_line(cursor.line_offset).muted
            val tracker = this.get_activity().get_action_interface()

            if (is_mute) {
                tracker.line_unmute()
            } else {
                tracker.line_mute()
            }
        }
    }

    fun click_button_insert_line() {
        this.get_activity().get_action_interface().insert_line(1)
    }

    fun long_click_button_insert_line(): Boolean {
        this.get_activity().get_action_interface().insert_line()
        return true
    }

    fun click_button_remove_line() {
        this.get_activity().get_action_interface().remove_line(1)
    }

    fun long_click_button_remove_line(): Boolean {
        this.get_activity().get_action_interface().remove_line()
        return true
    }

    private fun interact_btnChoosePercussion() {
        this.get_activity().get_action_interface().set_percussion_instrument()
    }

    override fun get_widget(): ControlWidget<OpusVolumeEvent> {
        return this.widget_volume
    }
}
