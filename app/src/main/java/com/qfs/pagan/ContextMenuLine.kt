package com.qfs.pagan

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusVolumeEvent

class ContextMenuLine(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_row, R.layout.contextmenu_row_secondary, primary_container, secondary_container), ContextMenuWithController<OpusVolumeEvent> {
    lateinit var button_insert: ImageView
    lateinit var button_remove: ImageView
    lateinit var button_choose_percussion: TextView
    lateinit var button_toggle_volume_control: ImageView
    lateinit var widget_volume: ControlWidgetVolume
    lateinit var spacer: Space

    init {
        this.refresh()
    }
    override fun init_properties() {
        val primary = this.primary!!
        this.button_toggle_volume_control = primary.findViewById(R.id.btnToggleChannelCtl)
        this.button_insert = primary.findViewById(R.id.btnInsertLine)
        this.button_remove = primary.findViewById(R.id.btnRemoveLine)
        this.button_choose_percussion = primary.findViewById(R.id.btnChoosePercussion)

        this.widget_volume = ControlWidgetVolume(OpusVolumeEvent(0F), true, this.context) { event: OpusControlEvent ->
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            opus_manager.controller_line_set_initial_event(
                ControlEventType.Volume,
                cursor.channel,
                cursor.line_offset,
                event
            )
        }

        this.secondary!!.addView(this.widget_volume)
        (this.widget_volume as View).layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        (this.widget_volume as View).layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        this.spacer = primary.findViewById(R.id.spacer)
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw OpusManagerCursor.InvalidModeException(cursor.mode, OpusManagerCursor.CursorMode.Line)
        }

        val channel = cursor.channel
        val line_offset = cursor.line_offset

        if (!opus_manager.is_percussion(channel)) {
            this.spacer.visibility = View.VISIBLE
            this.button_choose_percussion.visibility = View.GONE
        } else {
            this.spacer.visibility = View.GONE
            this.button_choose_percussion.visibility = View.VISIBLE
            val instrument = opus_manager.get_percussion_instrument(line_offset)
            main.populate_active_percussion_names(false)
            this.button_choose_percussion.text = if (this.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                this.context.getString(
                    R.string.label_short_percussion,
                    instrument
                )
            } else {
                this.context.getString(
                    R.string.label_choose_percussion,
                    instrument,
                    main.get_drum_name(instrument) ?: this.context.getString(R.string.drum_not_found)
                )
            }
        }

        val working_channel = opus_manager.get_channel(channel)
        this.button_remove.isEnabled = working_channel.size > 1

        var show_control_toggle = false
        for (ctl_type in OpusLayerInterface.line_controller_domain) {
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

        // Show the volume control regardless of if line control is visible. redundancy is probably better.
        val controller = working_channel.lines[line_offset].controllers.get_controller<OpusVolumeEvent>(ControlEventType.Volume)
        this.widget_volume.set_event(controller.initial_event, true)
        this.widget_volume.visibility = View.VISIBLE
    }

    override fun setup_interactions() {
        this.button_choose_percussion.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.interact_btnChoosePercussion()
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
            this.get_main().get_action_interface().show_hidden_line_controller()
            //this.click_button_toggle_volume_control()
        }
    }

    fun click_button_insert_line() {
        this.get_main().get_action_interface().insert_line(1)
    }

    fun long_click_button_insert_line(): Boolean {
        this.get_main().get_action_interface().insert_line()
        return true
    }

    fun click_button_remove_line() {
        this.get_main().get_action_interface().remove_line(1)
    }

    fun long_click_button_remove_line(): Boolean {
        this.get_main().get_action_interface().remove_line()
        return true
    }

    private fun interact_btnChoosePercussion() {
        this.get_main().get_action_interface().set_percussion_instrument()
    }

    override fun get_widget(): ControlWidget<OpusVolumeEvent> {
        return this.widget_volume
    }
}
