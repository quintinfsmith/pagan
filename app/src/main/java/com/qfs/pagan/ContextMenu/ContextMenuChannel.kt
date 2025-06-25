package com.qfs.pagan.ContextMenu

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.button.MaterialButton
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.opusmanager.OpusManagerCursor

/*
    CHANNEL_CTLS. Channel controls are not currently considered in playback, so I'll comment out the controls for a future release
*/

class ContextMenuChannel(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(
    R.layout.contextmenu_channel, R.layout.contextmenu_channel_secondary, primary_container, secondary_container) {
    lateinit var button_insert: Button
    lateinit var button_remove: Button
    lateinit var button_choose_instrument: Button
    lateinit var button_toggle_controllers: Button
    lateinit var button_mute: Button
    lateinit var button_adjust: Button

    override fun init_properties() {
        val primary = this.primary!!
        val secondary = this.secondary!!
        this.button_toggle_controllers = primary.findViewById(R.id.btnToggleChannelCtl)
        this.button_insert = primary.findViewById(R.id.btnInsertLine)
        this.button_remove = primary.findViewById(R.id.btnRemoveLine)
        this.button_adjust = primary.findViewById(R.id.btnAdjust)
        this.button_choose_instrument = secondary.findViewById(R.id.btnChooseInstrument)
        this.button_mute = secondary.findViewById(R.id.btnMuteChannel)
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

        this.button_adjust.visibility = if (opus_manager.is_percussion(channel_index)) {
            View.GONE
        } else {
            View.VISIBLE
        }

        this.button_remove.isEnabled = opus_manager.channels.size > 1

        var show_control_toggle = false
        for (ctl_type in OpusLayerInterface.Companion.channel_controller_domain) {
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

        (this.button_mute as MaterialButton).setIconResource(
            if (channel.muted) {
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

        this.button_adjust.setOnClickListener {
            this.get_activity().get_action_interface().adjust_selection()
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
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor

            val is_mute = opus_manager.get_channel(cursor.channel).muted
            val tracker = this.get_activity().get_action_interface()

            if (is_mute) {
                tracker.channel_unmute()
            } else {
                tracker.channel_mute()
            }
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
