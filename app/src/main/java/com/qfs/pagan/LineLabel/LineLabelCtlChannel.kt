package com.qfs.pagan.LineLabel

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel

class LineLabelCtlChannel(context: Context, ctl_type: ControlEventType, val channel: Int): LineLabelCtl(context, CtlLineLevel.Channel, ctl_type) {

    override fun on_click() {
        val tracker = this.get_activity().get_action_interface()

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range()) {
            tracker.repeat_selection_ctl_channel(this.ctl_type, this.channel, -1)
        } else {
            tracker.cursor_select_channel_ctl_line(this.ctl_type, this.channel)
        }
    }

    override fun on_long_click(): Boolean {
        val tracker = this.get_activity().get_action_interface()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range()) {
            tracker.repeat_selection_ctl_channel(this.ctl_type, this.channel)
        } else {
            tracker.cursor_select_channel_ctl_line(this.ctl_type, this.channel)
        }

        return true
    }

    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_channel_control_line_selected(
            this.ctl_type,
            this.channel
        )
    }

    override fun is_selected_secondary(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_channel_control_line_selected_secondary(
            this.ctl_type,
            this.channel
        )
    }

    override fun is_muted(): Boolean {
        val opus_manager = this.get_opus_manager()
        val channel = opus_manager.get_channel(this.channel)
        return channel.muted
    }
}
