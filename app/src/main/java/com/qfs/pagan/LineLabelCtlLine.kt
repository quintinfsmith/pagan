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
                if (first != second) {
                    opus_manager.overwrite_line_ctl_range_horizontally(this.ctl_type, first.channel, first.line_offset, first, second)
                } else {
                    opus_manager.overwrite_line_ctl_line(this.ctl_type, first.channel, first.line_offset, first)
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
}
