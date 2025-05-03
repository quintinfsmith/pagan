package com.qfs.pagan

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.clearFragmentResult
import androidx.fragment.app.setFragmentResult
import androidx.navigation.findNavController
import com.qfs.json.JSONBoolean
import com.qfs.json.JSONFloat
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONObject
import com.qfs.json.JSONString
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.ControlTransition
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusTempoEvent
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager


/**
 * Handle all the logic between a user action and the OpusManager.
 * This class is meant for recording and playing back UI tests and eventually debugging so
 * not every action directed through here at the moment.
 */
class ActionTracker {
    var DEBUG_ON = false
    class NoActivityException: Exception()
    enum class TrackedAction {
        ApplyUndo,
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
        SaveProject,
        DeleteProject,
        CopyProject,
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
        ShowLineController,
        ShowChannelController,
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
        SetCopyMode,
        DrawerOpen,
        DrawerClose,
        OpenSettings,
        OpenAbout,
        GoBack,
        SetSampleRate,
        DisableSoundFont,
        SetSoundFont,
        SetProjectName,
        SetClipNotes,
        SetTuningTable,
        ImportSong,
        ImportSoundFont,
        DeleteSoundFont,
        SetRelativeModeVisibility,
        SetRelativeMode,
        SwapLines,
        SwapChannels,
        MuteChannel,
        UnMuteChannel,
        MuteLine,
        UnMuteLine,
        ForceOrientation,
        AllowMidiPlayback
    }

    companion object {
        fun from_json_entry(entry: JSONList): Pair<TrackedAction, List<Int?>> {
            val s = entry.get_string(0)
            val token = TrackedAction.valueOf(s)
            return Pair(
                token,
                when (token) {
                    // STRING
                    TrackedAction.DeleteSoundFont,
                    TrackedAction.ImportSoundFont,
                    TrackedAction.SetSoundFont,
                    TrackedAction.SetTransitionAtCursor,
                    TrackedAction.ImportSong,
                    TrackedAction.ShowLineController,
                    TrackedAction.ShowChannelController,
                    TrackedAction.SetCopyMode,
                    TrackedAction.SetProjectName,
                    TrackedAction.LoadProject -> {
                        val string = entry.get_string(1)
                        ActionTracker.string_to_ints(string)
                    }

                    // Boolean
                    TrackedAction.AllowMidiPlayback,
                    TrackedAction.GoBack,
                    TrackedAction.SetClipNotes,
                    TrackedAction.SetRelativeModeVisibility -> {
                        if (entry.size == 1) {
                            listOf()
                        } else {
                            val value = entry.get_booleann(1)
                            listOf(
                                if (value == null) {
                                    null
                                } else if (value) {
                                    1
                                } else {
                                    0
                                }
                            )
                        }
                    }
                    TrackedAction.SetTempoAtCursor -> {
                        listOf(entry.get_float(1).toBits())
                    }
                    TrackedAction.RepeatSelectionCtlLine,
                    TrackedAction.RepeatSelectionCtlChannel,
                    TrackedAction.RepeatSelectionCtlGlobal,
                    TrackedAction.CursorSelectLeafCtlChannel,
                    TrackedAction.CursorSelectLeafCtlGlobal,
                    TrackedAction.CursorSelectGlobalCtlLine,
                    TrackedAction.CursorSelectGlobalCtlRange,
                    TrackedAction.CursorSelectChannelCtlLine,
                    TrackedAction.CursorSelectChannelCtlRange,
                    TrackedAction.CursorSelectLineCtlLine,
                    TrackedAction.CursorSelectLineCtlRange,
                    TrackedAction.CursorSelectLeafCtlLine -> {
                        val name = entry.get_string(1)
                        val string_ints = string_to_ints(name)
                        listOf(string_ints.size) + string_ints + List(entry.size - 2) { i: Int ->
                            entry.get_intn(i + 2)
                        }
                    }

                    else -> {
                        if (entry.size == 1) {
                            listOf()
                        } else {
                            List(entry.size - 1) { i: Int ->
                                entry.get_intn(i + 1)
                            }
                        }
                    } // PASS
                }
            )
        }

        fun string_from_ints(integers: List<Int?>): String {
            val path_bytes = ByteArray(integers.size) { i: Int ->
                integers[i]!!.toByte()
            }
            return path_bytes.decodeToString()
        }

        fun string_to_ints(string: String): List<Int?> {
            val bytes = string.toByteArray()
            return List(bytes.size) {
                bytes[it].toInt()
            }
        }

        fun <E : Enum<E>> enum_to_ints(enum_value: Enum<E>): List<Int?> {
            val initial = string_to_ints(enum_value.name)
            return listOf(initial.size) + initial
        }

        fun sized_string_from_ints(int_list: List<Int?>, first_index: Int = 0): String {
            val name = ByteArray(int_list[first_index]!!) { i: Int ->
                int_list[i + first_index + 1]!!.toByte()
            }.decodeToString()
            return name
        }

        fun type_from_ints(int_list: List<Int?>, first_index: Int = 0): ControlEventType {
            val name = ByteArray(int_list[first_index]!!) { i: Int ->
                int_list[i + first_index + 1]!!.toByte()
            }.decodeToString()
            return ControlEventType.valueOf(name)
        }

        fun transition_from_ints(int_list: List<Int?>, first_index: Int = 0): ControlTransition {
            val name = ByteArray(int_list[first_index]!!) { i: Int ->
                int_list[i + first_index + 1]!!.toByte()
            }.decodeToString()
            return ControlTransition.valueOf(name)
        }

        fun item_to_json(item: Pair<TrackedAction, List<Int?>?>): JSONList {
            val (token, integers) = item

            val var_args = if (!integers.isNullOrEmpty()) {
                when (token) {
                    // STRING
                    TrackedAction.DeleteSoundFont,
                    TrackedAction.ImportSoundFont,
                    TrackedAction.SetSoundFont,
                    TrackedAction.SetTransitionAtCursor,
                    TrackedAction.ShowLineController,
                    TrackedAction.ShowChannelController,
                    TrackedAction.SetCopyMode,
                    TrackedAction.ImportSong,
                    TrackedAction.SetProjectName,
                    TrackedAction.LoadProject -> {
                        arrayOf(JSONString(ActionTracker.string_from_ints(integers)))
                    }
                    // Boolean
                    TrackedAction.AllowMidiPlayback,
                    TrackedAction.GoBack,
                    TrackedAction.SetClipNotes,
                    TrackedAction.SetRelativeModeVisibility -> {
                        arrayOf(JSONBoolean(integers[0] != 0))
                    }

                    TrackedAction.SetTempoAtCursor -> {
                        arrayOf(JSONFloat(Float.fromBits(integers[0]!!)))
                    }

                    TrackedAction.ShowLineController,
                    TrackedAction.ShowChannelController -> {
                        arrayOf(JSONString(string_from_ints(integers)))
                    }

                    TrackedAction.RepeatSelectionCtlLine,
                    TrackedAction.RepeatSelectionCtlChannel,
                    TrackedAction.RepeatSelectionCtlGlobal,
                    TrackedAction.CursorSelectLeafCtlChannel,
                    TrackedAction.CursorSelectLeafCtlGlobal,
                    TrackedAction.CursorSelectGlobalCtlLine,
                    TrackedAction.CursorSelectGlobalCtlRange,
                    TrackedAction.CursorSelectChannelCtlLine,
                    TrackedAction.CursorSelectChannelCtlRange,
                    TrackedAction.CursorSelectLineCtlLine,
                    TrackedAction.CursorSelectLineCtlRange,
                    TrackedAction.CursorSelectLeafCtlLine -> {
                        val str_len = integers[0]!!
                        Array(integers.size - str_len) { i: Int ->
                            if (i == 0) {
                                JSONString(ActionTracker.sized_string_from_ints(integers))
                            } else {
                                JSONInteger(integers[i + str_len]!!)
                            }
                        }
                    }


                    else -> {
                        Array(integers.size) {
                            JSONInteger(integers[it]!!)
                        }
                    }
                }
            } else {
                arrayOf()
            }

            return JSONList(JSONString(token.name), *var_args)
        }
    }

