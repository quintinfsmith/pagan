package com.qfs.pagan

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.qfs.pagan.opusmanager.OpusManagerCursor

/*
    CHANNEL_CTLS. Channel controls are not currently considered in playback, so I'll comment out the controls for a future release
*/

class ContextMenuChannel(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_channel, R.layout.contextmenu_channel_secondary, primary_container, secondary_container) {
    lateinit var button_insert: ImageView
    lateinit var button_remove: ImageView
    lateinit var button_choose_instrument: TextView
    lateinit var button_toggle_controllers: ImageView
    lateinit var button_mute: ImageView

    init {
        this.refresh()
    }
    override fun init_properties() {
        val primary = this.primary!!
        this.button_toggle_controllers = primary.findViewById(R.id.btnToggleChannelCtl)
        this.button_insert = primary.findViewById(R.id.btnInsertLine)
        this.button_remove = primary.findViewById(R.id.btnRemoveLine)
        this.button_choose_instrument = this.secondary!!.findViewById(R.id.btnChooseInstrument)
        this.button_mute = primary.findViewById(R.id.btnMuteLine)
    }

    override fun refresh() {
        val main = this.get_activity()
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
        this.button_remove.isEnabled = (!is_percussion && opus_manager.channels.isNotEmpty()) || (is_percussion && opus_manager.channels.isNotEmpty())
        this.button_remove.setImageResource(
            if (is_percussion) {
                R.drawable.hide
            } else {
                R.drawable.remove_line
            }
        )

        var show_control_toggle = false
        for (ctl_type in OpusLayerInterface.channel_controller_domain) {
            if (opus_manager.is_channel_ctl_visible(ctl_type, channel_index)) {
                continue
            }
            show_control_toggle = true
            break
        }
        if (!show_control_toggle) {
            this.button_toggle_controllers.visibility = View.GONE
        } else {
            this.button_toggle_controllers.visibility = View.VISIBLE
        }

        val cursor = opus_manager.cursor
        this.button_mute.setImageResource(
            if (opus_manager.get_channel(cursor.channel).muted) {
                R.drawable.mute
            } else {
                R.drawable.unmute
            }
        )
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
            this.long_click_button_insert_channel()
        }

        this.button_insert.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_insert_channel()
        }

        this.button_remove.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.click_button_remove_channel()
        }

        this.button_remove.setOnLongClickListener {
            if (!it.isEnabled) {
                return@setOnLongClickListener false
            }

            this.long_click_button_remove_channel()
        }

        this.button_toggle_controllers.setOnClickListener {
            if (!it.isEnabled) {
                return@setOnClickListener
            }
            this.get_activity().get_action_interface().show_hidden_channel_controller()
        }

        this.button_mute.setOnClickListener {
            this.get_activity().get_action_interface().toggle_channel_mute()
        }
    }

    fun click_button_insert_channel() {
        this.get_activity().get_action_interface().insert_channel()
    }

    fun long_click_button_insert_channel(): Boolean {
        this.get_activity().get_action_interface().insert_channel()
        return true
    }

    fun click_button_remove_channel() {
        this.get_activity().get_action_interface().remove_channel()
    }

    fun long_click_button_remove_channel(): Boolean {
        this.get_activity().get_action_interface().remove_channel()
        return true
    }

    private fun interact_choose_instrument() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        this.get_activity().get_action_interface().set_channel_instrument(cursor.channel)
    }

}
