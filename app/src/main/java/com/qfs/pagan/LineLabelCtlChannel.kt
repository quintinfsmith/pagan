package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase

class LineLabelCtlChannel(context: Context, ctl_type: ControlEventType, val channel: Int): LineLabelCtl(context, CtlLineLevel.Channel, ctl_type) {
    override fun on_click() {

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        try {
            if (cursor.is_selecting_range()) {
                val (first, second) = cursor.range!!
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        if (first != second) {
                            opus_manager.controller_line_to_channel_overwrite_range_horizontally(this.ctl_type, this.channel, first, second)
                        } else {
                            opus_manager.controller_line_to_channel_overwrite_line(this.ctl_type, this.channel, first)
                        }
                        return
                    }
                    CtlLineLevel.Channel -> {
                        if (first != second) {
                            opus_manager.controller_channel_overwrite_range_horizontally(this.ctl_type, this.channel, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_channel_overwrite_line(this.ctl_type, this.channel, first.channel, first.beat)
                        }
                        return
                    }
                    CtlLineLevel.Global -> {
                        if (first != second) {
                            opus_manager.controller_global_to_channel_overwrite_range_horizontally(this.ctl_type, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_global_to_channel_overwrite_line(this.ctl_type, this.channel, first.beat)
                        }
                        return
                    }
                    null -> {}
                }
            }
        } catch (e: OpusLayerBase.InvalidOverwriteCall) {
            // pass
        }
        opus_manager.cursor_select_channel_ctl_line(this.ctl_type, this.channel)
    }

    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_channel_control_line_selected(
            this.ctl_type,
            this.channel
        )
    }
}
