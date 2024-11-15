package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuChannel(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_channel, R.layout.contextmenu_channel_secondary, primary_container, secondary_container) {
    lateinit var button_insert: ButtonIcon
    lateinit var button_remove: ButtonIcon
    lateinit var button_choose_instrument: ButtonStd
    lateinit var button_toggle_volume_control: ButtonIcon
    val _visible_line_controls_domain = listOf(
        Pair(CtlLineLevel.Channel, ControlEventType.Volume),
        Pair(CtlLineLevel.Channel, ControlEventType.Pan)
    )

    init {
        this.refresh()
    }
    override fun init_properties() {
        val primary = this.primary!!
        this.button_toggle_volume_control = primary.findViewById(R.id.btnToggleVolCtl)
        this.button_insert = primary.findViewById(R.id.btnInsertLine)
        this.button_remove = primary.findViewById(R.id.btnRemoveLine)
        this.button_choose_instrument = this.secondary!!.findViewById(R.id.btnChooseInstrument)

    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Channel) {
            throw OpusManagerCursor.InvalidModeException(opus_manager.cursor.mode, OpusManagerCursor.CursorMode.Line)
        }

        this.button_choose_instrument.visibility = View.VISIBLE

        val channel_index = opus_manager.cursor.channel
        val channel = opus_manager.get_channel(channel_index)
        val instrument = opus_manager.get_channel_instrument(channel_index)
        val midi_program = channel.midi_program

        val defaults = main.resources.getStringArray(R.array.midi_instruments)
        val supported_instruments = main.get_supported_instrument_names()
        val label = supported_instruments[instrument] ?: if (opus_manager.is_percussion(channel_index)) {
            "$midi_program"
        } else {
            main.resources.getString(R.string.unknown_instrument, defaults[midi_program])
        }
        this.button_choose_instrument.text = label


        val is_percussion = opus_manager.is_percussion(channel_index)
        this.button_remove.isEnabled = (!is_percussion && opus_manager.channels.isNotEmpty()) || (is_percussion && opus_manager.percussion_channel.is_empty())

        var show_control_toggle = false
        for ((ctl_level, ctl_type) in this._visible_line_controls_domain) {
            if (opus_manager.is_ctl_line_visible(ctl_level, ctl_type)) {
                continue
            }
            show_control_toggle = true
            break
        }

        if (!show_control_toggle) {
            this.button_toggle_volume_control.visibility = View.GONE
        }

        this.button_toggle_volume_control.visibility = View.VISIBLE
        this.button_toggle_volume_control.setImageResource(R.drawable.volume_plus)
    }

    fun dialog_popup_hidden_lines() {
        val opus_manager = this.get_opus_manager()
        val options = mutableListOf<Pair<Pair<CtlLineLevel, ControlEventType>, String>>( )

        for ((ctl_level, ctl_type) in this._visible_line_controls_domain) {
            if (opus_manager.is_ctl_line_visible(ctl_level, ctl_type)) {
                continue
            }

            options.add(
                Pair(
                    Pair(ctl_level, ctl_type),
                    ctl_type.name
                )
            )
        }

        this.get_main().dialog_popup_menu("Show Line Controls...", options) { index: Int, (ctl_level, ctl_type): Pair<CtlLineLevel, ControlEventType> ->
            val cursor = opus_manager.cursor
            opus_manager.get_all_channels()[cursor.channel].controllers.new_controller(ctl_type)
            opus_manager.toggle_control_line_visibility(ctl_level, ctl_type)
        }
    }

    override fun setup_interactions() {
        this.button_choose_instrument.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.interact_choose_instrument()
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
        if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
            opus_manager.new_channel(opus_manager.cursor.channel)
        } else {
            opus_manager.new_channel(opus_manager.cursor.channel + 1)
        }
    }

    fun long_click_button_insert_line(): Boolean {
        TODO()
        //val main = this.get_main()
        //val opus_manager = main.get_opus_manager()
        //main.dialog_number_input(
        //    this.context.getString(R.string.dlg_insert_lines),
        //    1,
        //    9,
        //) { count: Int ->
        //    opus_manager.insert_line(count)
        //}
        return true
    }

    fun click_button_remove_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
            try {
                opus_manager.toggle_percussion_visibility()
            } catch (e: OpusLayerInterface.HidingLastChannelException) {
                // pass
            }
        } else if (opus_manager.channels.isNotEmpty()) {
            opus_manager.remove_channel(opus_manager.cursor.channel)
        }
    }

    fun long_click_button_remove_line(): Boolean {
        TODO()
       // val main = this.get_main()
       // val opus_manager = main.get_opus_manager()
       // val lines = opus_manager.channels[opus_manager.cursor.channel].size
       // val max_lines = Integer.min(lines - 1, lines - opus_manager.cursor.line_offset)
       // main.dialog_number_input(
       //     this.context.getString(R.string.dlg_remove_lines),
       //     1,
       //     max_lines
       // ) { count: Int ->
       //     opus_manager.remove_line(count)
       // }

       // return true
    }

    private fun interact_choose_instrument() {
        val main = this.get_main()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        main.dialog_set_channel_instrument(cursor.channel)

    }

}