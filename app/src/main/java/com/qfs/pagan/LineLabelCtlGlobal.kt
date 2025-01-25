package com.qfs.pagan

import android.content.Context
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import kotlin.math.ceil

class LineLabelCtlGlobal(context: Context, ctl_type: ControlEventType): LineLabelCtl(context, CtlLineLevel.Global, ctl_type) {
    override fun on_click() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        try {
            if (cursor.is_selecting_range()) {
                val (first, second) = cursor.range!!
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        if (first != second) {
                            opus_manager.controller_line_to_global_overwrite_range_horizontally(this.ctl_type, first.channel, first.line_offset, first.beat, second.beat)
                        } else {
                            opus_manager.controller_line_to_global_overwrite_line(this.ctl_type, first)
                        }
                        return
                    }

                    CtlLineLevel.Channel -> {
                        if (first != second) {
                            opus_manager.controller_channel_to_global_overwrite_range_horizontally(this.ctl_type, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_channel_to_global_overwrite_line(this.ctl_type, first.channel, first.beat)
                        }
                        return
                    }

                    CtlLineLevel.Global -> {
                        if (first != second) {
                            opus_manager.controller_global_overwrite_range_horizontally(this.ctl_type, first.beat, second.beat)
                        } else {
                            opus_manager.controller_global_overwrite_line(this.ctl_type, first.beat)
                        }
                        return
                    }

                    null -> {}
                }

            }
        } catch (e: OpusLayerBase.InvalidOverwriteCall) {
            // pass
        }
        opus_manager.cursor_select_global_ctl_line(this.ctl_type)
    }

    override fun on_long_click(): Boolean {
        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range()) {
            val (first_key, second_key) = cursor.get_ordered_range()!!
            val default_count = ceil((second_key.beat - first_key.beat + 1).toFloat() / opus_manager.beat_count).toInt()
            when (cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    activity.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first_key != second_key) {
                            opus_manager.controller_line_to_global_overwrite_range_horizontally(this.ctl_type, first_key.channel, first_key.line_offset, first_key.beat, second_key.beat, repeat)
                        } else {
                            opus_manager.controller_line_to_global_overwrite_line(this.ctl_type, first_key, repeat)
                        }
                    }
                }

                CtlLineLevel.Channel -> {
                    activity.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first_key != second_key) {
                            opus_manager.controller_channel_to_global_overwrite_range_horizontally(this.ctl_type, first_key.channel, first_key.beat, second_key.beat, repeat)
                        } else {
                            opus_manager.controller_channel_to_global_overwrite_line(this.ctl_type, first_key.channel, first_key.beat, repeat)
                        }
                    }
                }

                CtlLineLevel.Global -> {
                    activity.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first_key != second_key) {
                            opus_manager.controller_global_overwrite_range_horizontally(this.ctl_type, first_key.beat, second_key.beat, repeat)
                        } else {
                            opus_manager.controller_global_overwrite_line(this.ctl_type, first_key.beat, repeat)
                        }
                    }
                }
                null -> {
                    opus_manager.cursor_select_global_ctl_line(this.ctl_type)
                }
            }
        } else {
            opus_manager.cursor_select_global_ctl_line(this.ctl_type)
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
}
