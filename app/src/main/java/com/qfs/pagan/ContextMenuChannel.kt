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
        this.button_choose_instrument = primary.findViewById(R.id.btnChooseInstrument)

    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Channel) {
            throw OpusManagerCursor.InvalidModeException(opus_manager.cursor.mode, OpusManagerCursor.CursorMode.Line)
        }

        val channel = opus_manager.cursor.channel

        this.button_choose_instrument.visibility = View.VISIBLE
        val instrument = opus_manager.get_channel_instrument(channel)
       // this.button_choose_instrument.text = if (this.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
       //     this.context.getString(
       //         R.string.label_short_percussion,
       //         instrument
       //     )
       // } else {
       //     this.context.getString(
       //         R.string.label_choose_percussion,
       //         instrument,
       //         main.get_drum_name(instrument) ?: this.context.getString(R.string.drum_not_found)
       //     )
       // }


        val is_percussion = opus_manager.is_percussion(channel)
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
        val channel = opus_manager.get_all_channels()[cursor.channel]
        val default_instrument = channel.get_instrument()

       // val options = mutableListOf<Pair<Int, String>>()
       // val sorted_keys = main.active_percussion_names.keys.toMutableList()
       // sorted_keys.sort()
       // for (note in sorted_keys) {
       //     val name = main.active_percussion_names[note]
       //     options.add(Pair(note - 27, "${note - 27}: $name"))
       // }


       // main.dialog_popup_menu(this.context.getString(R.string.dropdown_choose_percussion), options, default_instrument) { _: Int, value: Int ->
       //     opus_manager.set_percussion_instrument(value)
       //     main.play_event(
       //         opus_manager.channels.size,
       //         value,
       //         80
       //     )
       // }
    }

}
