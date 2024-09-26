package com.qfs.pagan

import android.app.AlertDialog
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.SeekBar
import android.widget.Space
import android.widget.TextView
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
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

    override fun init_properties() {
        val primary = this.primary!!
        this.button_toggle_volume_control = primary.findViewById(R.id.btnToggleVolCtl)
        this.button_insert = primary.findViewById(R.id.btnInsertLine)
        this.button_remove = primary.findViewById(R.id.btnRemoveLine)
        this.button_choose_percussion = primary.findViewById(R.id.btnChoosePercussion)

        this.widget_volume = ControlWidgetVolume(OpusVolumeEvent(0), this.context) { event: OpusControlEvent ->
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
        this.widget_volume.layoutParams.width = MATCH_PARENT
        this.widget_volume.layoutParams.height = WRAP_CONTENT

        this.spacer = primary.findViewById(R.id.spacer)
    }

    override fun refresh() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw OpusManagerCursor.InvalidModeException(opus_manager.cursor.mode, OpusManagerCursor.CursorMode.Line)
        }

        val channel = opus_manager.cursor.channel
        val line_offset = opus_manager.cursor.line_offset

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

        // Hiding volume control line for now (VOLCTLTMP)
        this.button_toggle_volume_control.visibility = View.GONE

        if (opus_manager.is_ctl_line_visible(CtlLineLevel.Line, ControlEventType.Volume)) {
            // Hiding volume control line for now (VOLCTLTMP)
            //this.button_toggle_volume_control.setImageResource(R.drawable.volume_minus)

            this.widget_volume.visibility = View.GONE
        } else {
            // Hiding volume control line for now (VOLCTLTMP)
            //this.button_toggle_volume_control.setImageResource(R.drawable.volume_plus)
            val controller = working_channel.lines[line_offset].controllers.get_controller(ControlEventType.Volume)
            this.widget_volume.set_event(controller.initial_event as OpusVolumeEvent)

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

        // Hiding volume control line for now (VOLCTLTMP)
        //this.button_toggle_volume_control.setOnClickListener {
        //    if (!it.isEnabled) {
        //        return@setOnClickListener
        //    }

        //    this.click_button_toggle_volume_control()
        //}
    }

    fun click_button_toggle_volume_control() {
        val opus_manager = this.get_opus_manager()
        opus_manager.toggle_control_line_visibility(CtlLineLevel.Line, ControlEventType.Volume)
    }

    fun click_button_insert_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.insert_line(1)
    }

    fun long_click_button_insert_line(): Boolean {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        main.dialog_number_input(
            this.context.getString(R.string.dlg_insert_lines),
            1,
            9,
        ) { count: Int ->
            opus_manager.insert_line(count)
        }
        return true
    }

    fun click_button_remove_line() {
        val main = this.get_main()
        val opus_manager = main.get_opus_manager()
        opus_manager.remove_line(1)
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
            opus_manager.remove_line(count)
        }

        return true
    }

    private fun _line_volume_dialog(channel: Int, line_offset: Int) {
        val view = LayoutInflater.from(this.context)
            .inflate(
                R.layout.dialog_line_volume,
                this as ViewGroup,
                false
            )
        val opus_manager = this.get_main().get_opus_manager()
        val line_volume = opus_manager.get_line_volume(channel, line_offset)

        val scroll_bar = view.findViewById<SeekBar>(R.id.line_volume_scrollbar)!!
        scroll_bar.progress = line_volume
        val title_text = view.findViewById<TextView>(R.id.line_volume_title)!!
        title_text.text = resources.getString(R.string.label_volume_scrollbar, line_volume)
        title_text.contentDescription = resources.getString(R.string.label_volume_scrollbar, line_volume)

        scroll_bar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                title_text.text = resources.getString(R.string.label_volume_scrollbar, p1)
                title_text.contentDescription = resources.getString(R.string.label_volume_scrollbar, p1)
                opus_manager.set_line_controller_initial_event(ControlEventType.Volume, channel, line_offset, OpusVolumeEvent(p1))
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(seekbar: SeekBar?) { }
        })

        val dialog = AlertDialog.Builder(this.get_main())
        dialog.setView(view)
        dialog.show()
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
                opus_manager.get_line_volume(opus_manager.channels.size, cursor.line_offset)
            )
        }
    }

}
