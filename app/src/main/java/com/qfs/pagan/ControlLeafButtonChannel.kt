package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.structure.OpusTree

class ControlLeafButtonChannel(
    context: Context,
    event: OpusControlEvent?,
    var channel: Int,
    position: List<Int>,
    control_type: ControlEventType
): ControlLeafButton(context, event, position, CtlLineLevel.Channel, control_type) {
    override fun get_tree(): OpusTree<OpusControlEvent> {
        return this.get_opus_manager().get_channel_ctl_tree(
            this.control_type,
            this.channel,
            this.get_beat(),
            this.position
        )
    }

    override fun is_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_channel_control_selected(
            this.control_type,
            this.channel,
            this.get_beat(),
            this.position
        )
    }
}

