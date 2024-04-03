package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel

class LineLabelCtlGlobal(context: Context, ctl_type: ControlEventType): LineLabelCtl(context, CtlLineLevel.Global, ctl_type) {
    override fun on_click() {
        this.get_opus_manager().cursor_select_ctl_row_at_global(this.ctl_type)
    }
    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_global_control_line_selected(this.ctl_type)
    }
}
