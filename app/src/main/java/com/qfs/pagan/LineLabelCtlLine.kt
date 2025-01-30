package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel

class LineLabelCtlLine(context: Context, ctl_type: ControlEventType, val channel: Int, val line_offset: Int): LineLabelCtl(context, CtlLineLevel.Line, ctl_type) {
    override fun on_click() {
        this.get_activity().get_action_interface().click_label_line_ctl_line(this.ctl_type, this.channel, this.line_offset)
    }

    override fun on_long_click(): Boolean {
        this.get_activity().get_action_interface().long_click_label_line_ctl_line(this.ctl_type, this.channel, this.line_offset)
        return true
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
