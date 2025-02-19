package com.qfs.pagan

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import com.qfs.pagan.opusmanager.OpusManagerCursor

class ContextMenuRange(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(R.layout.contextmenu_range, R.layout.contextmenu_range_secondary, primary_container, secondary_container) {
    lateinit var button_erase: ImageView
    lateinit var radio_mode: RadioGroup
    lateinit var label: TextView

    init {
        this.refresh()
    }

    override fun init_properties() {
        this.button_erase = this.primary!!.findViewById(R.id.btnEraseSelection)
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

        this.radio_mode.check(when (main.configuration.move_mode) {
            PaganConfiguration.MoveMode.MOVE -> R.id.rbMoveModeMove
            PaganConfiguration.MoveMode.COPY -> R.id.rbMoveModeCopy
            PaganConfiguration.MoveMode.MERGE -> R.id.rbMoveModeMerge
        })

        if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Single) {
            this.label.text = when (main.configuration.move_mode) {
                PaganConfiguration.MoveMode.MOVE -> this.context.resources.getString(R.string.label_move_beat)
                PaganConfiguration.MoveMode.COPY ->  this.context.resources.getString(R.string.label_copy_beat)
                PaganConfiguration.MoveMode.MERGE -> this.context.resources.getString(R.string.label_merge_beat)
            }
        } else {
            return
        }
    }

}
