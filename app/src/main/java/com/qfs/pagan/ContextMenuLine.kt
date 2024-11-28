package com.qfs.pagan

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusVolumeEvent

class ContextMenuLine(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_row, R.layout.contextmenu_row_secondary, primary_container, secondary_container) {
    lateinit var button_insert: ButtonIcon
    lateinit var button_remove: ButtonIcon
    lateinit var button_choose_percussion: ButtonStd
    lateinit var button_toggle_volume_control: ButtonIcon
    lateinit var widget_volume: ControlWidgetVolume
    lateinit var spacer: Space
    val _visible_line_controls_domain = listOf(ControlEventType.Volume, ControlEventType.Pan)

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
            opus_manager.set_line_controller_initial_event(
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
        for (ctl_type in this._visible_line_controls_domain) {
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

    fun dialog_popup_hidden_lines() {
        val opus_manager = this.get_opus_manager()
        val options = mutableListOf<Pair<ControlEventType, String>>( )
        val cursor = opus_manager.cursor

        for (ctl_type in this._visible_line_controls_domain) {
            if (opus_manager.is_line_ctl_visible(ctl_type, cursor.channel, cursor.line_offset)) {
                continue
            }

            options.add(Pair(ctl_type, ctl_type.name))
        }

        this.get_main().dialog_popup_menu("Show Line Controls...", options) { index: Int, ctl_type: ControlEventType ->
            opus_manager.toggle_line_controller_visibility(ctl_type, cursor.channel, cursor.line_offset)
        }
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
            this.dialog_popup_hidden_lines()
            //this.click_button_toggle_volume_control()
        }
    }

    fun click_button_insert_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.insert_line_at_cursor(1)
    }

    fun long_click_button_insert_line(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        main.dialog_number_input(
            this.context.getString(R.string.dlg_insert_lines),
            1,
            9,
        ) { count: Int ->
            opus_manager.insert_line_at_cursor(count)
        }
        return true
    }

    fun click_button_remove_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.remove_line_at_cursor(1)
    }

    fun long_click_button_remove_line(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        val lines = opus_manager.channels[opus_manager.cursor.channel].size
        val max_lines = Integer.min(lines - 1, lines - opus_manager.cursor.line_offset)
        main.dialog_number_input(
            this.context.getString(R.string.dlg_remove_lines),
            1,
            max_lines
        ) { count: Int ->
            opus_manager.remove_line_at_cursor(count)
        }

        return true
    }

    private fun interact_btnChoosePercussion() {
        val main = this.get_main()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val default_instrument = opus_manager.get_percussion_instrument(cursor.line_offset)

        val options = mutableListOf<Pair<Int, String>>()
        val sorted_keys = main.active_percussion_names.keys.toMutableList()
        sorted_keys.sort()
        for (note in sorted_keys) {
            val name = main.active_percussion_names[note]
            options.add(Pair(note - 27, "${note - 27}: $name"))
        }

        main.dialog_popup_menu(this.context.getString(R.string.dropdown_choose_percussion), options, default_instrument) { _: Int, value: Int ->
            opus_manager.set_percussion_instrument(value)
            main.play_event(
                opus_manager.channels.size,
                value,
                .8F
            )
        }
    }

}
