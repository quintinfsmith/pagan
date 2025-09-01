package com.qfs.pagan.contextmenu

import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.cursor.InvalidCursorState
import com.qfs.pagan.structure.opusmanager.cursor.OpusManagerCursor

class ContextMenuControlLeafRange(primary_container: ViewGroup, secondary_container: ViewGroup): ContextMenuView(
    R.layout.contextmenu_line_ctl_leaf_b, R.layout.contextmenu_line_ctl_leaf_b_secondary, primary_container, secondary_container) {
    lateinit var button_erase: Button
    lateinit var radio_mode: RadioGroup

    var active_ctl_level: CtlLineLevel? = null
    var active_ctl_type: EffectType? = null
    var active_channel: Int? = null
    var active_line_offset: Int? = null
    var active_corners: Pair<Int, Int>? = null

    init {
        this.refresh()
    }

    override fun init_properties() {
        this.button_erase = this.primary!!.findViewById(R.id.btnEraseSelection)
        this.radio_mode = this.secondary!!.findViewById<RadioGroup>(R.id.rgMoveMode)
    }


    override fun setup_interactions() {
        this.button_erase.setOnClickListener {
            this.get_opus_manager().unset()
        }
        this.radio_mode.setOnCheckedChangeListener { _: RadioGroup, button_id: Int ->
            val main = this.get_activity()
            main.configuration.move_mode = when (button_id) {
                R.id.rbMoveModeMove -> PaganConfiguration.MoveMode.MOVE
                R.id.rbMoveModeCopy -> PaganConfiguration.MoveMode.COPY
                else -> PaganConfiguration.MoveMode.COPY
            }
            main.save_configuration()
            this.refresh()
        }

    }

    override fun refresh() {
        val main = this.get_activity()

        val cursor = main.get_opus_manager().cursor
        this.active_corners = cursor.get_ordered_range()?.let {
            Pair(it.first.beat, it.second.beat)
        }  ?: throw InvalidCursorState()

        this.active_ctl_level = cursor.ctl_level
        this.active_ctl_type = cursor.ctl_type
        this.active_channel = when (cursor.ctl_level) {
            CtlLineLevel.Line,
            CtlLineLevel.Channel -> cursor.channel
            else -> null
        }
        this.active_line_offset = when (cursor.ctl_level) {
            CtlLineLevel.Line -> cursor.line_offset
            else -> null
        }


        this.radio_mode.check(when (main.configuration.move_mode) {
            PaganConfiguration.MoveMode.MOVE -> R.id.rbMoveModeMove
            PaganConfiguration.MoveMode.COPY -> R.id.rbMoveModeCopy
            else -> R.id.rbMoveModeCopy
        })
    }


    override fun matches_cursor(cursor: OpusManagerCursor): Boolean {
        val pair = cursor.get_ordered_range() ?: return false
        return cursor.mode == CursorMode.Range
                && cursor.ctl_type == this.active_ctl_type
                && Pair(pair.first.beat, pair.second.beat) == this.active_corners
                && cursor.ctl_level == this.active_ctl_level
                && when (cursor.ctl_level) {
            CtlLineLevel.Global  -> true
            CtlLineLevel.Channel -> this.active_channel == cursor.channel
            CtlLineLevel.Line -> this.active_channel == cursor.channel && this.active_line_offset == cursor.line_offset
            else -> false
        }
    }

}