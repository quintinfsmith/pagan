package com.qfs.pagan

import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResult
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
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
        CursorSelectColumn,
        CursorSelectGlobalCtlRange,
        CursorSelectChannelCtlRange,
        CursorSelectLineCtlRange,
        CursorSelectRange,
        CursorSelectLeaf,
        CursorSelectLeafCtlLine,
        CursorSelectLeafCtlChannel,
        CursorSelectLeafCtlGlobal,
        CursorSelectLine,
        CursorSelectLineCtlLine,
        CursorSelectChannelCtlLine,
        CursorSelectGlobalCtlLine,
        PlayEvent, // Not Sure about this one
        RepeatSelectionStd,
        RepeatSelectionCtlLine,
        RepeatSelectionCtlChannel,
        RepeatSelectionCtlGlobal,
        MoveLineCtlToBeat,
        MoveChannelCtlToBeat,
        MoveGlobalCtlToBeat,
        MoveSelectionToBeat,
        CopyLineCtlToBeat,
        CopyChannelCtlToBeat,
        CopyGlobalCtlToBeat,
        CopySelectionToBeat,
        MergeSelectionIntoBeat
    }

    var activity: MainActivity? = null
    private var ignore_flagged: Boolean = false
    private val action_queue = mutableListOf<Pair<TrackedAction, List<Int?>?>>()

    fun attach_activity(activity: MainActivity) {
        this.activity = activity
    }

    fun get_activity(): MainActivity {
        return this.activity ?: throw NoActivityException()
    }

    fun move_selection_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MoveSelectionToBeat,
            beat_key.toList()
        )
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.move_to_beat(beat_key)
    }

    fun copy_selection_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.CopySelectionToBeat,
            beat_key.toList()
        )
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.copy_to_beat(beat_key)
    }

    fun merge_selection_into_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MergeSelectionIntoBeat,
            beat_key.toList()
        )
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.merge_into_beat(beat_key)
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeaf,
            beat_key.toList() + position
        )

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select(beat_key, position)
    }

    fun move_line_ctl_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MoveLineCtlToBeat,
            beat_key.toList()
        )
        this.get_opus_manager().move_line_ctl_to_beat(beat_key)
    }

    fun copy_line_ctl_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.CopyLineCtlToBeat,
            beat_key.toList()
        )
        this.get_opus_manager().copy_line_ctl_to_beat(beat_key)
    }

    fun move_channel_ctl_to_beat(channel: Int, beat: Int) {
        this.track(
            TrackedAction.MoveChannelCtlToBeat,
            listOf(channel, beat)
        )
        this.get_opus_manager().move_channel_ctl_to_beat(channel, beat)
    }

    fun copy_channel_ctl_to_beat(channel: Int, beat: Int) {
        this.track(
            TrackedAction.CopyChannelCtlToBeat,
            listOf(channel, beat)
        )
        this.get_opus_manager().copy_channel_ctl_to_beat(channel, beat)
    }

    fun move_global_ctl_to_beat(beat: Int) {
        this.track(
            TrackedAction.MoveGlobalCtlToBeat,
            listOf(beat)
        )
        this.get_opus_manager().move_global_ctl_to_beat(beat)
    }

    fun copy_global_ctl_to_beat(beat: Int) {
        this.track(
            TrackedAction.CopyGlobalCtlToBeat,
            listOf(beat)
        )
        this.get_opus_manager().copy_global_ctl_to_beat(beat)
    }

    fun cursor_select_ctl_at_line(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlLine,
            listOf(type.i, beat_key.channel, beat_key.line_offset, beat_key.beat) + position
        )

        this.get_opus_manager().cursor_select_ctl_at_line(type, beat_key, position)
    }

    fun cursor_select_ctl_at_channel(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlChannel,
            listOf(type.i, channel, beat) + position
        )

        this.get_opus_manager().cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    fun cursor_select_ctl_at_global(type: ControlEventType, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlGlobal,
            listOf(type.i, beat) + position
        )
        this.get_opus_manager().cursor_select_ctl_at_global(type, beat, position)
    }

    fun repeat_selection_std(channel: Int, line_offset: Int, repeat: Int? = null) {
        this.track(
            TrackedAction.RepeatSelectionStd,
            listOf(channel, line_offset, repeat)
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val context = this.get_activity()

        val (first_key, second_key) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.beat_count.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()

        // a value of negative 1 means use default value, where a null would show the dialog
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        if (first_key != second_key) {
            this.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
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
            this.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                try {
                    opus_manager.overwrite_line(channel, line_offset, first_key, repeat)
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    opus_manager.cursor_select_line(channel, line_offset)
                }
            }
        } else {
            opus_manager.cursor_select_line(channel, line_offset)
        }
    }

    fun cursor_select_line_std(channel: Int, line_offset: Int) {
        this.track(
            TrackedAction.CursorSelectLine,
            listOf(channel, line_offset)
        )

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == null) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_line(channel, line_offset)
        }
    }

    fun cursor_select_line_ctl_line(type: ControlEventType, channel: Int, line_offset: Int) {
        this.track(
            TrackedAction.CursorSelectLineCtlLine,
            listOf(type.i, channel, line_offset)
        )

        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    fun repeat_selection_ctl_line(type: ControlEventType, channel: Int, line_offset: Int, repeat: Int? = null) {
        this.track(
            TrackedAction.RepeatSelectionCtlLine,
            listOf(type.i, channel, line_offset, repeat)
        )

        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val (first, second) = cursor.range!!
        val default_count = ceil((opus_manager.beat_count.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    if (first != second) {
                        opus_manager.controller_line_overwrite_range_horizontally(type, channel, line_offset, first, second, repeat)
                    } else {
                        opus_manager.controller_line_overwrite_line(type, channel, line_offset, first, repeat)
                    }
                }
            }

            CtlLineLevel.Channel -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    if (first != second) {
                        opus_manager.controller_channel_to_line_overwrite_range_horizontally(type, channel, line_offset, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_channel_to_line_overwrite_line(type, channel, line_offset, first.channel, first.beat, repeat)
                    }
                }
            }

            CtlLineLevel.Global -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
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
    }

    fun cursor_select_channel_ctl_line(type: ControlEventType, channel: Int) {
        this.track(
            TrackedAction.CursorSelectChannelCtlLine,
            listOf(type.i, channel)
        )

        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_channel_ctl_line(type, channel)
    }

    fun repeat_selection_ctl_channel(type: ControlEventType, channel: Int, repeat: Int? = null) {
        this.track(
            TrackedAction.RepeatSelectionCtlChannel,
            listOf(type.i, channel, repeat)
        )
        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val (first, second) = cursor.range!!
        val default_count = ceil((opus_manager.beat_count.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        val use_repeat = if (repeat == -1) { default_count } else { repeat }
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    if (first != second) {
                        opus_manager.controller_line_to_channel_overwrite_range_horizontally(type, channel, first, second, repeat)
                    } else {
                        opus_manager.controller_line_to_channel_overwrite_line(type, channel, first, repeat)
                    }
                }
            }
            CtlLineLevel.Channel -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    if (first != second) {
                        opus_manager.controller_channel_overwrite_range_horizontally(type, channel, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_channel_overwrite_line(type, channel, first.channel, first.beat, repeat)
                    }
                }
            }
            CtlLineLevel.Global -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
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
    }

    fun cursor_select_global_ctl_line(type: ControlEventType) {
        this.track(TrackedAction.CursorSelectGlobalCtlLine, listOf(type.i))
        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_global_ctl_line(type)
    }

    fun repeat_selection_ctl_global(type: ControlEventType, repeat: Int? = null) {
        this.track(
            TrackedAction.RepeatSelectionCtlGlobal,
            listOf(type.i, repeat)
        )
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val activity = this.get_activity()

        val (first_key, second_key) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.beat_count.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
        // a value of negative 1 means use default value, where a null would show the dialog
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    if (first_key != second_key) {
                        opus_manager.controller_line_to_global_overwrite_range_horizontally(type, first_key.channel, first_key.line_offset, first_key.beat, second_key.beat, repeat)
                    } else {
                        opus_manager.controller_line_to_global_overwrite_line(type, first_key, repeat)
                    }
                }
            }

            CtlLineLevel.Channel -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    if (first_key != second_key) {
                        opus_manager.controller_channel_to_global_overwrite_range_horizontally(type, first_key.channel, first_key.beat, second_key.beat, repeat)
                    } else {
                        opus_manager.controller_channel_to_global_overwrite_line(type, first_key.channel, first_key.beat, repeat)
                    }
                }
            }

            CtlLineLevel.Global -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, repeat) { repeat: Int ->
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
    }

    fun cursor_select_column(beat: Int) {
        this.track(TrackedAction.CursorSelectColumn, listOf(beat))
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

    /**
     *  wrapper around MainActivity::dialog_number_input
     *  will subvert the popup on replay
     */
    fun dialog_number_input(title: String, min_value: Int, max_value: Int, default: Int? = null, stub_output: Int? = null, callback: (value: Int) -> Unit) {
        if (stub_output != null) {
            callback(stub_output)
        } else {
            val activity = this.get_activity()
            activity.dialog_number_input(title, min_value, max_value, default, callback)
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


    private fun track(token: TrackedAction, args: List<Int?>? = null) {
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
