package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusLayerBase

class LeafButtonCtlLine(
    context: Context,
    event: OpusControlEvent?,
    var channel: Int,
    var line_offset: Int,
    position: List<Int>,
    control_type: ControlEventType
): LeafButtonCtl(context, event, position, CtlLineLevel.Line, control_type) {

    override fun get_controller(): ActiveController<OpusControlEvent> {
        return this.get_opus_manager().get_all_channels()[this.channel].lines[line_offset].controllers.get_controller(this.control_type)

    }

    override fun long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = this.get_beat_key()

        if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Line && cursor.range!!.first.channel == beat_key.channel && cursor.range!!.first.line_offset == beat_key.line_offset && this.control_type == cursor.ctl_type) {
            opus_manager.cursor_select_line_ctl_range(
                this.control_type,
                opus_manager.cursor.range!!.first,
                beat_key
            )
        } else {
            opus_manager.cursor_select_line_ctl_range(
                this.control_type,
                beat_key,
                beat_key
            )
        }

        return true
    }

    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_line_control_selected(
            this.control_type,
            BeatKey(
                this.channel,
                this.line_offset,
                this.get_beat()
            ),
            this.position
        )
    }

    override fun is_secondary_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_line_control_secondary_selected(
            this.control_type,
            BeatKey(
                this.channel,
                this.line_offset,
                this.get_beat()
            ),
            this.position
        )
    }

    override fun callback_click() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat = this.get_beat()
        val beat_key = this.get_beat_key()

        if (cursor.is_selecting_range() && cursor.ctl_level == this.control_level && cursor.ctl_type == this.control_type) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> {
                        opus_manager.copy_line_ctl_to_beat(beat_key)
                    }

                    PaganConfiguration.MoveMode.MOVE -> {
                        opus_manager.move_line_ctl_to_beat(beat_key)
                    }

                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is OpusLayerBase.InvalidOverwriteCall -> {
                        opus_manager.cursor_select_ctl_at_line(this.control_type, this.get_beat_key(), this.position)
                    }
                    else -> throw e
                }
            }
        } else {
            opus_manager.cursor_select_ctl_at_line(
                this.control_type,
                BeatKey(
                    this.channel,
                    this.line_offset,
                    beat
                ),
                this.position
            )
        }
    }

    fun get_beat_key(): BeatKey {
        val opus_manager = this.get_opus_manager()
        val (pointer, ctl_level, _) = opus_manager.get_ctl_line_info(
            opus_manager.get_ctl_line_from_row((this.parent as CellLayout).row)
        )

        val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
        return BeatKey(channel, line_offset, this.get_beat())
    }
}


