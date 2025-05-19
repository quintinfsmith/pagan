package com.qfs.pagan.ContextMenu

import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuRange(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(
    R.layout.contextmenu_range, R.layout.contextmenu_range_secondary, primary_container, secondary_container) {
    lateinit var button_erase: Button
    lateinit var button_adjust: Button
    lateinit var radio_mode: RadioGroup
    lateinit var label: TextView

    init {
        this.refresh()
    }

    override fun init_properties() {
        this.button_erase = this.primary!!.findViewById(R.id.btnEraseSelection)
        this.button_adjust = this.primary!!.findViewById(R.id.btnAdjust)
        this.label = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.secondary!!.findViewById(R.id.tvMoveModeLabelB)
        } else {
            this.primary.findViewById(R.id.tvMoveModeLabel)
        }

        this.radio_mode = this.secondary!!.findViewById<RadioGroup?>(R.id.rgMoveMode)
    }

    override fun setup_interactions() {
        this.button_erase.setOnClickListener {
            this.get_activity().get_action_interface().unset()
        }

        this.button_adjust.setOnClickListener {
            this.get_activity().get_action_interface().adjust_selection()
        }

        this.radio_mode.setOnCheckedChangeListener { _: RadioGroup, button_id: Int ->
            val main = this.get_activity()
            val new_mode = when (button_id) {
                R.id.rbMoveModeMove -> PaganConfiguration.MoveMode.MOVE
                R.id.rbMoveModeCopy -> PaganConfiguration.MoveMode.COPY
                R.id.rbMoveModeMerge -> PaganConfiguration.MoveMode.MERGE
                else -> PaganConfiguration.MoveMode.COPY
            }
            main.get_action_interface().set_copy_mode(new_mode)
        }
    }

    override fun refresh() {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()

        // NOTE: I *could* just check the endpoints of the range, but eventually if/when I change
        // how percussion works, that'll break so check each key in selection

        var is_selecting_non_percussion = false
        val (first, second) = opus_manager.cursor.get_ordered_range()!!
        for (beat_key in opus_manager.get_beatkeys_in_range(first, second)) {
            if (!opus_manager.is_percussion(beat_key.channel)) {
                is_selecting_non_percussion = true
                break
            }
        }

        this.button_adjust.visibility = if (is_selecting_non_percussion) {
            View.VISIBLE
        } else {
            View.GONE
        }

        this.radio_mode.check(when (main.configuration.move_mode) {
            PaganConfiguration.MoveMode.MOVE -> R.id.rbMoveModeMove
            PaganConfiguration.MoveMode.COPY -> R.id.rbMoveModeCopy
            PaganConfiguration.MoveMode.MERGE -> R.id.rbMoveModeMerge
        })

        if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            this.label.text = when (main.configuration.move_mode) {
                PaganConfiguration.MoveMode.MOVE -> this.context.resources.getString(R.string.label_move_beat)
                PaganConfiguration.MoveMode.MERGE -> this.context.resources.getString(R.string.label_merge_beat)
                else ->  this.context.resources.getString(R.string.label_copy_beat)
            }
        } else {
            return
        }
    }

}
