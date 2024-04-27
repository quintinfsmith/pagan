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
        val cursor = opus_manager.cursor
        if (!cursor.is_linking || cursor.ctl_level != CtlLineLevel.Global || cursor.ctl_type != this.control_type) {
            opus_manager.cursor_select_global_ctl_end_point(
                this.control_type,
                this.get_beat()
            )
        } else if (!cursor.is_linking_range()) {
            opus_manager.cursor_select_global_ctl_range(
                this.control_type,
                cursor.beat,
                this.get_beat()
            )
        } else {
            opus_manager.cursor_select_global_ctl_range(
                this.control_type,
                cursor.range!!.first.beat,
                this.get_beat()
            )
        }
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
        val cursor = opus_manager.cursor
        val beat = this.get_beat()
        if (!cursor.is_linking || cursor.ctl_level != this.control_level || cursor.ctl_type != this.control_type) {
            opus_manager.cursor_select_ctl_at_global(this.control_type, beat, this.position)
        } else {
            try {
                when (this.get_activity().configuration.link_mode) {
                    PaganConfiguration.LinkMode.COPY -> {
                        opus_manager.copy_global_ctl_to_beat(beat)
                    }

                    PaganConfiguration.LinkMode.MOVE -> {
                        opus_manager.move_global_ctl_to_beat(beat)
                    }

                    PaganConfiguration.LinkMode.LINK -> {/* Unreachable */
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                opus_manager.cursor_select_ctl_at_global(this.control_type, beat, this.position)
            }
        }
    }
}

