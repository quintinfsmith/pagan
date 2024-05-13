package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.structure.OpusTree

class LeafButtonCtlChannel(
    context: Context,
    event: OpusControlEvent?,
    var channel: Int,
    position: List<Int>,
    control_type: ControlEventType
): LeafButtonCtl(context, event, position, CtlLineLevel.Channel, control_type) {
    override fun get_tree(): OpusTree<OpusControlEvent> {
        return this.get_opus_manager().get_channel_ctl_tree(
            this.control_type,
            this.channel,
            this.get_beat(),
            this.position
        )
    }

    override fun long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (!cursor.selecting_range || cursor.ctl_level != CtlLineLevel.Channel || cursor.ctl_type != this.control_type) {
            opus_manager.cursor_select_channel_ctl_end_point(
                this.control_type,
                this.channel,
                this.get_beat()
            )
        } else if (!cursor.is_linking_range()) {
            opus_manager.cursor_select_channel_ctl_range(
                this.control_type,
                this.channel,
                cursor.beat,
                this.get_beat()
            )
        } else {
            // Currently, can't select multiple channels in a range
            if (this.channel != cursor.range!!.first.channel) {
                return true
            }

            opus_manager.cursor_select_channel_ctl_range(
                this.control_type,
                this.channel,
                cursor.range!!.first.beat,
                this.get_beat()
            )
        }
        return true
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

    override fun callback_click() {
        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_ctl_at_channel(this.control_type, this.channel, this.get_beat(), this.position)
    }
}

