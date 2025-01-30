package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel

class LineLabelCtlChannel(context: Context, ctl_type: ControlEventType, val channel: Int): LineLabelCtl(context, CtlLineLevel.Channel, ctl_type) {

    override fun on_click() {
        this.get_activity().get_action_interface().click_label_line_ctl_channel(this.ctl_type, this.channel)
    }

    override fun on_long_click(): Boolean {
        this.get_activity().get_action_interface().long_click_label_line_ctl_channel(this.ctl_type, this.channel)
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
}
