package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel

class LineLabelCtlLine(context: Context, ctl_type: ControlEventType, val channel: Int, val line_offset: Int): LineLabelCtl(context, CtlLineLevel.Line, ctl_type) {
    override fun on_click() {
        this.get_opus_manager().cursor_select_ctl_row_at_line(this.ctl_type, this.channel, this.line_offset)
    }
}