    var activity: MainActivity? = null
    private var ignore_flagged: Boolean = false
    private val action_queue = mutableListOf<Pair<TrackedAction, List<Int?>?>>()
    var lock: Boolean = false

    fun attach_activity(activity: MainActivity) {
        this.activity = activity
        this.DEBUG_ON = activity.is_debug_on()

    }

    fun get_activity(): MainActivity {
        return this.activity ?: throw NoActivityException()
    }


    // Track is called in the callback functions of onDrawerOpen and onDrawerClosed
    fun drawer_close() {
        val drawer_layout = this.get_activity().findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawers()
        }
    }

    // Track is called in the callback functions of onDrawerOpen and onDrawerClosed
    fun drawer_open() {
        val drawer_layout = this.get_activity().findViewById<DrawerLayout>(R.id.drawer_layout)
        if (!drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    fun apply_undo() {
        this.track(TrackedAction.ApplyUndo)
        this.get_opus_manager().apply_undo()
    }

    fun allow_midi_playback(value: Boolean) {
        this.track(TrackedAction.AllowMidiPlayback, listOf(if (value) 1 else 0))
        val activity = this.get_activity()
        activity.configuration.allow_midi_playback = value
        if (value) {
            activity.enable_physical_midi_output()
        } else {
            activity.block_physical_midi_output()
        }
        activity.save_configuration()
    }

    fun go_back(do_save: Boolean? = null) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val navController = activity.findNavController(R.id.nav_host_fragment_content_main)
        if (navController.currentDestination?.id == R.id.EditorFragment) {
            if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Unset) {
                this.track(TrackedAction.GoBack)
                opus_manager.cursor_clear()
            } else {
                this.dialog_save_project(do_save) { saved: Boolean ->
                    this.track(TrackedAction.GoBack, listOf(if (saved) 1 else 0))
                    activity.finish()
                }
            }
        } else {
            this.track(TrackedAction.GoBack)
            navController.popBackStack()
        }
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

    fun save() {
        this.track(TrackedAction.SaveProject)
        this.get_activity().project_save()
    }

    fun delete() {
        this.track(TrackedAction.DeleteProject)
        val activity = this.get_activity()
        activity.project_delete()
        this.ignore().drawer_close()
    }

    fun project_copy() {
        this.track(TrackedAction.CopyProject)
        val activity = this.get_activity()
        activity.project_move_to_copy()
        this.ignore().drawer_close()
    }

    fun swap_lines(from_channel: Int, from_line: Int, to_channel: Int, to_line: Int) {
        this.track(TrackedAction.SwapLines, listOf(from_channel, from_line, to_channel, to_line))
        val opus_manager = this.get_opus_manager()
        try {
            opus_manager.swap_lines(
                from_channel,
                from_line,
                to_channel,
                to_line
            )
        } catch (e: OpusLayerBase.IncompatibleChannelException) {
            val activity = this.get_activity()
            activity.feedback_msg(activity.getString(R.string.std_percussion_swap))
        }
    }

    fun swap_channels(from_channel: Int, to_channel: Int) {
        this.track(TrackedAction.SwapChannels, listOf(from_channel, to_channel))
        val opus_manager = this.get_opus_manager()
        try {
            opus_manager.swap_channels(from_channel, to_channel)
        } catch (e: OpusLayerBase.IncompatibleChannelException) {
            val activity = this.get_activity()
            activity.feedback_msg(activity.getString(R.string.can_t_move_percussion_channel))
        }
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeaf,
            beat_key.toList() + position
        )

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select(beat_key, position)

        val tree = opus_manager.get_tree()
        thread {
            if (tree.is_event()) {
                val note = if (opus_manager.is_percussion(beat_key.channel)) {
                    opus_manager.get_percussion_instrument(beat_key.line_offset)
                } else {
                    opus_manager.get_absolute_value(beat_key, position) ?: return@thread
                }
                if (note >= 0) {
                    this.get_activity().play_event(
                        beat_key.channel,
                        note
                    )
                }
            }
        }
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
                ActionTracker.enum_to_ints(type) + listOf(beat_key.channel, beat_key.line_offset, beat_key.beat) + position
        )

        this.get_opus_manager().cursor_select_ctl_at_line(type, beat_key, position)
    }

    fun cursor_select_ctl_at_channel(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlChannel,
            ActionTracker.enum_to_ints(type) + listOf(channel, beat) + position
        )

        this.get_opus_manager().cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    fun cursor_select_ctl_at_global(type: ControlEventType, beat: Int, position: List<Int>) {
        this.track(TrackedAction.CursorSelectLeafCtlGlobal, enum_to_ints(type) + listOf(beat) + position)
        this.get_opus_manager().cursor_select_ctl_at_global(type, beat, position)
    }

    fun repeat_selection_std(channel: Int, line_offset: Int, repeat: Int? = null) {

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val context = this.get_activity()

        val (first_key, second_key) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.length.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()

        // a value of negative 1 means use default value, where a null would show the dialog
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        if (first_key != second_key) {
            this.dialog_number_input(context.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                this.track(TrackedAction.RepeatSelectionStd, listOf(channel, line_offset, repeat))
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
                this.track(TrackedAction.RepeatSelectionStd, listOf(channel, line_offset, repeat))
                try {
                    opus_manager.overwrite_line(channel, line_offset, first_key, repeat)
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    opus_manager.cursor_select_line(channel, line_offset)
                }
            }
        } else {
            this.track(TrackedAction.RepeatSelectionStd, listOf(channel, line_offset, repeat))
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
        this.track(TrackedAction.CursorSelectLineCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset))

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == CtlLineLevel.Line) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
        }
    }

    fun repeat_selection_ctl_line(type: ControlEventType, channel: Int, line_offset: Int, repeat: Int? = null) {

        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val (first, second) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.length.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset, repeat))
                    if (first != second) {
                        opus_manager.controller_line_overwrite_range_horizontally(type, channel, line_offset, first, second, repeat)
                    } else {
                        opus_manager.controller_line_overwrite_line(type, channel, line_offset, first, repeat)
                    }
                }
            }

            CtlLineLevel.Channel -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset, repeat))
                    if (first != second) {
                        opus_manager.controller_channel_to_line_overwrite_range_horizontally(type, channel, line_offset, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_channel_to_line_overwrite_line(type, channel, line_offset, first.channel, first.beat, repeat)
                    }
                }
            }

            CtlLineLevel.Global -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset, repeat))
                    if (first != second) {
                        opus_manager.controller_global_to_line_overwrite_range_horizontally(type, channel, line_offset, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_global_to_line_overwrite_line(type, first.beat, channel, line_offset, repeat)
                    }
                }
            }
            null -> {
                this.track(TrackedAction.RepeatSelectionCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset, repeat))
                opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
            }
        }
    }

    fun cursor_select_channel_ctl_line(type: ControlEventType, channel: Int) {
        this.track(TrackedAction.CursorSelectChannelCtlLine, enum_to_ints(type) + listOf(channel))

        val opus_manager = this.get_opus_manager()

        val cursor = opus_manager.cursor
        if (cursor.mode == OpusManagerCursor.CursorMode.Line && cursor.channel == channel && cursor.ctl_level == CtlLineLevel.Channel) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_channel_ctl_line(type, channel)
        }
    }

    fun repeat_selection_ctl_channel(type: ControlEventType, channel: Int, repeat: Int? = null) {

        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val (first, second) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.length.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        val use_repeat = if (repeat == -1) { default_count } else { repeat }
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlChannel, enum_to_ints(type) + listOf(channel, repeat))
                    if (first != second) {
                        opus_manager.controller_line_to_channel_overwrite_range_horizontally(type, channel, first, second, repeat)
                    } else {
                        opus_manager.controller_line_to_channel_overwrite_line(type, channel, first, repeat)
                    }
                }
            }
            CtlLineLevel.Channel -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlChannel, enum_to_ints(type) + listOf(channel, repeat))
                    if (first != second) {
                        opus_manager.controller_channel_overwrite_range_horizontally(type, channel, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_channel_overwrite_line(type, channel, first.channel, first.beat, repeat)
                    }
                }
            }
            CtlLineLevel.Global -> {
                this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlChannel, enum_to_ints(type) + listOf(channel, repeat))
                    if (first != second) {
                        opus_manager.controller_global_to_channel_overwrite_range_horizontally(type, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_global_to_channel_overwrite_line(type, channel, first.beat, repeat)
                    }
                }
            }
            null -> {
                this.track(TrackedAction.RepeatSelectionCtlChannel, enum_to_ints(type) + listOf(channel, repeat))
                opus_manager.cursor_select_channel_ctl_line(type, channel)
            }
        }
    }

    fun cursor_select_global_ctl_line(type: ControlEventType) {
        this.track(TrackedAction.CursorSelectGlobalCtlLine, enum_to_ints(type))
        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_global_ctl_line(type)
    }

    fun repeat_selection_ctl_global(type: ControlEventType, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val activity = this.get_activity()

        val (first_key, second_key) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.length.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
        // a value of negative 1 means use default value, where a null would show the dialog
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        this.dialog_number_input(activity.getString(R.string.repeat_selection), 1, 999, default_count, use_repeat) { repeat: Int ->
            this.track(TrackedAction.RepeatSelectionCtlGlobal, enum_to_ints(type) + listOf(repeat))
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
            enum_to_ints(type) +
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
        opus_manager.cursor_select_line_ctl_range(type, first_key, second_key)
    }

    fun cursor_select_channel_ctl_range(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectChannelCtlRange, enum_to_ints(type) + listOf(first_beat, second_beat))

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select_channel_ctl_range(type, channel, first_beat, second_beat)
    }


    fun cursor_select_global_ctl_range(type: ControlEventType, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectGlobalCtlRange, enum_to_ints(type) + listOf(first_beat, second_beat))

        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        opus_manager.cursor_select_global_ctl_range(type, first_beat, second_beat)
    }

    fun set_sample_rate(new_rate: Int) {
        this.track(TrackedAction.SetSampleRate, listOf(new_rate))
        this.get_activity().set_sample_rate(new_rate)
    }

    fun open_settings() {
        this.track(TrackedAction.OpenSettings)
        this.get_activity().navigate(R.id.SettingsFragment)
    }

    fun open_about() {
        this.track(TrackedAction.OpenAbout)
        this.get_activity().navigate(R.id.LicenseFragment)
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
        this.track(TrackedAction.LoadProject, ActionTracker.string_to_ints(path))

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
            this.track(TrackedAction.SetTransitionAtCursor, ActionTracker.string_to_ints(transition.name))
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

        val context_menu = fragment.active_context_menu
        if (context_menu !is ContextMenuWithController<*>) {
            return
        }

        val widget: ControlWidgetVolume = context_menu.get_widget() as ControlWidgetVolume

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

    fun show_hidden_line_controller(forced_value: ControlEventType? = null) {
        val opus_manager = this.get_opus_manager()
        val options = mutableListOf<Pair<ControlEventType, String>>( )
        val cursor = opus_manager.cursor

        for (ctl_type in OpusLayerInterface.line_controller_domain) {
            if (opus_manager.is_line_ctl_visible(ctl_type, cursor.channel, cursor.line_offset)) {
                continue
            }

            options.add(Pair(ctl_type, ctl_type.name))
        }

        this.dialog_popup_menu(this.get_activity().getString(R.string.show_line_controls), options, stub_output = forced_value) { index: Int, ctl_type: ControlEventType ->
            this.track(TrackedAction.ShowLineController, string_to_ints(ctl_type.name))
            opus_manager.toggle_line_controller_visibility(ctl_type, cursor.channel, cursor.line_offset)
        }
    }

    fun show_hidden_channel_controller(forced_value: ControlEventType? =  null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val options = mutableListOf<Pair<ControlEventType, String>>( )

        for (ctl_type in OpusLayerInterface.channel_controller_domain) {
            if (opus_manager.is_channel_ctl_visible(ctl_type, cursor.channel)) {
                continue
            }

            options.add(Pair(ctl_type, ctl_type.name))
        }

        this.dialog_popup_menu(this.get_activity().getString(R.string.show_channel_controls), options, stub_output = forced_value) { index: Int, ctl_type: ControlEventType ->
            this.track(TrackedAction.ShowChannelController, string_to_ints(ctl_type.name))
            opus_manager.toggle_channel_controller_visibility(ctl_type, cursor.channel)
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
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        this.get_activity().play_event(beat_key.channel, event_note)
    }

    fun set_octave(new_octave: Int) {
        this.track(TrackedAction.SetOctave, listOf(new_octave))

        val opus_manager = this.get_opus_manager()
        opus_manager.set_note_octave_at_cursor(new_octave)

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        this.get_activity().play_event(beat_key.channel, event_note)
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
            val event_note = opus_manager.get_percussion_instrument(beat_key.line_offset)
            this.get_activity().play_event(beat_key.channel, event_note)
        }
    }

    fun toggle_percussion_visibility() {
        this.track(TrackedAction.TogglePercussionVisibility)

        val opus_manager = this.get_opus_manager()
        try {
            if (!opus_manager.percussion_channel.visible || opus_manager.channels.isNotEmpty()) {
                opus_manager.toggle_channel_visibility(opus_manager.channels.size)
            } else {
                return
            }
        } catch (e: OpusLayerInterface.HidingNonEmptyPercussionException) {
            return
        } catch (e: OpusLayerInterface.HidingLastChannelException) {
            return
        }

    }

    fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>? = null) {
        val activity = this.get_activity()
        val supported_instrument_names = activity.get_supported_instrument_names()
        val sorted_keys = supported_instrument_names.keys.toList().sortedBy {
            it.first + (it.second * 128)
        }

        val opus_manager = this.get_opus_manager()
        val is_percussion = opus_manager.is_percussion(channel)
        val default_position = opus_manager.get_channel_instrument(channel)

        val options = mutableListOf<Pair<Pair<Int, Int>, String>>()
        val current_instrument_supported = sorted_keys.contains(default_position)
        for (key in sorted_keys) {
            val name = supported_instrument_names[key]
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

    fun insert_channel(index: Int? = null) {
        this.track(TrackedAction.InsertChannel, listOf(index ?: -1))
        val opus_manager = this.get_opus_manager()
        if (index != null) {
            opus_manager.new_channel(index)
        } else {
            val channel = opus_manager.cursor.channel
            if (opus_manager.is_percussion(channel)) {
                opus_manager.new_channel(channel)
            } else {
                opus_manager.new_channel(channel + 1)
            }
        }
    }

    fun remove_channel(index: Int? = null) {
        this.track(TrackedAction.RemoveChannel, listOf(index ?: - 1))

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
            main.play_event(opus_manager.channels.size, value)
        }
    }

    fun set_pan_at_cursor(value: Int) {
        val main = this.get_activity()
        val fragment = main.get_active_fragment()
        if (fragment !is FragmentEditor) {
            return
        }

        this.track(TrackedAction.SetPanAtCursor, listOf(value))
        val context_menu = fragment.active_context_menu
        if (context_menu !is ContextMenuWithController<*>) {
            return
        }

        val widget = context_menu.get_widget() as ControlWidgetPan
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


        val context_menu = fragment.active_context_menu
        if (context_menu !is ContextMenuWithController<*>) {
            return
        }
        val widget = context_menu.get_widget() as ControlWidgetTempo

        val event = widget.get_event()
        this.dialog_float_input(main.getString(R.string.dlg_set_tempo), widget.min, widget.max, event.value, input_value) { new_value: Float ->
            this.track(TrackedAction.SetTempoAtCursor, listOf(new_value.toBits()))
            val new_event = OpusTempoEvent((new_value * 1000F).roundToInt().toFloat() / 1000F)
            widget.set_event(new_event)
        }
    }

    fun insert_beat_after_cursor(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(this.get_activity().getString(R.string.dlg_insert_beats), 1, 4096, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.InsertBeat, listOf(count))
            opus_manager.insert_beat_after_cursor(count)
        }
    }

    fun remove_beat_at_cursor(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(this.get_activity().getString(R.string.dlg_remove_beats), 1, opus_manager.length - 1, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.RemoveBeat, listOf(count))
            opus_manager.remove_beat_at_cursor(count)
        }
    }

    fun disable_soundfont() {
        this.track(ActionTracker.TrackedAction.DisableSoundFont)
        val activity = this.get_activity()
        val btnChooseSoundFont = activity.findViewById<TextView>(R.id.btnChooseSoundFont)
        btnChooseSoundFont.text = activity.getString(R.string.no_soundfont)
        activity.disable_soundfont()
        activity.save_configuration()
    }

    fun set_relative_mode_visibility(enabled: Boolean) {
        this.track(TrackedAction.SetRelativeModeVisibility, listOf(if (enabled) 1 else 0))
        val activity = this.get_activity()
        activity.configuration.relative_mode = enabled
        activity.save_configuration()
    }

    fun set_relative_mode(mode: Int) {
        this.track(TrackedAction.SetRelativeMode, listOf(mode))

        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()

        val current_tree_position = opus_manager.get_actual_position(
            opus_manager.cursor.get_beatkey(),
            opus_manager.cursor.get_position()
        )
        val current_tree = opus_manager.get_tree(
            current_tree_position.first,
            current_tree_position.second
        )

        val event = current_tree.get_event()
        if (event == null) {
            opus_manager.set_relative_mode(mode, false)
            return
        }
        val radix = opus_manager.tuning_map.size

        val new_value = when (event) {
            is RelativeNoteEvent -> {
                when (mode) {
                    /* Abs */
                    0 -> {
                        try {
                            opus_manager.convert_event_to_absolute()
                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            (new_event as AbsoluteNoteEvent).note
                        } catch (e: OpusLayerBase.NoteOutOfRange) {
                            opus_manager.set_event_at_cursor(
                                AbsoluteNoteEvent(0, event.duration)
                            )
                            0
                        }
                    }
                    /* + */
                    1 -> {
                        if (event.offset < 0) {
                            val new_event = RelativeNoteEvent(0 - event.offset, event.duration)
                            opus_manager.set_event_at_cursor(new_event)
                        }
                        abs(event.offset)
                    }
                    /* - */
                    2 -> {
                        if (event.offset > 0) {
                            val new_event = RelativeNoteEvent(0 - event.offset, event.duration)
                            opus_manager.set_event_at_cursor(new_event)
                        }
                        abs(event.offset)
                    }

                    else -> null
                }
            }
            is AbsoluteNoteEvent -> {
                when (mode) {
                    /* Abs */
                    0 -> {
                        event.note
                    }
                    /* + */
                    1 -> {
                        val cursor = opus_manager.cursor
                        val value = opus_manager.get_relative_value(cursor.get_beatkey(), cursor.get_position())
                        if (value >= 0) {
                            opus_manager.convert_event_to_relative()

                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            abs((new_event as RelativeNoteEvent).offset)
                        } else {
                            opus_manager.relative_mode = 1
                            null
                        }

                    }
                    /* - */
                    2 -> {
                        val cursor = opus_manager.cursor
                        val value = opus_manager.get_relative_value(cursor.get_beatkey(), cursor.get_position())
                        if (value <= 0) {
                            opus_manager.convert_event_to_relative()

                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            abs((new_event as RelativeNoteEvent).offset)
                        } else {
                            opus_manager.relative_mode = 2
                            null
                        }
                    }

                    else -> null
                }
            }

            else -> null
        }

        val nsOctave: NumberSelector = activity.findViewById(R.id.nsOctave)
        val nsOffset: NumberSelector = activity.findViewById(R.id.nsOffset)
        if (new_value == null) {
            nsOctave.unset_active_button()
            nsOffset.unset_active_button()
        } else {
            nsOctave.setState(new_value / radix, manual = true, surpress_callback = true)
            nsOffset.setState(new_value % radix, manual = true, surpress_callback = true)
        }

        opus_manager.relative_mode = mode
    }

    fun delete_soundfont(input_filename: String? = null) {
        val activity = this.get_activity()
        val soundfont_dir = activity.get_soundfont_directory()
        val file_list = soundfont_dir.listFiles()?.toList() ?: listOf<File>()

        val soundfonts = mutableListOf<Pair<String, String>>( )

        for (file in file_list) {
            soundfonts.add(Pair(file.name, file.name))
        }
        if (soundfonts.isEmpty()) {
            return
        }

        this.dialog_popup_menu(activity.getString(R.string.dialog_remove_soundfont_title), soundfonts, stub_output = input_filename) { _: Int, filename: String ->
            this.dialog_confirm(activity.getString(R.string.dialog_remove_soundfont_text, filename), input_filename != null) {
                this.track(TrackedAction.DeleteSoundFont, ActionTracker.string_to_ints(filename))
                if (activity.configuration.soundfont != null && activity.configuration.soundfont!! == filename) {
                    activity.get_action_interface().ignore().disable_soundfont()
                }

                val file = File("${soundfont_dir.absolutePath}/${filename}")
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    fun import_soundfont(uri: Uri? = null) {
        // TODO: Track action
        if (uri == null) {
            val intent = Intent()
                .setType("*/*")
                .setAction(Intent.ACTION_GET_CONTENT)
            this.get_activity()._import_soundfont_intent_listener.launch(intent)
        } else {
            TODO("would only be used for debug atm anyway.")
        }
    }

    fun set_soundfont(filename: String) {
        val activity = this.get_activity()

        val path = "${activity.getExternalFilesDir(null)}/SoundFonts/$filename"
        if (!File(path).isFile) {
            return // TODO: Maybe throw exception?
        }

        this.track(TrackedAction.SetSoundFont, ActionTracker.string_to_ints(filename))

        val btnChooseSoundFont = activity.findViewById<TextView>(R.id.btnChooseSoundFont)
        thread {
            activity.loading_reticle_show(activity.getString(R.string.loading_new_soundfont))
            activity.set_soundfont(filename)

            // Check that it set
            if (filename == activity.configuration.soundfont) {
                if (btnChooseSoundFont != null) {
                    btnChooseSoundFont.text = filename
                }
                activity.save_configuration()
            }
            activity.loading_reticle_hide()
        }
    }


    fun set_project_name(project_name: String? = null) {
        val activity = this.get_activity()
        this.dialog_string_popup(activity.getString(R.string.dlg_change_name), this.get_opus_manager().project_name, project_name) { string: String ->
            this.track(
                TrackedAction.SetProjectName,
                ActionTracker.string_to_ints(string)
            )
            val opus_manager = this.get_opus_manager()
            opus_manager.set_project_name(string)
        }
    }

    /**
     * Wrapper around MainActivity::dialog_confirm
     * Will skip check on replay
     */
    private fun dialog_confirm(title: String, skip: Boolean, callback: () -> Unit) {
        if (skip) {
            callback()
        } else {
            this.get_activity().dialog_confirm(title, callback)
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

    private fun dialog_save_project(stub_output: Boolean? = null, callback: (Boolean) -> Unit) {
        if (stub_output == null) {
            this.get_activity().dialog_save_project(callback)
        } else {
            if (stub_output) {
                this.get_activity().project_save()
            }
            callback(stub_output)
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

    private fun dialog_string_popup(title: String, default: String? = null, stub_output: String? = null, callback: (String) -> Unit) {
        val activity = this.get_activity()
        if (stub_output != null) {
            callback(stub_output)
        } else {
            activity.dialog_string_popup(title, default, callback)
        }
    }

    fun ignore(): ActionTracker {
        this.ignore_flagged = true
        return this
    }

    fun track(token: TrackedAction, args: List<Int?>? = null) {
        if (!this.DEBUG_ON || this.ignore_flagged || this.lock) {
            this.ignore_flagged = false
            return
        }

        Log.d("PaganTracker", "Tracked $token")
        this.action_queue.add(Pair(token, args))
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun playback() {
        this.lock = true
        for ((action, integers) in this.action_queue) {
            this.process_queued_action(action, integers ?: listOf())
        }
        this.lock = false
    }

    fun process_queued_action(token: TrackedAction, integers: List<Int?>) {
        when (token) {
            TrackedAction.ApplyUndo -> {
                this.apply_undo()
            }
            TrackedAction.NewProject -> {
                this.new_project()
            }
            TrackedAction.LoadProject -> {
                this.load_project(string_from_ints(integers))
            }
            TrackedAction.CursorSelectColumn -> {
                this.cursor_select_column(integers[0]!!)
            }
            TrackedAction.CursorSelectGlobalCtlRange -> {
                this.cursor_select_global_ctl_range(
                    type_from_ints(integers),
                    integers[1]!!,
                    integers[2]!!
                )
            }
            TrackedAction.CursorSelectChannelCtlRange -> {
                val offset = integers[0]!!
                this.cursor_select_channel_ctl_range(
                    type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!,
                    integers[3 + offset]!!
                )
            }
            TrackedAction.CursorSelectLineCtlRange -> {
                val offset = integers[0]!!
                this.cursor_select_line_ctl_range(
                    type_from_ints(integers),
                    BeatKey(
                        integers[1 + offset]!!,
                        integers[2 + offset]!!,
                        integers[3 + offset]!!
                    ),
                    BeatKey(
                        integers[4 + offset]!!,
                        integers[5 + offset]!!,
                        integers[6 + offset]!!
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
                val offset = integers[0]!!
                this.cursor_select_ctl_at_line(
                    type_from_ints(integers),
                    BeatKey(
                        integers[1 + offset]!!,
                        integers[2 + offset]!!,
                        integers[3 + offset]!!
                    ),
                    List(integers.size - 4 - offset) { i: Int -> integers[i + 4 + offset]!! }
                )
            }
            TrackedAction.CursorSelectLeafCtlChannel -> {
                val offset = integers[0]!!
                this.cursor_select_ctl_at_channel(
                    type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!,
                    List(integers.size - 3 - offset) { i: Int -> integers[i + 3 + offset]!! }
                )
            }
            TrackedAction.CursorSelectLeafCtlGlobal -> {
                val offset = integers[0]!!
                this.cursor_select_ctl_at_global(
                    type_from_ints(integers),
                    integers[1 + offset]!!,
                    List(integers.size - 2 - offset) { i: Int -> integers[i + 2 + offset]!! }
                )
            }
            TrackedAction.CursorSelectLine -> {
                this.cursor_select_line_std(
                    integers[0]!!,
                    integers[1]!!
                )
            }
            TrackedAction.CursorSelectLineCtlLine -> {
                val offset = integers[0]!!
                this.cursor_select_line_ctl_line(
                    type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!
                )
            }
            TrackedAction.CursorSelectChannelCtlLine -> {
                val offset = integers[0]!!
                this.cursor_select_channel_ctl_line(
                    type_from_ints(integers),
                    integers[1 + offset]!!
                )
            }
            TrackedAction.CursorSelectGlobalCtlLine -> {
                this.cursor_select_global_ctl_line(type_from_ints(integers))
            }
            TrackedAction.RepeatSelectionStd -> {
                this.repeat_selection_std(
                    integers[0]!!,
                    integers[1]!!,
                    integers[2]
                )
            }
            TrackedAction.RepeatSelectionCtlLine -> {
                val offset = integers[0]!!
                this.repeat_selection_ctl_line(
                    type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!,
                    integers[3 + offset]
                )
            }
            TrackedAction.RepeatSelectionCtlChannel -> {
                val offset = integers[0]!!
                this.repeat_selection_ctl_channel(
                    type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!
                )
            }
            TrackedAction.RepeatSelectionCtlGlobal -> {
                val offset = integers[0]!!
                this.repeat_selection_ctl_global(
                    type_from_ints(integers),
                    integers[1 + offset]!!
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
                this.set_percussion_instrument(integers[0])
            }
            TrackedAction.UnsetRoot -> {
                this.unset_root()
            }
            TrackedAction.InsertLine -> {
                this.insert_line(integers[0])
            }
            TrackedAction.RemoveLine -> {
                this.remove_line(integers[0])
            }
            TrackedAction.SetTransitionAtCursor -> {
                this.set_ctl_transition(ControlTransition.valueOf(string_from_ints(integers)))
            }
            TrackedAction.SetVolumeAtCursor -> {
                this.set_volume(integers[0])
            }
            TrackedAction.SetTempoAtCursor -> {
                this.set_tempo_at_cursor(
                    Float.fromBits(integers[0]!!)
                )
            }
            TrackedAction.SetChannelInstrument -> {
                this.set_channel_instrument(integers[0]!!, Pair(integers[1]!!, integers[2]!!))
            }
            TrackedAction.RemoveBeat -> {
                this.remove_beat_at_cursor(integers[0])
            }
            TrackedAction.InsertBeat -> {
                this.insert_beat_after_cursor(integers[0]!!)
            }
            TrackedAction.SetPanAtCursor -> {
                this.set_pan_at_cursor(integers[0]!!)
            }
            TrackedAction.ToggleControllerVisibility -> {
                this.toggle_controller_visibility()
            }
            TrackedAction.RemoveController -> {
                this.remove_controller()
            }
            TrackedAction.InsertChannel -> {
                // -1 means insert channel at cursor
                val index = integers[0]!!
                if (index == -1) {
                    this.insert_channel(null)
                } else {
                    this.insert_channel(index)
                }
            }
            TrackedAction.RemoveChannel -> {
                // -1 means remove channel at cursor
                val index = integers[0]!!
                if (index == -1) {
                    this.remove_channel(null)
                } else {
                    this.remove_channel(index)
                }
            }
            TrackedAction.TogglePercussionVisibility -> {
                this.toggle_percussion_visibility()
            }
            TrackedAction.DrawerOpen -> {
                this.drawer_open()
            }
            TrackedAction.DrawerClose -> {
                this.drawer_close()
            }
            TrackedAction.MergeSelectionIntoBeat -> {
                this.merge_selection_into_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.SetCopyMode -> {
                this.set_copy_mode(
                    PaganConfiguration.MoveMode.valueOf(
                        string_from_ints(integers)
                    )
                )
            }
            TrackedAction.OpenSettings -> {
                this.open_settings()
            }
            TrackedAction.OpenAbout -> {
                this.open_about()
            }
            TrackedAction.GoBack -> {
                if (integers.isEmpty()) {
                    this.go_back()
                } else {
                    this.go_back(integers[0] != 0)
                }
            }

            TrackedAction.SetSampleRate -> {
                this.set_sample_rate(integers[0]!!)
            }
            TrackedAction.DisableSoundFont -> {
                this.disable_soundfont()
            }
            TrackedAction.SetSoundFont -> {
                this.set_soundfont(string_from_ints(integers))
            }

            TrackedAction.SetProjectName -> {
                this.set_project_name(string_from_ints(integers))
            }

            TrackedAction.ShowLineController -> {
                this.show_hidden_line_controller(
                    ControlEventType.valueOf(string_from_ints(integers))
                )
            }
            TrackedAction.ShowChannelController -> {
                this.show_hidden_channel_controller(
                    ControlEventType.valueOf(string_from_ints(integers))
                )
            }
            TrackedAction.SaveProject -> {
                this.save()
            }
            TrackedAction.DeleteProject -> {
                this.delete()
            }
            TrackedAction.CopyProject -> {
                this.project_copy()
            }

            TrackedAction.SetClipNotes -> {
                this.set_clip_same_line_notes(integers[0] != 0)
            }

            TrackedAction.SetTuningTable -> {
                this.set_tuning_table_and_transpose(
                    Array<Pair<Int, Int>>((integers.size - 2) / 2) { i: Int ->
                        Pair(integers[i * 2]!!, integers[(i * 2) + 1]!!)
                    },
                    Pair(
                        integers[integers.size - 2]!!,
                        integers[integers.size - 1]!!
                    )
                )
            }

            TrackedAction.ImportSong -> {
                val uri_string = string_from_ints(integers)
                val uri = Uri.parse(uri_string)
                this.import(uri)
            }

            TrackedAction.ImportSoundFont -> {
                this.import_soundfont(Uri.parse(string_from_ints(integers)))
            }

            TrackedAction.DeleteSoundFont -> {
                this.delete_soundfont(string_from_ints(integers))
            }

            TrackedAction.SetRelativeModeVisibility -> {
                this.set_relative_mode_visibility(integers[0] != 0)
            }
            TrackedAction.SetRelativeMode -> {
                this.set_relative_mode(integers[0]!!)
            }

            TrackedAction.SwapLines -> {
                this.swap_lines(integers[0]!!, integers[1]!!, integers[2]!!, integers[3]!!)
            }
            TrackedAction.SwapChannels -> {
                this.swap_channels(integers[0]!!, integers[1]!!)
            }

            TrackedAction.MuteChannel -> {
                this.channel_mute(integers[0]!!)
            }
            TrackedAction.UnMuteChannel -> {
                this.channel_unmute(integers[0]!!)
            }
            TrackedAction.MuteLine -> {
                this.line_mute(integers[0]!!, integers[1]!!)
            }
            TrackedAction.UnMuteLine -> {
                this.line_unmute(integers[0]!!, integers[1]!!)
            }
            TrackedAction.ForceOrientation -> {
                this.set_orientation(integers[0]!!)
            }
            TrackedAction.AllowMidiPlayback -> {
                this.allow_midi_playback(integers[0] != 0)
            }
        }
    }

    fun set_copy_mode(mode: PaganConfiguration.MoveMode) {
        val main = this.get_activity()
        val opus_manager = this.get_opus_manager()

        main.configuration.move_mode = mode
        main.save_configuration()

        val fragment = main.get_active_fragment()
        if (fragment !is FragmentEditor) {
            return
        }
        val context_menu = fragment.active_context_menu
        if (context_menu !is ContextMenuRange) {
            return
        }

        this.track(TrackedAction.SetCopyMode, ActionTracker.string_to_ints(mode.name))

        context_menu.label.text = when (mode) {
            PaganConfiguration.MoveMode.MOVE -> {
                if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                    main.resources.getString(R.string.label_move_range)
                } else {
                    main.resources.getString(R.string.label_move_beat)
                }
            }
            PaganConfiguration.MoveMode.COPY -> {
                if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                    main.resources.getString(R.string.label_copy_range)
                } else {
                    main.resources.getString(R.string.label_copy_beat)
                }
            }
            PaganConfiguration.MoveMode.MERGE -> {
                if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Range) {
                    main.resources.getString(R.string.label_merge_range)
                } else {
                    main.resources.getString(R.string.label_merge_beat)
                }
            }
        }
    }

    fun remove_controller() {
        this.track(TrackedAction.RemoveController)

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                opus_manager.remove_line_controller(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset
                )
            }

            CtlLineLevel.Channel -> {
                opus_manager.remove_channel_controller(
                    cursor.ctl_type!!,
                    cursor.channel
                )
            }

            CtlLineLevel.Global,
            null -> {} // pass
        }
    }

    fun toggle_controller_visibility() {
        this.track(TrackedAction.ToggleControllerVisibility)

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                opus_manager.toggle_line_controller_visibility(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset
                )
            }

            CtlLineLevel.Channel -> {
                opus_manager.toggle_channel_controller_visibility(
                    cursor.ctl_type!!,
                    cursor.channel
                )
            }

            CtlLineLevel.Global,
            null -> {} // Pass
        }
    }

    fun channel_mute(channel: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel

        this.track(TrackedAction.MuteChannel, listOf(w_channel))
        opus_manager.mute_channel(w_channel)
    }

    fun channel_unmute(channel: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel

        this.track(TrackedAction.UnMuteChannel, listOf(w_channel))
        opus_manager.unmute_channel(w_channel)
    }

    fun line_mute(channel: Int? = null, line_offset: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel
        val w_line_offset = line_offset ?: cursor.line_offset

        this.track(TrackedAction.MuteLine, listOf(w_channel, w_line_offset))
        opus_manager.mute_line(w_channel, w_line_offset)
    }

    fun line_unmute(channel: Int? = null, line_offset: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel
        val w_line_offset = line_offset ?: cursor.line_offset

        this.track(TrackedAction.UnMuteLine, listOf(w_channel, w_line_offset))
        opus_manager.unmute_line(w_channel, w_line_offset)
    }

    fun set_orientation(value: Int) {
        this.track(TrackedAction.ForceOrientation, listOf(value))
        this.get_activity().set_forced_orientation(value)
    }

    fun set_clip_same_line_notes(value: Boolean) {
        this.track(TrackedAction.SetClipNotes, listOf(if (value) 1 else 0))
        val activity = this.get_activity()
        activity.configuration.clip_same_line_release = value
        activity.save_configuration()
    }

    private fun dialog_tuning_table() {
        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val main_fragment = activity.get_active_fragment() ?: return

        val viewInflated: View = LayoutInflater.from(main_fragment.context)
            .inflate(
                R.layout.dialog_tuning_map,
                main_fragment.view as ViewGroup,
                false
            )

        val etRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etRadix)
        val etTranspose = viewInflated.findViewById<RangedIntegerInput>(R.id.etTranspose)
        etTranspose.set_range(0, 99999999)
        etTranspose.set_value(opus_manager.transpose.first)

        val etTransposeRadix = viewInflated.findViewById<RangedIntegerInput>(R.id.etTransposeRadix)
        etTransposeRadix.set_range(1, 99999999)
        etTransposeRadix.set_value(opus_manager.transpose.second)

        val rvTuningMap = viewInflated.findViewById<TuningMapRecycler>(R.id.rvTuningMap)
        rvTuningMap.adapter = TuningMapRecyclerAdapter(opus_manager.tuning_map.clone())


        val dialog = AlertDialog.Builder(main_fragment.context, R.style.AlertDialog)
            .setCustomTitle(activity._build_dialog_title_view(
                activity.resources.getString(R.string.dlg_tuning)
            ))
            .setView(viewInflated)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val tuning_map = (rvTuningMap.adapter as TuningMapRecyclerAdapter).tuning_map
                val transpose = Pair(etTranspose.get_value() ?: 0, etTransposeRadix.get_value() ?: tuning_map.size)
                this._track_tuning_map_and_transpose(tuning_map, transpose)
                opus_manager.set_tuning_map_and_transpose(tuning_map, transpose)
                dialog.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()

        activity._adjust_dialog_colors(dialog)


        val default_value = opus_manager.tuning_map.size

        etRadix.set_value(default_value)
        etRadix.set_range(2, 36)
        etRadix.value_set_callback = { new_radix: Int? ->
            rvTuningMap.reset_tuning_map(new_radix)
        }
    }

    private fun _track_tuning_map_and_transpose(tuning_map: Array<Pair<Int, Int>>, transpose: Pair<Int, Int>) {
        this.track(
            TrackedAction.SetTuningTable,
            List(tuning_map.size * 2) { i: Int ->
                if (i % 2 == 0) {
                    tuning_map[i / 2].first
                } else {
                    tuning_map[i / 2].second
                }
            }
            + listOf(transpose.first, transpose.second)
        )
    }

    fun set_tuning_table_and_transpose(tuning_map: Array<Pair<Int, Int>>? = null, transpose: Pair<Int, Int>? = null) {
        if (tuning_map == null || transpose == null) {
            this.dialog_tuning_table()
        } else {
            val opus_manager = this.get_opus_manager()
            this._track_tuning_map_and_transpose(tuning_map, transpose)
            opus_manager.set_tuning_map_and_transpose(tuning_map, transpose)
        }
    }

    fun import(uri: Uri? = null) {
        // TODO: Track action
        val activity = this.get_activity()
        if (uri == null) {
            val intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
            activity.general_import_intent_launcher.launch(intent)
        } else {
            // TODO: Right now this still needs to be manually handled during playback. Not sure if its even possible to automate
            val intent = Intent()
            intent.setAction(Intent.ACTION_GET_CONTENT)
            intent.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
            activity.general_import_intent_launcher.launch(intent)
        }
    }

    fun to_json(): JSONObject {
        return JSONList(
            *Array(this.action_queue.size) { i: Int ->
                ActionTracker.item_to_json(this.action_queue[i])
            }
        )
    }

    fun from_json(json_list: JSONList) {
        this.action_queue.clear()
        for (i in 0 until json_list.size) {
            val entry = json_list.get_listn(i) ?: continue
            val (token, ints) = ActionTracker.from_json_entry(entry)
            this.track(token, ints)
        }
    }


    private fun _gen_string_list(int_list: List<Int>): String {
        return int_list.joinToString(", ", "listOf(", ")")
    }

    fun clear() {
        this.action_queue.clear()
    }
}
