package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.structure.OpusTree

class LeafButtonCtlGlobal(
    context: Context,
    event: OpusControlEvent?,
    position: List<Int>,
    control_type: ControlEventType
): LeafButtonCtl(context, event, position, CtlLineLevel.Global, control_type) {
    override fun get_tree(): OpusTree<OpusControlEvent> {
        return this.get_opus_manager().get_global_ctl_tree(
            this.control_type,
            this.get_beat(),
            this.position
        )
    }

    override fun long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_global_ctl_end_point(
            this.control_type,
            this.get_beat()
        )
        return true
    }
    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_global_control_selected(
            this.control_type,
            this.get_beat(),
            this.position
        )
    }
    override fun callback_click() {
        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_ctl_at_global(this.control_type, this.get_beat(), this.position)
    }
}

