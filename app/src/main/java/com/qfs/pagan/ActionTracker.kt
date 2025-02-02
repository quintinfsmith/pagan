package com.qfs.pagan

import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResult
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusPanEvent
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
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
        MergeSelectionIntoBeat,
        SetOffset,
        SetOctave,
        TogglePercussion,
        SplitLeaf,
        InsertLeaf,
        RemoveLeaf,
        Unset,
        UnsetRoot,
        SetDuration,
        SetDurationCtl,
        SetChannelInstrument,
        SetPercussionInstrument,
        TogglePercussionVisibility,
        ToggleControllerVisibility,
        RemoveController,
        InsertLine,
        RemoveLine,
        InsertChannel,
        RemoveChannel,
        SetTransitionAtCursor,
        SetVolumeAtCursor,
        SetTempoAtCursor,
        SetPanAtCursor,
        RemoveBeat,
        InsertBeat,
        SetCopyMode
    }

    var activity: MainActivity? = null
    private var ignore_flagged: Boolean = false
    private val action_queue = mutableListOf<Pair<TrackedAction, List<Int?>?>>()
    private var lock: Boolean = false

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

        println("$beat_key, $position")
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
        this.track(TrackedAction.CursorSelectLeafCtlChannel, listOf(type.i, channel, beat) + position)

        this.get_opus_manager().cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    fun cursor_select_ctl_at_global(type: ControlEventType, beat: Int, position: List<Int>) {
        this.track(TrackedAction.CursorSelectLeafCtlGlobal, listOf(type.i, beat) + position)
        this.get_opus_manager().cursor_select_ctl_at_global(type, beat, position)
    }

    fun repeat_selection_std(channel: Int, line_offset: Int, repeat: Int? = null) {
        this.track(TrackedAction.RepeatSelectionStd, listOf(channel, line_offset, repeat))

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
        this.track(TrackedAction.CursorSelectLine, listOf(channel, line_offset))

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == null) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_line(channel, line_offset)
        }
    }

    fun cursor_select_line_ctl_line(type: ControlEventType, channel: Int, line_offset: Int) {
        this.track(TrackedAction.CursorSelectLineCtlLine, listOf(type.i, channel, line_offset))

        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    fun repeat_selection_ctl_line(type: ControlEventType, channel: Int, line_offset: Int, repeat: Int? = null) {
        this.track(TrackedAction.RepeatSelectionCtlLine, listOf(type.i, channel, line_offset, repeat))

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
        this.track(TrackedAction.CursorSelectChannelCtlLine, listOf(type.i, channel))

        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_channel_ctl_line(type, channel)
    }

    fun repeat_selection_ctl_channel(type: ControlEventType, channel: Int, repeat: Int? = null) {
        this.track(TrackedAction.RepeatSelectionCtlChannel, listOf(type.i, channel, repeat))

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
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val activity = this.get_activity()

        val (first_key, second_key) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.beat_count.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
        // a value of negative 1 means use default value, where a null would show the dialog
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
            this.track(TrackedAction.RepeatSelectionCtlGlobal, listOf(type.i, repeat))
            when (cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first_key != second_key) {
                        opus_manager.controller_line_to_global_overwrite_range_horizontally(type, first_key.channel, first_key.line_offset, first_key.beat, second_key.beat, repeat)
                    } else {
                        opus_manager.controller_line_to_global_overwrite_line(type, first_key, repeat)
                    }
                }

                CtlLineLevel.Channel -> {
                    if (first_key != second_key) {
                        opus_manager.controller_channel_to_global_overwrite_range_horizontally(type, first_key.channel, first_key.beat, second_key.beat, repeat)
                    } else {
                        opus_manager.controller_channel_to_global_overwrite_line(type, first_key.channel, first_key.beat, repeat)
                    }
                }

                CtlLineLevel.Global -> {
                    if (first_key != second_key) {
                        opus_manager.controller_global_overwrite_range_horizontally(type, first_key.beat, second_key.beat, repeat)
                    } else {
                        opus_manager.controller_global_overwrite_line(type, first_key.beat, repeat)
                    }
                }

                null -> {
                    opus_manager.cursor_select_global_ctl_line(type)
                }
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
        this.track(TrackedAction.LoadProject, List(bytes.size) { i: Int -> bytes[i].toInt() })

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
        this.track(TrackedAction.PlayEvent, listOf(channel, note, volume.toBits()))
        this.get_activity().play_event(channel, note, volume)
    }

    fun <K: OpusControlEvent> set_ctl_duration(duration: Int? = null) {
        val main = this.get_activity()

        val fragment = main.get_active_fragment()
        if (fragment !is FragmentEditor) {
            return
        }

        val context_menu = fragment.active_context_menu as ContextMenuControlLeaf<K>
        val event = context_menu.get_control_event<K>().copy() as K
        val event_duration = event.duration

        this.dialog_number_input(main.getString(R.string.dlg_duration), 1, 99, event_duration, duration) { value: Int ->
            this.track(TrackedAction.SetDurationCtl, listOf(value))
            event.duration = max(1, value)
            context_menu._widget_callback(event)
        }
    }

    fun set_ctl_transition(transition: ControlTransition? = null) {
        val control_transitions = ControlTransition.values()
        val options = List(control_transitions.size) { i: Int ->
            Pair(control_transitions[i], control_transitions[i].name)
        }

        val main = this.get_activity()

        val fragment = main.get_active_fragment()
        if (fragment !is FragmentEditor) {
            return
        }
        val context_menu = fragment.active_context_menu as ContextMenuControlLeaf<OpusControlEvent>

        val event = context_menu.get_control_event<OpusControlEvent>().copy()
        this.dialog_popup_menu(main.getString(R.string.dialog_transition), options, default = event.transition, transition) { i: Int, transition: ControlTransition ->
            this.track(TrackedAction.SetTransitionAtCursor, listOf(transition.i))
            event.transition = transition
            context_menu.widget.set_event(event)
        }
    }

    fun set_volume(volume: Int? = null) {
        val main = this.get_activity()

        val fragment = main.get_active_fragment()
        if (fragment !is FragmentEditor) {
            return
        }

        val context_menu = fragment.active_context_menu as ContextMenuControlLeaf<OpusVolumeEvent>
        val widget = context_menu.widget as ControlWidgetVolume

        val dlg_default = (widget.get_event().value * widget.max.toFloat()).toInt()
        val dlg_title = main.getString(R.string.dlg_set_volume)
        this.dialog_number_input(dlg_title, widget.min, widget.max, dlg_default, volume) { new_value: Int ->
            this.track(TrackedAction.SetVolumeAtCursor, listOf(new_value))
            val new_event = OpusVolumeEvent(new_value.toFloat() / widget.max.toFloat(), widget.get_event().transition, widget.working_event.duration)
            widget.set_event(new_event)
        }
    }

    fun set_duration(duration: Int? = null) {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        val cursor = opus_manager.cursor
        val (beat_key, position) = opus_manager.get_actual_position(
            cursor.get_beatkey(),
            cursor.get_position()
        )

        val event_duration = opus_manager.get_tree(beat_key, position).get_event()?.duration ?: return

        this.dialog_number_input(main.getString(R.string.dlg_duration), 1, 99, event_duration, duration) { value: Int ->
            this.track(TrackedAction.SetDuration, listOf(value))
            opus_manager.set_duration(beat_key, position, max(1, value))
        }
    }

    fun split(split: Int? = null) {
        this.dialog_number_input(this.get_activity().getString(R.string.dlg_split), 2, 32, stub_output = split) { splits: Int ->
            this.track(TrackedAction.SplitLeaf, listOf(splits))

            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            when (cursor.ctl_level) {
                CtlLineLevel.Global -> opus_manager.controller_global_split_tree(cursor.ctl_type!!, cursor.beat, cursor.get_position(), splits)
                CtlLineLevel.Channel -> opus_manager.controller_channel_split_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position(), splits)
                CtlLineLevel.Line -> opus_manager.controller_line_split_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.get_position(), splits)
                null -> opus_manager.split_tree_at_cursor(splits)
            }
        }
    }

    fun set_offset(new_offset: Int) {
        this.track(TrackedAction.SetOffset, listOf(new_offset))

        val opus_manager = this.get_opus_manager()
        opus_manager.set_note_offset_at_cursor(new_offset)

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()
        val volume = opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        this.get_activity().play_event(beat_key.channel, event_note, volume)
    }

    fun set_octave(new_octave: Int) {
        this.track(TrackedAction.SetOctave, listOf(new_octave))

        val opus_manager = this.get_opus_manager()
        opus_manager.set_note_octave_at_cursor(new_octave)

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()
        val volume = opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        this.get_activity().play_event(beat_key.channel, event_note, volume)
    }

    fun unset() {
        this.track(TrackedAction.Unset)
        val opus_manager = this.get_opus_manager()
        opus_manager.unset()
    }

    fun unset_root() {
        this.track(TrackedAction.UnsetRoot)
        val opus_manager = this.get_activity().get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Global -> {
                opus_manager.cursor_select_ctl_at_global(cursor.ctl_type!!, cursor.beat, listOf())
            }
            CtlLineLevel.Channel -> {
                opus_manager.cursor_select_ctl_at_channel(cursor.ctl_type!!, cursor.channel, cursor.beat, listOf())
            }
            CtlLineLevel.Line -> {
                val beat_key = cursor.get_beatkey()
                opus_manager.cursor_select_ctl_at_line(cursor.ctl_type!!, beat_key, listOf())
            }
            null -> {
                val beat_key = cursor.get_beatkey()
                opus_manager.unset(beat_key, listOf())
            }
        }
    }

    fun remove_at_cursor() {
        this.track(TrackedAction.RemoveLeaf)
        val opus_manager = this.get_activity().get_opus_manager()
        opus_manager.remove_at_cursor(1)
    }

    fun insert_leaf(repeat: Int? = null) {
        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()

        this.dialog_number_input(activity.getString(R.string.dlg_insert), 1, 29, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.InsertLeaf, listOf(count))

            val position = opus_manager.cursor.get_position().toMutableList()
            val cursor = opus_manager.cursor
            if (position.isEmpty()) {
                when (cursor.ctl_level) {
                    CtlLineLevel.Global -> opus_manager.controller_global_split_tree(cursor.ctl_type!!, cursor.beat, position, 2)
                    CtlLineLevel.Channel -> opus_manager.controller_channel_split_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, position, 2)
                    CtlLineLevel.Line -> opus_manager.controller_line_split_tree(cursor.ctl_type!!, cursor.get_beatkey(), position, 2)
                    null -> opus_manager.split_tree_at_cursor(count + 1)
                }
            } else {
                when (cursor.ctl_level) {
                    CtlLineLevel.Global -> opus_manager.controller_global_insert_after(cursor.ctl_type!!, cursor.beat, position)
                    CtlLineLevel.Channel -> opus_manager.controller_channel_insert_after(cursor.ctl_type!!, cursor.channel, cursor.beat, position)
                    CtlLineLevel.Line -> opus_manager.controller_line_insert_after(cursor.ctl_type!!, cursor.get_beatkey(), position)
                    null -> opus_manager.insert_after_cursor(count)
                }
            }
        }
    }

    fun toggle_percussion() {
        this.track(TrackedAction.TogglePercussion)

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position()
        val current_tree_position = opus_manager.get_actual_position(beat_key, position)

        if (opus_manager.get_tree(current_tree_position.first, current_tree_position.second).is_event()) {
            opus_manager.unset()
        } else {
            opus_manager.set_percussion_event_at_cursor()

            val volume = opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
            val event_note = opus_manager.get_percussion_instrument(beat_key.line_offset)
            this.get_activity().play_event(beat_key.channel, event_note, volume)
        }
    }

    fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>? = null) {
        val activity = this.get_activity()
        val sorted_keys = activity._soundfont_supported_instrument_names.keys.toList().sortedBy {
            it.first + (it.second * 128)
        }

        val opus_manager = this.get_opus_manager()
        val is_percussion = opus_manager.is_percussion(channel)
        val default_position = opus_manager.get_channel_instrument(channel)

        val options = mutableListOf<Pair<Pair<Int, Int>, String>>()
        val current_instrument_supported = sorted_keys.contains(default_position)
        for (key in sorted_keys) {
            val name = activity._soundfont_supported_instrument_names[key]
            if (is_percussion && key.first == 128) {
                options.add(Pair(key, "[${key.second}] $name"))
            } else if (key.first != 128 && !is_percussion) {
                val pairstring = "${key.first}/${key.second}"
                options.add(Pair(key, "[$pairstring] $name"))
            }
        }

        if (is_percussion) {
            val use_menu_dialog = options.isNotEmpty() && (!current_instrument_supported || options.size > 1)

            if (use_menu_dialog) {
                this.dialog_popup_menu(activity.getString(R.string.dropdown_choose_instrument), options, default = default_position, stub_output = instrument) { _: Int, instrument: Pair<Int, Int> ->
                    this.track(
                        TrackedAction.SetChannelInstrument,
                        listOf(channel, instrument.first, instrument.second)
                    )
                    opus_manager.channel_set_instrument(channel, instrument)
                }
            } else {
                this.dialog_number_input(activity.getString(R.string.dropdown_choose_instrument), 0, 127, default_position.second, stub_output = instrument?.second) { program: Int ->
                    this.track(
                        TrackedAction.SetChannelInstrument,
                        listOf(channel, instrument?.first ?: 1, program)
                    )
                    opus_manager.channel_set_instrument(channel, Pair(instrument?.first ?: 1, program))
                }
            }
        } else if (options.size > 1 || !current_instrument_supported) {
            this.dialog_popup_menu(activity.getString(R.string.dropdown_choose_instrument), options, default = default_position, stub_output = instrument) { _: Int, instrument: Pair<Int, Int> ->
                this.track(
                    TrackedAction.SetChannelInstrument,
                    listOf(channel, instrument.first, instrument.second)
                )
                opus_manager.channel_set_instrument(channel, instrument)
            }
        }
    }

    fun insert_channel() {
        val opus_manager = this.get_opus_manager()
        val channel = opus_manager.cursor.channel
        if (opus_manager.is_percussion(channel)) {
            opus_manager.new_channel(channel)
        } else {
            opus_manager.new_channel(channel + 1)
        }
    }
    fun remove_channel() {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
            try {
                opus_manager.toggle_channel_visibility(opus_manager.channels.size)
            } catch (e: OpusLayerInterface.HidingLastChannelException) {
                // pass
            }
        } else if (opus_manager.channels.isNotEmpty()) {
            opus_manager.remove_channel(opus_manager.cursor.channel)
        }
    }

    fun insert_line(count: Int? = null) {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        this.dialog_number_input(main.getString(R.string.dlg_insert_lines), 1, 9, stub_output = count) { count: Int ->
            this.track(TrackedAction.InsertLine, listOf(count))
            opus_manager.insert_line_at_cursor(count)
        }
    }

    fun remove_line(count: Int? = null) {
        val main = this.get_activity()
        val opus_manager = main.get_opus_manager()
        val lines = opus_manager.get_all_channels()[opus_manager.cursor.channel].size
        val max_lines = Integer.min(lines - 1, lines - opus_manager.cursor.line_offset)
        this.dialog_number_input(main.getString(R.string.dlg_remove_lines), 1, max_lines, stub_output = count) { count: Int ->
            this.track(TrackedAction.RemoveLine, listOf(count))
            opus_manager.remove_line_at_cursor(count)
        }
    }

    fun set_percussion_instrument(value: Int? = null) {
        val main = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val default_instrument = opus_manager.get_percussion_instrument(cursor.line_offset)

        val options = mutableListOf<Pair<Int, String>>()
        val sorted_keys = main.active_percussion_names.keys.toMutableList()
        sorted_keys.sort()
        for (note in sorted_keys) {
            val name = main.active_percussion_names[note]
            options.add(Pair(note - 27, "${note - 27}: $name"))
        }

        this.dialog_popup_menu(main.getString(R.string.dropdown_choose_percussion), options, default_instrument, stub_output = value) { _: Int, value: Int ->
            this.track(TrackedAction.SetPercussionInstrument, listOf(value))
            opus_manager.set_percussion_instrument(value)
            main.play_event(
                opus_manager.channels.size,
                value,
                .8F
            )
        }
    }

    fun set_pan_at_cursor(value: Int) {
        val main = this.get_activity()
        val fragment = main.get_active_fragment()
        if (fragment !is FragmentEditor) {
            return
        }

        this.track(TrackedAction.SetPanAtCursor, listOf(value))

        val context_menu = fragment.active_context_menu as ContextMenuControlLeaf<OpusPanEvent>
        val widget = context_menu.widget as ControlWidgetPan
        val new_event = widget.working_event.copy()
        new_event.value = (value.toFloat() / widget.max.toFloat()) * -1F
        widget.set_event(new_event)
    }

    fun set_tempo_at_cursor(input_value: Float? = null) {
        val main = this.get_activity()
        val fragment = main.get_active_fragment()
        if (fragment !is FragmentEditor) {
            return
        }

        this.track(TrackedAction.SetTempoAtCursor, listOf(input_value?.toBits()))

        val context_menu = fragment.active_context_menu as ContextMenuControlLeaf<OpusTempoEvent>
        val widget = context_menu.widget as ControlWidgetTempo

        val event = widget.get_event()
        this.dialog_float_input(main.getString(R.string.dlg_set_tempo), widget.min, widget.max, event.value, input_value) { new_value: Float ->
            val new_event = OpusTempoEvent((new_value * 1000F).roundToInt().toFloat() / 1000F)
            widget.set_event(new_event)
        }
    }

    /**
     *  wrapper around MainActivity::dialog_number_input
     *  will subvert the popup on replay
     */
    private fun dialog_number_input(title: String, min_value: Int, max_value: Int, default: Int? = null, stub_output: Int? = null, callback: (value: Int) -> Unit) {
        if (stub_output != null) {
            callback(stub_output)
        } else {
            val activity = this.get_activity()
            activity.dialog_number_input(title, min_value, max_value, default, callback)
        }
    }

    /**
     *  wrapper around MainActivity::dialog_float_input
     *  will subvert the popup on replay
     */
    private fun dialog_float_input(title: String, min_value: Float, max_value: Float, default: Float? = null, stub_output: Float? = null, callback: (value: Float) -> Unit) {
        if (stub_output != null) {
            callback(stub_output)
        } else {
            val activity = this.get_activity()
            activity.dialog_float_input(title, min_value, max_value, default, callback)
        }
    }

    /**
     * wrapper around MainActivity::dialog_popup_menu
     * will subvert popup on replay
     */
    private fun <T> dialog_popup_menu(title: String, options: List<Pair<T, String>>, default: T? = null, stub_output: T? = null, callback: (index: Int, value: T) -> Unit) {
        if (stub_output != null) {
            callback(-1, stub_output)
        } else {
            val activity = this.get_activity()
            activity.dialog_popup_menu(title, options, default, callback)
        }

    }

    fun ignore(): ActionTracker {
        this.ignore_flagged = true
        return this
    }


    private fun track(token: TrackedAction, args: List<Int?>? = null) {
        if (this.ignore_flagged || this.lock) {
            this.ignore_flagged = false
            return
        }

        this.action_queue.add(Pair(token, args))
        Log.d("PaganTracker", "$token")
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun playback() {
        println("PLAYBACK")
        this.lock = true
        for ((action, integers) in this.action_queue) {
            this.process_queued_action(action, integers ?: listOf())
        }
        this.lock = false
    }

    private fun process_queued_action(token: TrackedAction, integers: List<Int?>) {
        when (token) {
            TrackedAction.NewProject -> {
                this.new_project()
            }
            TrackedAction.LoadProject -> {
                val path_bytes = ByteArray(integers.size) { i: Int ->
                     integers[i]!!.toByte()
                }

                this.load_project(path_bytes.decodeToString())
            }
            TrackedAction.CursorSelectColumn -> {
                this.cursor_select_column(integers[0]!!)
            }
            TrackedAction.CursorSelectGlobalCtlRange -> {
                this.cursor_select_global_ctl_range(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!,
                    integers[2]!!
                )
            }
            TrackedAction.CursorSelectChannelCtlRange -> {
                this.cursor_select_channel_ctl_range(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!,
                    integers[2]!!,
                    integers[3]!!
                )
            }
            TrackedAction.CursorSelectLineCtlRange -> {
                this.cursor_select_line_ctl_range(
                    ControlEventType.values()[integers[0]!!],
                    BeatKey(
                        integers[1]!!,
                        integers[2]!!,
                        integers[3]!!
                    ),
                    BeatKey(
                        integers[4]!!,
                        integers[5]!!,
                        integers[6]!!
                    )
                )
            }
            TrackedAction.CursorSelectRange -> {
                this.cursor_select_range(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    ),
                    BeatKey(
                        integers[3]!!,
                        integers[4]!!,
                        integers[5]!!
                    )
                )
            }
            TrackedAction.CursorSelectLeaf -> {
                this.cursor_select(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    ),
                    List(integers.size - 3) { i: Int -> integers[i + 3]!! }
                )
            }
            TrackedAction.CursorSelectLeafCtlLine -> {
                this.cursor_select_ctl_at_line(
                    ControlEventType.values()[integers[0]!!],
                    BeatKey(
                        integers[1]!!,
                        integers[2]!!,
                        integers[3]!!
                    ),
                    List(integers.size - 4) { i: Int -> integers[i + 4]!! }
                )
            }
            TrackedAction.CursorSelectLeafCtlChannel -> {
                this.cursor_select_ctl_at_channel(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!,
                    integers[2]!!,
                    List(integers.size - 3) { i: Int -> integers[i + 3]!! }
                )
            }
            TrackedAction.CursorSelectLeafCtlGlobal -> {
                this.cursor_select_ctl_at_global(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!,
                    List(integers.size - 2) { i: Int -> integers[i + 2]!! }
                )
            }
            TrackedAction.CursorSelectLine -> {
                this.cursor_select_line_std(
                    integers[0]!!,
                    integers[1]!!
                )
            }
            TrackedAction.CursorSelectLineCtlLine -> {
                this.cursor_select_line_ctl_line(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!,
                    integers[2]!!
                )
            }
            TrackedAction.CursorSelectChannelCtlLine -> {
                this.cursor_select_channel_ctl_line(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!
                )
            }
            TrackedAction.CursorSelectGlobalCtlLine -> {
                this.cursor_select_global_ctl_line(
                    ControlEventType.values()[integers[0]!!]
                )
            }
            TrackedAction.PlayEvent -> {
                this.play_event(
                    integers[0]!!,
                    integers[1]!!,
                    Float.fromBits(integers[2]!!)
                )
            }
            TrackedAction.RepeatSelectionStd -> {
                this.repeat_selection_std(
                    integers[0]!!,
                    integers[1]!!,
                    integers[2]
                )
            }
            TrackedAction.RepeatSelectionCtlLine -> {
                this.repeat_selection_ctl_line(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!,
                    integers[2]!!,
                    integers[3]
                )
            }
            TrackedAction.RepeatSelectionCtlChannel -> {
                this.repeat_selection_ctl_channel(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!,
                    integers[2]!!
                )
            }
            TrackedAction.RepeatSelectionCtlGlobal -> {
                this.repeat_selection_ctl_global(
                    ControlEventType.values()[integers[0]!!],
                    integers[1]!!
                )
            }
            TrackedAction.MoveLineCtlToBeat -> {
                this.move_line_ctl_to_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.MoveChannelCtlToBeat -> {
                this.move_channel_ctl_to_beat(
                    integers[0]!!,
                    integers[1]!!
                )
            }
            TrackedAction.MoveGlobalCtlToBeat -> {
                this.move_global_ctl_to_beat(integers[0]!!)
            }
            TrackedAction.MoveSelectionToBeat -> {
                this.move_selection_to_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.CopyLineCtlToBeat -> {
                this.copy_line_ctl_to_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.CopyChannelCtlToBeat -> {
                this.copy_channel_ctl_to_beat(
                    integers[0]!!,
                    integers[1]!!
                )
            }
            TrackedAction.CopyGlobalCtlToBeat -> {
                this.copy_global_ctl_to_beat(integers[0]!!)
            }
            TrackedAction.CopySelectionToBeat -> {
                this.copy_selection_to_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.SetOffset -> {
                this.set_offset(integers[0]!!)
            }
            TrackedAction.SetOctave -> {
                this.set_octave(integers[0]!!)
            }
            TrackedAction.TogglePercussion -> {
                this.toggle_percussion()
            }
            TrackedAction.SplitLeaf -> {
                this.split(integers[0])
            }
            TrackedAction.InsertLeaf -> {
                this.insert_leaf(integers[0])
            }
            TrackedAction.RemoveLeaf -> {
                this.remove_at_cursor()
            }
            TrackedAction.Unset -> {
                this.unset()
            }
            TrackedAction.SetDuration -> {
                this.set_duration(integers[0])
            }
            TrackedAction.SetDurationCtl -> {
                this.set_ctl_duration<OpusControlEvent>(integers[0])
            }
            TrackedAction.SetPercussionInstrument -> {
                this.set_percussion_instrument(integers[0]!!)
            }
            TrackedAction.UnsetRoot -> {
                this.unset_root()
            }
            TrackedAction.InsertLine -> {
                this.insert_line(integers[0]!!)
            }
            TrackedAction.RemoveLine -> {
                this.remove_line(integers[0]!!)
            }
            TrackedAction.SetTransitionAtCursor -> {
                this.set_ctl_transition(ControlTransition.values()[integers[0]!!])
            }
            TrackedAction.SetVolumeAtCursor -> {
                this.set_volume(integers[0]!!)
            }
            TrackedAction.SetTempoAtCursor -> {
                this.set_tempo_at_cursor(
                    Float.fromBits(integers[0]!!)
                )
            }
            TrackedAction.SetChannelInstrument -> TODO()
            TrackedAction.TogglePercussionVisibility -> TODO()
            TrackedAction.ToggleControllerVisibility -> TODO()
            TrackedAction.RemoveController -> TODO()
            TrackedAction.InsertChannel -> TODO()
            TrackedAction.RemoveChannel -> TODO()
            TrackedAction.SetPanAtCursor -> TODO()
            TrackedAction.RemoveBeat -> TODO()
            TrackedAction.InsertBeat -> TODO()
            TrackedAction.MergeSelectionIntoBeat -> TODO()
            TrackedAction.SetCopyMode -> TODO()
        }
    }
}
