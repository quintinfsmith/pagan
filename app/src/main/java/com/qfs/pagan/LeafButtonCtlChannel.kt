package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent

class LeafButtonCtlChannel(
    context: Context,
    event: OpusControlEvent?,
    var channel: Int,
    position: List<Int>,
    control_type: ControlEventType
): LeafButtonCtl(context, event, position, CtlLineLevel.Channel, control_type) {
    override fun get_controller(): ActiveController<OpusControlEvent> {
        return this.get_opus_manager().get_all_channels()[this.channel].controllers.get_controller(this.control_type)
    }

    override fun long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Channel && cursor.ctl_type == this.control_type) {
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
        } else {
            opus_manager.cursor_select_channel_ctl_range(
                this.control_type,
                this.channel,
                this.get_beat(),
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
    override fun is_secondary_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_channel_control_secondary_selected(
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

