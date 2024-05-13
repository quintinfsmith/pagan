package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel

class LineLabelCtlGlobal(context: Context, ctl_type: ControlEventType): LineLabelCtl(context, CtlLineLevel.Global, ctl_type) {
    override fun on_click() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.is_linking_range()) {
            val (first, second) = cursor.range!!
            opus_manager.overwrite_global_ctl_range_horizontally(this.ctl_type, first.beat, second.beat)
        } else if (cursor.selecting_range) {
            opus_manager.overwrite_global_ctl_row(this.ctl_type, cursor.beat)
        }
        opus_manager.cursor_select_ctl_row_at_global(this.ctl_type)
    }
    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_global_control_line_selected(this.ctl_type)
    }
}
