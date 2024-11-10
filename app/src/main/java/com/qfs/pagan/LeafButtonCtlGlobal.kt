package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ActiveController
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusLayerBase

class LeafButtonCtlGlobal(
    context: Context,
    event: OpusControlEvent?,
    position: List<Int>,
    control_type: ControlEventType
): LeafButtonCtl(context, event, position, CtlLineLevel.Global, control_type) {
    override fun get_controller(): ActiveController<OpusControlEvent> {
        return this.get_opus_manager().controllers.get_controller<OpusControlEvent>(this.control_type)
    }

    override fun long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_linking_range() && cursor.ctl_level == CtlLineLevel.Global && cursor.ctl_type == this.control_type) {
            opus_manager.cursor_select_global_ctl_range(
                this.control_type,
                cursor.beat,
                this.get_beat()
            )
        } else {
            opus_manager.cursor_select_global_ctl_range(
                this.control_type,
                this.get_beat(),
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

    override fun is_secondary_selected(): Boolean {
        val opus_manager = this.get_opus_manager()
        return opus_manager.is_global_control_secondary_selected(
            this.control_type,
            this.get_beat(),
            this.position
        )
    }

    override fun callback_click() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat = this.get_beat()

        if (cursor.is_linking_range() && cursor.ctl_level == this.control_level && cursor.ctl_type == this.control_type) {
            try {
                when (this.get_activity().configuration.link_mode) {
                    PaganConfiguration.LinkMode.COPY -> {
                        opus_manager.copy_global_ctl_to_beat(beat)
                    }

                    PaganConfiguration.LinkMode.MOVE -> {
                        opus_manager.move_global_ctl_to_beat(beat)
                    }

                    PaganConfiguration.LinkMode.LINK -> { /* Unreachable */ }
                    PaganConfiguration.LinkMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is OpusLayerBase.InvalidOverwriteCall -> {
                        opus_manager.cursor_select_ctl_at_global(this.control_type, beat, this.position)
                    }
                    else -> {
                        throw e
                    }
                }
            }
        } else {
            opus_manager.cursor_select_ctl_at_global(this.control_type, beat, this.position)
        }
    }
}

