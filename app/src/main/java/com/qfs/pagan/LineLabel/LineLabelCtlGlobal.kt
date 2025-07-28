package com.qfs.pagan.LineLabel

import android.content.Context
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel

class LineLabelCtlGlobal(context: Context, ctl_type: EffectType): LineLabelCtl(context, CtlLineLevel.Global, ctl_type) {
    override fun on_click() {
        val tracker = this.get_activity().get_action_interface()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range()) {
            tracker.repeat_selection_ctl_global(this.ctl_type, -1)
        } else {
            tracker.cursor_select_global_ctl_line(this.ctl_type)
        }
    }

    override fun on_long_click(): Boolean {
        val tracker = this.get_activity().get_action_interface()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range()) {
            tracker.repeat_selection_ctl_global(this.ctl_type)
        } else {
            tracker.cursor_select_global_ctl_line(this.ctl_type)
        }

        return true
    }

    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_global_control_line_selected(this.ctl_type)
    }
    override fun is_selected_secondary(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_global_control_line_selected_secondary(this.ctl_type)
    }

    override fun is_muted(): Boolean {
        return false
    }

}
