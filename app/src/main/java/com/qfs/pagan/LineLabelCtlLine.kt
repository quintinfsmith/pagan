package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase

class LineLabelCtlLine(context: Context, ctl_type: ControlEventType, val channel: Int, val line_offset: Int): LineLabelCtl(context, CtlLineLevel.Line, ctl_type) {
    override fun on_click() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        try {
            if (cursor.is_selecting_range()) {
                val (first, second) = cursor.range!!
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        if (first != second) {
                            opus_manager.controller_line_overwrite_range_horizontally(this.ctl_type, this.channel, this.line_offset, first, second)
                        } else {
                            opus_manager.controller_line_overwrite_line(this.ctl_type, this.channel, this.line_offset, first)
                        }
                        return
                    }

                    CtlLineLevel.Channel -> {
                        if (first != second) {
                            opus_manager.controller_channel_to_line_overwrite_range_horizontally(this.ctl_type, this.channel, this.line_offset, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_channel_to_line_overwrite_line(this.ctl_type, this.channel, this.line_offset, first.channel, first.beat)
                        }
                        return
                    }

                    CtlLineLevel.Global -> {
                        if (first != second) {
                            opus_manager.controller_global_to_line_overwrite_range_horizontally(this.ctl_type, this.channel, this.line_offset, first.beat, second.beat)
                        } else {
                            opus_manager.controller_global_to_line_overwrite_line(this.ctl_type, first.beat, this.channel, this.line_offset)
                        }
                        return
                    }
                    null -> { }
                }
            }
        } catch (e: OpusLayerBase.InvalidOverwriteCall) {
            // pass
        }
        opus_manager.cursor_select_line_ctl_line(
            this.ctl_type,
            this.channel,
            this.line_offset
        )
    }

    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_line_control_line_selected(
            this.ctl_type,
            this.channel,
            this.line_offset
        )
    }
    override fun is_selected_secondary(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_line_control_line_selected_secondary(
            this.ctl_type,
            this.channel,
            this.line_offset
        )
    }
}
