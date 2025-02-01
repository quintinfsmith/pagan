package com.qfs.pagan

import android.util.Log
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import kotlin.math.ceil
import com.qfs.pagan.OpusLayerInterface as OpusManager

/**
 * Handle all the logic between a user action and the OpusManager
 */
class ActionTracker {
    class NoActivityException: Exception()
    enum class TrackedAction {
        NewProject,
        LoadProject,
        CursorSelectGlobalCtlRange,
        CursorSelectChannelCtlRange,
        CursorSelectLineCtlRange,
        CursorSelectRange,
        CursorSelectLeaf,
        PlayEvent, // Not Sure about this one
    }

    var activity: MainActivity? = null
    var ignore_flagged: Boolean = false
    private val action_queue = mutableListOf<Pair<TrackedAction, List<Int>?>>()

    fun attach_activity(activity: MainActivity) {
        this.activity = activity
    }

    fun get_activity(): MainActivity {
        return this.activity ?: throw NoActivityException()
    }

    fun move_selection_to_beat(beat_key: BeatKey) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.move_to_beat(beat_key)
    }

    fun copy_selection_to_beat(beat_key: BeatKey) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.copy_to_beat(beat_key)
    }

    fun merge_selection_into_beat(beat_key: BeatKey) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.merge_into_beat(beat_key)
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select(beat_key, position)
    }

    fun move_line_ctl_to_beat(beat_key: BeatKey) {
        this.get_opus_manager().move_line_ctl_to_beat(beat_key)
    }

    fun copy_line_ctl_to_beat(beat_key: BeatKey) {
        this.get_opus_manager().copy_line_ctl_to_beat(beat_key)
    }


    fun cursor_select_ctl_at_line(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlLine,
            listOf(
                type.i,
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat
            ) + position
        )

        this.get_opus_manager().cursor_select_ctl_at_line(type, beat_key, position)
    }

    fun click_leaf_ctl_channel(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlChannel,
            listOf(
                type.i,
                channel,
                beat
            ) + position
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> opus_manager.copy_channel_ctl_to_beat(channel, beat)
                    PaganConfiguration.MoveMode.MOVE -> opus_manager.move_channel_ctl_to_beat(channel, beat)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is OpusLayerBase.InvalidOverwriteCall -> {
                        opus_manager.cursor_select_ctl_at_channel(type, channel, beat, position)
                    }
                    else -> throw e
                }
            }
        } else {
            opus_manager.cursor_select_ctl_at_channel(type, channel, beat, position)
        }
    }

    fun click_leaf_ctl_global(type: ControlEventType, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlGlobal,
            listOf(
                type.i,
                beat
            ) + position
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> opus_manager.copy_global_ctl_to_beat(beat)
                    PaganConfiguration.MoveMode.MOVE -> opus_manager.move_global_ctl_to_beat(beat)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is OpusLayerBase.InvalidOverwriteCall -> {
                        opus_manager.cursor_select_ctl_at_global(type, beat, position)
                    }
                    else -> {
                        throw e
                    }
                }
            }
        } else {
            opus_manager.cursor_select_ctl_at_global(type, beat, position)
        }
    }

    fun click_label_line_std(channel: Int, line_offset: Int) {
        this.track(
            TrackedAction.ClickLineLabelSTD,
            listOf(channel, line_offset)
        )

        val opus_manager = this.get_opus_manager()

        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range()) {
            val (first_key, second_key) = cursor.get_ordered_range()!!
            if (first_key != second_key) {
                try {
                    opus_manager.overwrite_beat_range_horizontally(
                        channel,
                        line_offset,
                        first_key,
                        second_key
                    )
                } catch (e: OpusLayerBase.MixedInstrumentException) {
                    opus_manager.cursor_select_line(channel, line_offset)
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    opus_manager.cursor_select_line(channel, line_offset)
                }
            } else {
                try {
                    opus_manager.overwrite_line(channel, line_offset, first_key)
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    opus_manager.cursor_select_line(channel, line_offset)
                }
            }
        } else {
            if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == null) {
                opus_manager.cursor_select_channel(channel)
            } else {
                opus_manager.cursor_select_line(channel, line_offset)
            }
        }
    }

    fun click_label_line_ctl_line(type: ControlEventType, channel: Int, line_offset: Int) {
        this.track(
            TrackedAction.ClickLineLabelCTLLine,
            listOf(type.i, channel, line_offset)
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        try {
            if (cursor.is_selecting_range()) {
                val (first, second) = cursor.range!!
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        if (first != second) {
                            opus_manager.controller_line_overwrite_range_horizontally(type, channel, line_offset, first, second)
                        } else {
                            opus_manager.controller_line_overwrite_line(type, channel, line_offset, first)
                        }
                    }

                    CtlLineLevel.Channel -> {
                        if (first != second) {
                            opus_manager.controller_channel_to_line_overwrite_range_horizontally(type, channel, line_offset, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_channel_to_line_overwrite_line(type, channel, line_offset, first.channel, first.beat)
                        }
                    }

                    CtlLineLevel.Global -> {
                        if (first != second) {
                            opus_manager.controller_global_to_line_overwrite_range_horizontally(type, channel, line_offset, first.beat, second.beat)
                        } else {
                            opus_manager.controller_global_to_line_overwrite_line(type, first.beat, channel, line_offset)
                        }
                    }
                    null -> {
                        opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
                    }
                }
            } else {
                opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
            }
        } catch (e: OpusLayerBase.InvalidOverwriteCall) {
            opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
        }
    }

    fun long_click_label_line_ctl_line(type: ControlEventType, channel: Int, line_offset: Int) {
        this.track(
            TrackedAction.LongClickLineLabelCTLLine,
            listOf(type.i, channel, line_offset)
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            val activity = this.get_activity()
            val (first, second) = cursor.get_ordered_range()!!
            val default_count = ceil((opus_manager.beat_count.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
            when (cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    activity.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first != second) {
                            opus_manager.controller_line_overwrite_range_horizontally(type, channel, line_offset, first, second, repeat)
                        } else {
                            opus_manager.controller_line_overwrite_line(type, channel, line_offset, first, repeat)
                        }
                    }
                }
                CtlLineLevel.Channel -> {
                    activity.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first != second) {
                            opus_manager.controller_channel_to_line_overwrite_range_horizontally(type, channel, line_offset, first.channel, first.beat, second.beat, repeat)
                        } else {
                            opus_manager.controller_channel_to_line_overwrite_line(type, channel, line_offset, first.channel, first.beat, repeat)
                        }
                    }
                }

                CtlLineLevel.Global -> {
                    activity.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first != second) {
                            opus_manager.controller_global_to_line_overwrite_range_horizontally(type, channel, line_offset, first.beat, second.beat, repeat)
                        } else {
                            opus_manager.controller_global_to_line_overwrite_line(type, first.beat, channel, line_offset, repeat)
                        }
                    }
                }

                null -> {
                    opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
                }
            }
        } else {
            opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
        }
    }

    fun long_click_label_line_std(channel: Int, line_offset: Int) {
        this.track(
            TrackedAction.LongClickLineLabelSTD,
            listOf(channel, line_offset)
        )

        val context = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range() && cursor.ctl_level == null) {
            val (first_key, second_key) = cursor.get_ordered_range()!!
            val default_count = ceil((opus_manager.beat_count.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
            if (first_key != second_key) {
                context.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                    try {
                        opus_manager.overwrite_beat_range_horizontally(
                            channel,
                            line_offset,
                            first_key,
                            second_key,
                            repeat
                        )
                    } catch (e: OpusLayerBase.MixedInstrumentException) {
                        opus_manager.cursor_select_line(channel, line_offset)
                    } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                        opus_manager.cursor_select_line(channel, line_offset)
                    }
                }
            } else if (opus_manager.is_percussion(first_key.channel) == opus_manager.is_percussion(channel)) {
                context.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                    try {
                        opus_manager.overwrite_line(channel, line_offset, first_key, repeat)
                    } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                        opus_manager.cursor_select_line(channel, line_offset)
                    }
                }
            } else {
                opus_manager.cursor_select_line(channel, line_offset)
            }
        } else {
            if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == null) {
                opus_manager.cursor_select_channel(channel)
            } else {
                opus_manager.cursor_select_line(channel, line_offset)
            }
        }
    }

    fun click_label_line_ctl_channel(type: ControlEventType, channel: Int) {
        this.track(
            TrackedAction.ClickLineLabelCTLChannel,
            listOf(type.i, channel)
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        try {
            if (cursor.is_selecting_range()) {
                val (first, second) = cursor.range!!
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        if (first != second) {
                            opus_manager.controller_line_to_channel_overwrite_range_horizontally(type, channel, first, second)
                        } else {
                            opus_manager.controller_line_to_channel_overwrite_line(type, channel, first)
                        }
                    }
                    CtlLineLevel.Channel -> {
                        if (first != second) {
                            opus_manager.controller_channel_overwrite_range_horizontally(type, channel, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_channel_overwrite_line(type, channel, first.channel, first.beat)
                        }
                    }
                    CtlLineLevel.Global -> {
                        if (first != second) {
                            opus_manager.controller_global_to_channel_overwrite_range_horizontally(type, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_global_to_channel_overwrite_line(type, channel, first.beat)
                        }
                    }
                    null -> {
                        opus_manager.cursor_select_channel_ctl_line(type, channel)
                    }
                }
            } else {
                opus_manager.cursor_select_channel_ctl_line(type, channel)
            }
        } catch (e: OpusLayerBase.InvalidOverwriteCall) {
            opus_manager.cursor_select_channel_ctl_line(type, channel)
        }
    }

    fun long_click_label_line_ctl_channel(type: ControlEventType, channel: Int) {
        this.track(
            TrackedAction.LongClickLineLabelCTLChannel,
            listOf(type.i, channel)
        )

        val opus_manager = this.get_opus_manager()
        val context = this.get_activity()
        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            val activity = this.get_activity()
            val (first, second) = cursor.get_ordered_range()!!
            val default_count = ceil((opus_manager.beat_count.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
            when (cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    activity.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first != second) {
                            opus_manager.controller_line_to_channel_overwrite_range_horizontally(type, channel, first, second, repeat)
                        } else {
                            opus_manager.controller_line_to_channel_overwrite_line(type, channel, first, repeat)
                        }
                    }
                }
                CtlLineLevel.Channel -> {
                    activity.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first != second) {
                            opus_manager.controller_channel_overwrite_range_horizontally(type, channel, first.channel, first.beat, second.beat, repeat)
                        } else {
                            opus_manager.controller_channel_overwrite_line(type, channel, first.channel, first.beat, repeat)
                        }
                    }
                }
                CtlLineLevel.Global -> {
                    activity.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first != second) {
                            opus_manager.controller_global_to_channel_overwrite_range_horizontally(type, first.channel, first.beat, second.beat, repeat)
                        } else {
                            opus_manager.controller_global_to_channel_overwrite_line(type, channel, first.beat, repeat)
                        }
                    }
                }
                null -> {
                    opus_manager.cursor_select_channel_ctl_line(type, channel)
                }
            }
        } else {
            opus_manager.cursor_select_channel_ctl_line(type, channel)
        }
    }

    fun click_label_line_ctl_global(type: ControlEventType) {
        this.track(
            TrackedAction.ClickLineLabelCTLGlobal,
            listOf(type.i)
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        try {
            if (cursor.is_selecting_range()) {
                val (first, second) = cursor.range!!
                when (cursor.ctl_level) {
                    CtlLineLevel.Line -> {
                        if (first != second) {
                            opus_manager.controller_line_to_global_overwrite_range_horizontally(type, first.channel, first.line_offset, first.beat, second.beat)
                        } else {
                            opus_manager.controller_line_to_global_overwrite_line(type, first)
                        }
                    }

                    CtlLineLevel.Channel -> {
                        if (first != second) {
                            opus_manager.controller_channel_to_global_overwrite_range_horizontally(type, first.channel, first.beat, second.beat)
                        } else {
                            opus_manager.controller_channel_to_global_overwrite_line(type, first.channel, first.beat)
                        }
                    }

                    CtlLineLevel.Global -> {
                        if (first != second) {
                            opus_manager.controller_global_overwrite_range_horizontally(type, first.beat, second.beat)
                        } else {
                            opus_manager.controller_global_overwrite_line(type, first.beat)
                        }
                    }

                    null -> {
                        opus_manager.cursor_select_global_ctl_line(type)
                    }
                }
            } else {
                opus_manager.cursor_select_global_ctl_line(type)
            }
        } catch (e: OpusLayerBase.InvalidOverwriteCall) {
            opus_manager.cursor_select_global_ctl_line(type)
        }
    }

    fun long_click_label_line_ctl_global(type: ControlEventType) {
        this.track(
            TrackedAction.LongClickLineLabelCTLGlobal,
            listOf(type.i)
        )

        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            val (first_key, second_key) = cursor.get_ordered_range()!!
            val default_count = ceil((opus_manager.beat_count.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
            when (cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    activity.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first_key != second_key) {
                            opus_manager.controller_line_to_global_overwrite_range_horizontally(type, first_key.channel, first_key.line_offset, first_key.beat, second_key.beat, repeat)
                        } else {
                            opus_manager.controller_line_to_global_overwrite_line(type, first_key, repeat)
                        }
                    }
                }

                CtlLineLevel.Channel -> {
                    activity.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first_key != second_key) {
                            opus_manager.controller_channel_to_global_overwrite_range_horizontally(type, first_key.channel, first_key.beat, second_key.beat, repeat)
                        } else {
                            opus_manager.controller_channel_to_global_overwrite_line(type, first_key.channel, first_key.beat, repeat)
                        }
                    }
                }

                CtlLineLevel.Global -> {
                    activity.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count) { repeat: Int ->
                        if (first_key != second_key) {
                            opus_manager.controller_global_overwrite_range_horizontally(type, first_key.beat, second_key.beat, repeat)
                        } else {
                            opus_manager.controller_global_overwrite_line(type, first_key.beat, repeat)
                        }
                    }
                }
                null -> {
                    opus_manager.cursor_select_global_ctl_line(type)
                }
            }
        } else {
            opus_manager.cursor_select_global_ctl_line(type)
        }
    }

    fun click_column(beat: Int) {
        this.track(
            TrackedAction.ClickColumn,
            listOf(beat)
        )

        this.get_opus_manager().cursor_select_column(beat)
    }

    fun cursor_select_range(first_key: BeatKey, second_key: BeatKey) {
        this.track(
            TrackedAction.CursorSelectRange,
            listOf(
                first_key.channel,
                first_key.line_offset,
                first_key.beat,
                second_key.channel,
                second_key.line_offset,
                second_key.beat
            )
        )

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select_range(first_key, second_key)
    }

    fun cursor_select_line_ctl_range(type: ControlEventType, first_key: BeatKey, second_key: BeatKey) {
        this.track(
            TrackedAction.CursorSelectLineCtlRange,
            listOf(
                type.i,
                first_key.channel,
                first_key.line_offset,
                first_key.beat,
                second_key.channel,
                second_key.line_offset,
                second_key.beat
            )
        )

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select_line_ctl_range(type, first_key, second_key)
    }

    fun cursor_select_channel_ctl_range(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectChannelCtlRange, listOf(type.i, first_beat, second_beat))
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select_channel_ctl_range(type, channel, first_beat, second_beat)
    }


    fun cursor_select_global_ctl_range(type: ControlEventType, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectGlobalCtlRange, listOf(type.i, first_beat, second_beat))
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select_global_ctl_range(type, first_beat, second_beat)
    }


    fun new_project() {
        this.track(TrackedAction.NewProject)

        val activity = this.get_activity()
        val fragment = activity.get_active_fragment()
        fragment?.clearFragmentResult(IntentFragmentToken.Resume.name)
        fragment?.setFragmentResult(IntentFragmentToken.New.name, bundleOf())

        if (fragment !is FragmentEditor) {
            activity.navigate(R.id.EditorFragment)
        }
    }

    fun load_project(path: String) {
        val bytes = path.toByteArray()
        this.track(TrackedAction.LoadProject, List(bytes.size) { i: Int -> bytes[i].toInt()})

        val activity = this.get_activity()
        val fragment = activity.get_active_fragment() ?: return

        activity.loading_reticle_show(activity.getString(R.string.reticle_msg_load_project))
        fragment.setFragmentResult(
            IntentFragmentToken.Load.name,
            bundleOf(Pair("PATH", path))
        )

        if (fragment !is FragmentEditor) {
            activity.navigate(R.id.EditorFragment)
        }
    }

    fun play_event(channel: Int, note: Int, volume: Float) {
        this.track(TrackedAction.PlayEvent, listOf(channel, note, (volume * 128F).toInt()))
        this.get_activity().play_event(channel, note, volume)
    }

    fun ignore(): ActionTracker {
        this.ignore_flagged = true
        return this
    }


    private fun track(token: TrackedAction, args: List<Int>? = null) {
        if (this.ignore_flagged) {
            this.ignore_flagged = false
            return
        }

        this.action_queue.add(Pair(token, args))
        Log.d("PaganTracker", "$token")
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }
}
