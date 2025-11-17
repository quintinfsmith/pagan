package com.qfs.pagan

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.qfs.json.JSONBoolean
import com.qfs.json.JSONFloat
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONObject
import com.qfs.json.JSONString
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.cxtmenu.IntegerInput
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.IncompatibleChannelException
import com.qfs.pagan.structure.opusmanager.base.InvalidOverwriteCall
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.NoteOutOfRange
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import kotlin.math.abs
import kotlin.math.ceil
import com.qfs.pagan.OpusLayerInterface as OpusManager


/**
 * Handle all (or as much of as possible) of the logic between a user action and the OpusManager.
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
        ToggleControllerVisibility,
        ShowLineController,
        ShowChannelController,
        ShowGlobalController,
        RemoveController,
        InsertLine,
        RemoveLine,
        InsertChannel,
        RemoveChannel,
        MoveChannel,
        SetTransitionAtCursor,
        SetVolumeAtCursor,
        SetVelocityAtCursor,
        SetTempoAtCursor,
        SetPanAtCursor,
        SetDelayAtCursor,
        RemoveBeat,
        InsertBeat,
        InsertBeatAt,
        SetCopyMode,
        DrawerOpen,
        DrawerClose,
        OpenSettings,
        OpenAbout,
        GoBack,
        SetProjectNameAndNotes,
        SetTuningTable,
        ImportSong,
        SetRelativeMode,
        SwapLines,
        MuteChannel,
        UnMuteChannel,
        MuteLine,
        UnMuteLine,
        AdjustSelection,
        TagColumn,
        UntagColumn,
        MoveLine
    }

    companion object {
        fun from_json_entry(entry: JSONList): Pair<TrackedAction, List<Int?>> {
            val s = entry.get_string(0)
            val token = TrackedAction.valueOf(s)
            return Pair(
                token,
                when (token) {
                    TrackedAction.SetProjectNameAndNotes -> {
                        val name = entry.get_string(1)
                        val notes = entry.get_string(2)
                        val name_ints = this.string_to_ints(name)
                        listOf(name_ints.size) + name_ints + this.string_to_ints(notes)
                    }
                    TrackedAction.SetDelayAtCursor -> {
                        listOf(
                            entry.get_int(1),
                            entry.get_int(2),
                            entry.get_float(3).toBits(),
                            entry.get_int(4)
                        )
                    }
                    // STRING
                    TrackedAction.SetTransitionAtCursor,
                    TrackedAction.ImportSong,
                    TrackedAction.ShowLineController,
                    TrackedAction.ShowChannelController,
                    TrackedAction.ShowGlobalController,
                    TrackedAction.SetCopyMode,
                    TrackedAction.LoadProject -> {
                        val string = entry.get_string(1)
                        this.string_to_ints(string)
                    }

                    // Boolean
                    TrackedAction.GoBack -> {
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
                    TrackedAction.UntagColumn -> {
                        listOf(entry.get_int(1))
                    }
                    TrackedAction.TagColumn -> {
                        val title = entry.get_stringn(2)
                        listOf(entry.get_int(1)) + if (title == null) {
                            listOf()
                        } else {
                            this.string_to_ints(title)
                        }
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
                        val string_ints = this.string_to_ints(name)
                        listOf(string_ints.size) + string_ints + List(entry.size - 2) { i: Int ->
                            entry.get_intn(i + 2)
                        }
                    }

                    TrackedAction.MoveChannel -> {
                        listOf(
                            entry.get_int(1),
                            entry.get_int(2),
                            if (entry.get_boolean(3)) 1 else 0
                        )
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
            val initial = this.string_to_ints(enum_value.name)
            return listOf(initial.size) + initial
        }

        fun sized_string_from_ints(int_list: List<Int?>, first_index: Int = 0): String {
            val name = ByteArray(int_list[first_index]!!) { i: Int ->
                int_list[i + first_index + 1]!!.toByte()
            }.decodeToString()
            return name
        }

        fun type_from_ints(int_list: List<Int?>, first_index: Int = 0): EffectType {
            val name = ByteArray(int_list[first_index]!!) { i: Int ->
                int_list[i + first_index + 1]!!.toByte()
            }.decodeToString()
            return EffectType.valueOf(name)
        }

        fun transition_from_ints(int_list: List<Int?>, first_index: Int = 0): EffectTransition {
            val name = ByteArray(int_list[first_index]!!) { i: Int ->
                int_list[i + first_index + 1]!!.toByte()
            }.decodeToString()
            return EffectTransition.valueOf(name)
        }

        fun item_to_json(item: Pair<TrackedAction, List<Int?>?>): JSONList {
            val (token, integers) = item

            val var_args = if (!integers.isNullOrEmpty()) {
                when (token) {
                    TrackedAction.SetProjectNameAndNotes -> {
                        val name_length = integers[0]!!
                        val name = this.string_from_ints(integers.subList(1, name_length + 1))
                        val notes = this.string_from_ints(integers.subList(name_length + 1, integers.size))
                        arrayOf(
                            JSONString(name),
                            JSONString(notes)
                        )
                    }

                    TrackedAction.SetDelayAtCursor -> {
                        arrayOf(
                            JSONInteger(integers[0]!!),
                            JSONInteger(integers[1]!!),
                            JSONFloat(Float.fromBits(integers[2]!!)),
                            JSONInteger(integers[3]!!)
                        )
                    }

                    // STRING
                    TrackedAction.SetTransitionAtCursor,
                    TrackedAction.ShowLineController,
                    TrackedAction.ShowChannelController,
                    TrackedAction.ShowGlobalController,
                    TrackedAction.SetCopyMode,
                    TrackedAction.ImportSong,
                    TrackedAction.LoadProject -> {
                        arrayOf(JSONString(this.string_from_ints(integers)))
                    }
                    // Boolean
                    TrackedAction.GoBack -> {
                        arrayOf(JSONBoolean(integers[0] != 0))
                    }

                    TrackedAction.SetTempoAtCursor -> {
                        arrayOf(JSONFloat(Float.fromBits(integers[0]!!)))
                    }

                    TrackedAction.TagColumn -> {
                        arrayOf(
                            JSONInteger(integers[0]!!),
                            if (integers.size > 1) {
                                JSONString(this.string_from_ints(integers.subList(1, integers.size)))
                            } else {
                                null
                            }
                        )
                    }

                    TrackedAction.UntagColumn -> {
                        arrayOf(JSONInteger(integers[0]!!))
                    }
                    TrackedAction.MoveChannel -> {
                        arrayOf(JSONInteger(integers[0]!!), JSONInteger(integers[1]!!), JSONBoolean(integers[2] != 0))
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
                                JSONString(this.sized_string_from_ints(integers))
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

    var activity: ComponentActivityEditor? = null
    var opus_manager: OpusManager? = null

    private var ignore_flagged: Boolean = false
    private val action_queue = mutableListOf<Pair<TrackedAction, List<Int?>?>>()
    var lock: Boolean = false

    fun attach_opus_manager(opus_manager: OpusManager) {
        this.opus_manager = opus_manager
    }
    fun detach_opus_manager() {
        this.opus_manager = null
    }
    fun detach_activity() {
        this.activity = null
    }

    fun attach_activity(activity: ComponentActivityEditor) {
        this.activity = activity
        //this.DEBUG_ON = activity.is_debug_on()
    }

    fun get_activity(): ComponentActivityEditor {
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

    fun go_back(do_save: Boolean? = null) {
        TODO()
        //val activity = this.get_activity()
        //val opus_manager = activity.get_opus_manager()
        //val navController = activity.findNavController(R.id.nav_host_fragment_content_main)
        //if (navController.currentDestination?.id == R.id.EditorFragment) {
        //    if (opus_manager.cursor.mode != OpusManagerCursor.CursorMode.Unset) {
        //        this.track(TrackedAction.GoBack)
        //        opus_manager.cursor_clear()
        //    } else {
        //        this.dialog_save_project(do_save) { saved: Boolean ->
        //            this.track(TrackedAction.GoBack, listOf(if (saved) 1 else 0))
        //            activity.finish()
        //        }
        //    }
        //} else {
        //    this.track(TrackedAction.GoBack)
        //    navController.popBackStack()
        //}
    }

    fun move_selection_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MoveSelectionToBeat,
            beat_key.to_list()
        )
        this.opus_manager?.move_to_beat(beat_key)
    }

    fun copy_selection_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.CopySelectionToBeat,
            beat_key.to_list()
        )
        this.opus_manager?.copy_to_beat(beat_key)
    }

    fun merge_selection_into_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MergeSelectionIntoBeat,
            beat_key.to_list()
        )
        this.opus_manager?.merge_into_beat(beat_key)
    }

    fun save() {
        this.track(TrackedAction.SaveProject)
        this.get_activity().project_save()
    }

    fun delete() {
        // TODO
       // this.track(TrackedAction.DeleteProject)
       // val activity = this.get_activity()
       // activity.project_delete()
       // this.ignore().drawer_close()
    }

    fun project_copy() {
        // this.track(TrackedAction.CopyProject)
        // val activity = this.get_activity()
        // activity.project_move_to_copy()
        // this.ignore().drawer_close()
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
        } catch (_: IncompatibleChannelException) {
            this.activity?.let {
                Toast.makeText(it, it.getString(R.string.std_percussion_swap), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cursor_clear() {
        // TODO Track
        //this.track(TrackedAction.CursorClear)
        this.opus_manager?.cursor_clear()
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeaf,
            beat_key.to_list() + position
        )

        this.opus_manager?.cursor_select(beat_key, position) ?: return

        // TODO()
        // val tree = opus_manager.get_tree()
        // thread {
        //     if (tree.has_event()) {
        //         val note = if (opus_manager.is_percussion(beat_key.channel)) {
        //             opus_manager.get_percussion_instrument(beat_key.channel, beat_key.line_offset)
        //         } else {
        //             opus_manager.get_absolute_value(beat_key, position) ?: return@thread
        //         }
        //         if (note >= 0) {
        //             this.get_activity().play_event(
        //                 beat_key.channel,
        //                 note
        //             )
        //         }
        //     }
        // }
    }

    fun move_line_ctl_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MoveLineCtlToBeat,
            beat_key.to_list()
        )
        this.get_opus_manager().move_line_ctl_to_beat(beat_key)
    }

    fun copy_line_ctl_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.CopyLineCtlToBeat,
            beat_key.to_list()
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

    fun cursor_select_ctl_at_line(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlLine,
                ActionTracker.enum_to_ints(type) + listOf(beat_key.channel, beat_key.line_offset, beat_key.beat) + position
        )

        this.get_opus_manager().cursor_select_ctl_at_line(type, beat_key, position)
    }

    fun cursor_select_ctl_at_channel(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeafCtlChannel,
            ActionTracker.enum_to_ints(type) + listOf(channel, beat) + position
        )

        this.get_opus_manager().cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    fun cursor_select_ctl_at_global(type: EffectType, beat: Int, position: List<Int>) {
        this.track(TrackedAction.CursorSelectLeafCtlGlobal, ActionTracker.enum_to_ints(type) + listOf(beat) + position)
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
            this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
                this.track(TrackedAction.RepeatSelectionStd, listOf(channel, line_offset, repeat))
                try {
                    opus_manager.overwrite_beat_range_horizontally(
                        channel,
                        line_offset,
                        first_key,
                        second_key,
                        repeat
                    )
                } catch (_: MixedInstrumentException) {
                    opus_manager.cursor_select_line(channel, line_offset)
                } catch (_: InvalidOverwriteCall) {
                    opus_manager.cursor_select_line(channel, line_offset)
                }
            }
        } else if (opus_manager.is_percussion(first_key.channel) == opus_manager.is_percussion(channel)) {
            this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
                this.track(TrackedAction.RepeatSelectionStd, listOf(channel, line_offset, repeat))
                try {
                    opus_manager.overwrite_line(channel, line_offset, first_key, repeat)
                } catch (_: InvalidOverwriteCall) {
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

        if (cursor.mode == CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == null) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_line(channel, line_offset)
        }
    }

    fun cursor_select_line_ctl_line(type: EffectType, channel: Int, line_offset: Int) {
        this.track(TrackedAction.CursorSelectLineCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset))

        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.mode == CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == CtlLineLevel.Line && type == cursor.ctl_type) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
        }
    }

    fun repeat_selection_ctl_line(type: EffectType, channel: Int, line_offset: Int, repeat: Int? = null) {

        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val (first, second) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.length.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset, repeat))
                    if (first != second) {
                        opus_manager.controller_line_overwrite_range_horizontally(type, channel, line_offset, first, second, repeat)
                    } else {
                        opus_manager.controller_line_overwrite_line(type, channel, line_offset, first, repeat)
                    }
                }
            }

            CtlLineLevel.Channel -> {
                this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel, line_offset, repeat))
                    if (first != second) {
                        opus_manager.controller_channel_to_line_overwrite_range_horizontally(type, channel, line_offset, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_channel_to_line_overwrite_line(type, channel, line_offset, first.channel, first.beat, repeat)
                    }
                }
            }

            CtlLineLevel.Global -> {
                this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
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

    fun cursor_select_channel_ctl_line(type: EffectType, channel: Int) {
        this.track(TrackedAction.CursorSelectChannelCtlLine, ActionTracker.enum_to_ints(type) + listOf(channel))

        val opus_manager = this.get_opus_manager()

        val cursor = opus_manager.cursor
        if (cursor.mode == CursorMode.Line && cursor.channel == channel && cursor.ctl_level == CtlLineLevel.Channel && type == cursor.ctl_type) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_channel_ctl_line(type, channel)
        }
    }

    fun repeat_selection_ctl_channel(type: EffectType, channel: Int, repeat: Int? = null) {

        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val (first, second) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.length.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        val use_repeat = if (repeat == -1) { default_count } else { repeat }
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlChannel, ActionTracker.enum_to_ints(type) + listOf(channel, repeat))
                    if (first != second) {
                        opus_manager.controller_line_to_channel_overwrite_range_horizontally(type, channel, first, second, repeat)
                    } else {
                        opus_manager.controller_line_to_channel_overwrite_line(type, channel, first, repeat)
                    }
                }
            }
            CtlLineLevel.Channel -> {
                this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlChannel, ActionTracker.enum_to_ints(type) + listOf(channel, repeat))
                    if (first != second) {
                        opus_manager.controller_channel_overwrite_range_horizontally(type, channel, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_channel_overwrite_line(type, channel, first.channel, first.beat, repeat)
                    }
                }
            }
            CtlLineLevel.Global -> {
                this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
                    this.track(TrackedAction.RepeatSelectionCtlChannel, ActionTracker.enum_to_ints(type) + listOf(channel, repeat))
                    if (first != second) {
                        opus_manager.controller_global_to_channel_overwrite_range_horizontally(type, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_global_to_channel_overwrite_line(type, channel, first.beat, repeat)
                    }
                }
            }
            null -> {
                this.track(TrackedAction.RepeatSelectionCtlChannel, ActionTracker.enum_to_ints(type) + listOf(channel, repeat))
                opus_manager.cursor_select_channel_ctl_line(type, channel)
            }
        }
    }

    fun cursor_select_global_ctl_line(type: EffectType) {
        this.track(TrackedAction.CursorSelectGlobalCtlLine, ActionTracker.enum_to_ints(type))
        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_global_ctl_line(type)
    }

    fun repeat_selection_ctl_global(type: EffectType, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val activity = this.get_activity()

        val (first_key, second_key) = cursor.get_ordered_range()!!
        val default_count = ceil((opus_manager.length.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
        // a value of negative 1 means use default value, where a null would show the dialog
        val use_repeat = if (repeat == -1) { default_count } else { repeat }

        this.dialog_number_input(R.string.repeat_selection, 1, 999, default_count, use_repeat) { repeat: Int ->
            this.track(TrackedAction.RepeatSelectionCtlGlobal, ActionTracker.enum_to_ints(type) + listOf(repeat))
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

    fun cursor_select_range_next(beat_key: BeatKey) {
        // TODO: Track
        this.opus_manager?.cursor_select_range_next(beat_key)
    }
    fun cursor_select_line_ctl_range_next(type: EffectType, beat_key: BeatKey) {
        // TODO: Track
        this.opus_manager?.cursor_select_line_ctl_range_next(type, beat_key)
    }
    fun cursor_select_channel_ctl_range_next(type: EffectType, channel: Int, beat: Int) {
        // TODO: Track
        this.opus_manager?.cursor_select_channel_ctl_range_next(type, channel, beat)
    }
    fun cursor_select_global_ctl_range_next(type: EffectType, beat: Int) {
        // TODO: Track
        this.opus_manager?.cursor_select_global_ctl_range_next(type, beat)
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

        this.opus_manager?.cursor_select_range(first_key, second_key)
    }

    fun cursor_select_line_ctl_range(type: EffectType, first_key: BeatKey, second_key: BeatKey) {
        this.track(
            TrackedAction.CursorSelectLineCtlRange,
            ActionTracker.enum_to_ints(type) +
            listOf(
                first_key.channel,
                first_key.line_offset,
                first_key.beat,
                second_key.channel,
                second_key.line_offset,
                second_key.beat
            )
        )

        this.opus_manager?.cursor_select_line_ctl_range(type, first_key, second_key)
    }

    fun cursor_select_channel_ctl_range(type: EffectType, channel: Int, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectChannelCtlRange, ActionTracker.enum_to_ints(type) + listOf(first_beat, second_beat))
        this.opus_manager?.cursor_select_channel_ctl_range(type, channel, first_beat, second_beat)
    }


    fun cursor_select_global_ctl_range(type: EffectType, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectGlobalCtlRange, ActionTracker.enum_to_ints(type) + listOf(first_beat, second_beat))
        this.opus_manager?.cursor_select_global_ctl_range(type, first_beat, second_beat)
    }

    fun open_settings() {
        this.track(TrackedAction.OpenSettings)
        this.get_activity().open_settings()
    }

    fun open_about() {
        this.track(TrackedAction.OpenAbout)
        this.get_activity().open_about()
    }


    fun new_project() {
        this.track(TrackedAction.NewProject)
        val activity = this.get_activity()
        activity.setup_new()
    }

    fun load_project(uri: Uri) {
        this.track(TrackedAction.LoadProject, ActionTracker.string_to_ints(uri.toString()))
        val activity = this.get_activity()
        activity.load_project(uri)
    }


    fun <K: EffectEvent> set_ctl_duration(duration: Int? = null) {
        TODO()
        // val main = this.get_activity()
        // val context_menu = main.active_context_menu as ContextMenuControlLeaf<K>
        // val event = context_menu.get_control_event<K>().copy() as K
        // val event_duration = event.duration

        // this.dialog_number_input(main.getString(R.string.dlg_duration), 1, 99, event_duration, duration) { value: Int ->
        //     this.track(TrackedAction.SetDurationCtl, listOf(value))
        //     event.duration = max(1, value)
        //     context_menu._widget_callback(event)
        // }
    }

    fun set_ctl_transition(transition: EffectTransition? = null) {
        TODO()
        // val main = this.get_activity()
        // val context_menu = main.active_context_menu as ContextMenuControlLeaf<EffectEvent>
        // val event = context_menu.get_control_event<EffectEvent>().copy()


        // val filter = when (event.event_type) {
        //     EffectType.Tempo -> listOf(EffectTransition.Instant, EffectTransition.RInstant)
        //     else -> listOf(
        //         EffectTransition.Instant,
        //         EffectTransition.Linear,
        //         EffectTransition.RInstant,
        //         EffectTransition.RLinear
        //     )
        // }

        // val options = listOf(
        //     Triple(
        //         EffectTransition.Instant,
        //         main.get_effect_transition_icon(EffectTransition.Instant),
        //         main.getString(R.string.effect_transition_instant)
        //     ),
        //     Triple(
        //         EffectTransition.Linear,
        //         main.get_effect_transition_icon(EffectTransition.Linear),
        //         main.getString(R.string.effect_transition_linear)
        //     ),
        //     Triple(
        //         EffectTransition.RInstant,
        //         main.get_effect_transition_icon(EffectTransition.RInstant),
        //         main.getString(R.string.effect_transition_rinstant)
        //     ),
        //     Triple(
        //         EffectTransition.RLinear,
        //         main.get_effect_transition_icon(EffectTransition.RLinear),
        //         main.getString(R.string.effect_transition_rlinear)
        //     )
        // ).filter { filter.contains(it.first) }

        // this.dialog_popup_menu(main.getString(R.string.dialog_transition), options, default = event.transition, transition) { i: Int, transition: EffectTransition ->
        //     this.track(TrackedAction.SetTransitionAtCursor, ActionTracker.string_to_ints(transition.name))
        //     event.transition = transition
        //     context_menu.widget.set_event(event)
        // }
    }

    fun set_velocity(velocity: Int? = null) {
       //  val main = this.get_activity()

       //  val context_menu = main.active_context_menu
       //  if (context_menu !is ContextMenuWithController<*>) {
       //      return
       //  }

       //  val widget: ControlWidgetVelocity = context_menu.get_widget() as ControlWidgetVelocity

       //  val dlg_default = (widget.get_event().value * 100F).toInt()
       //  val dlg_title = main.getString(R.string.dlg_set_velocity)
       //  this.dialog_number_input(dlg_title, widget.min, widget.max, dlg_default, velocity) { new_value: Int ->
       //      this.track(TrackedAction.SetVelocityAtCursor, listOf(new_value))
       //      val new_event = OpusVelocityEvent(
       //          new_value.toFloat() / 100F,
       //          widget.get_event().duration,
       //          widget.get_event().transition,
       //      )
       //      widget.set_event(new_event)
       //  }
    }
    fun set_volume(volume: Int? = null) {
        // val main = this.get_activity()

        // val context_menu = main.active_context_menu
        // if (context_menu !is ContextMenuWithController<*>) return

        // val widget: ControlWidgetVolume = context_menu.get_widget() as ControlWidgetVolume

        // val dlg_default = (widget.get_event().value * 100F).toInt()
        // val dlg_title = main.getString(R.string.dlg_set_volume)
        // this.dialog_number_input(dlg_title, widget.min, widget.max, dlg_default, volume) { new_value: Int ->
        //     this.track(TrackedAction.SetVolumeAtCursor, listOf(new_value))
        //     val new_event = OpusVolumeEvent(
        //         new_value.toFloat() / 100F,
        //         widget.get_event().duration,
        //         widget.get_event().transition
        //     )
        //     widget.set_event(new_event)
        // }
    }

    fun set_duration(duration: Int? = null) {
        // val main = this.get_activity()
        // val opus_manager = main.get_opus_manager()
        // val cursor = opus_manager.cursor
        // val (beat_key, position) = opus_manager.get_actual_position(
        //     cursor.get_beatkey(),
        //     cursor.get_position()
        // )

        // val event_duration = opus_manager.get_tree(beat_key, position).get_event()?.duration ?: return

        // this.dialog_number_input(main.getString(R.string.dlg_duration), 1, 99, event_duration, duration) { value: Int ->
        //     this.track(TrackedAction.SetDuration, listOf(value))
        //     opus_manager.set_duration(beat_key, position, max(1, value))
        // }
    }

    fun show_hidden_line_controller(forced_value: EffectType? = null) {
        val opus_manager = this.get_opus_manager()
        val options = mutableListOf<Triple<EffectType, Int?, String>>( )
        val cursor = opus_manager.cursor

        for ((ctl_type, icon_id) in OpusLayerInterface.line_controller_domain) {
            if (opus_manager.is_line_ctl_visible(ctl_type, cursor.channel, cursor.line_offset)) continue
            options.add(Triple(ctl_type, icon_id, ctl_type.name))
        }

        this.dialog_popup_menu(this.get_activity().getString(R.string.show_line_controls), options, stub_output = forced_value) { index: Int, ctl_type: EffectType ->
            this.track(TrackedAction.ShowLineController, ActionTracker.string_to_ints(ctl_type.name))
            opus_manager.toggle_line_controller_visibility(ctl_type, cursor.channel, cursor.line_offset)
        }
    }

    fun show_hidden_channel_controller(forced_value: EffectType? =  null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val options = mutableListOf<Triple<EffectType, Int?, String>>( )

        for ((ctl_type, icon_id) in OpusLayerInterface.channel_controller_domain) {
            if (opus_manager.is_channel_ctl_visible(ctl_type, cursor.channel)) continue
            options.add(Triple(ctl_type, icon_id, ctl_type.name))
        }

        this.dialog_popup_menu(this.get_activity().getString(R.string.show_channel_controls), options, stub_output = forced_value) { index: Int, ctl_type: EffectType ->
            this.track(TrackedAction.ShowChannelController, ActionTracker.string_to_ints(ctl_type.name))
            opus_manager.toggle_channel_controller_visibility(ctl_type, cursor.channel)
        }
    }

    fun show_hidden_global_controller(forced_value: EffectType? =  null) {
        val opus_manager = this.get_opus_manager()
        val options = mutableListOf<Triple<EffectType, Int?, String>>( )

        for ((ctl_type, icon_id) in OpusLayerInterface.global_controller_domain) {
            if (opus_manager.is_global_ctl_visible(ctl_type)) continue
            options.add(Triple(ctl_type, icon_id, ctl_type.name))
        }

        this.dialog_popup_menu(this.get_activity().getString(R.string.show_global_controls), options, stub_output = forced_value) { index: Int, ctl_type: EffectType ->
            this.track(TrackedAction.ShowGlobalController, ActionTracker.string_to_ints(ctl_type.name))
            opus_manager.toggle_global_controller_visibility(ctl_type)
        }
    }


    fun split(split: Int? = null) {
        this.dialog_number_input(R.string.dlg_split, 2, 32, stub_output = split) { splits: Int ->
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
        //this.track(TrackedAction.SetOffset, listOf(new_offset))

        //val opus_manager = this.get_opus_manager()
        //opus_manager.set_note_offset_at_cursor(new_offset)

        //val beat_key = opus_manager.cursor.get_beatkey()
        //val position = opus_manager.cursor.get_position()
        //val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        //this.get_activity().play_event(beat_key.channel, event_note)
    }

    fun set_octave(new_octave: Int) {
        // this.track(TrackedAction.SetOctave, listOf(new_octave))

        // val opus_manager = this.get_opus_manager()
        // opus_manager.set_note_octave_at_cursor(new_octave)

        // val beat_key = opus_manager.cursor.get_beatkey()
        // val position = opus_manager.cursor.get_position()
        // val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        // this.get_activity().play_event(beat_key.channel, event_note)
    }

    fun adjust_selection(amount: Int? = null) {
        // val opus_manager = this.get_opus_manager()
        // if (amount == null) {
        //     this.activity?.dialog_popup_selection_offset()
        // } else {
        //     this.track(TrackedAction.AdjustSelection, listOf(amount))
        //     opus_manager.offset_selection(amount)
        // }
    }

    fun unset() {
        this.track(TrackedAction.Unset)
        val opus_manager = this.get_opus_manager()
        opus_manager.unset()
    }

    fun unset_root() {
        // this.track(TrackedAction.UnsetRoot)
        // val opus_manager = this.get_activity().get_opus_manager()
        // val cursor = opus_manager.cursor
        // when (cursor.ctl_level) {
        //     CtlLineLevel.Global -> {
        //         opus_manager.cursor_select_ctl_at_global(cursor.ctl_type!!, cursor.beat, listOf())
        //     }
        //     CtlLineLevel.Channel -> {
        //         opus_manager.cursor_select_ctl_at_channel(cursor.ctl_type!!, cursor.channel, cursor.beat, listOf())
        //     }
        //     CtlLineLevel.Line -> {
        //         val beat_key = cursor.get_beatkey()
        //         opus_manager.cursor_select_ctl_at_line(cursor.ctl_type!!, beat_key, listOf())
        //     }
        //     null -> {
        //         val beat_key = cursor.get_beatkey()
        //         opus_manager.unset(beat_key, listOf())
        //     }
        // }
    }

    fun remove_at_cursor() {
        this.track(TrackedAction.RemoveLeaf)
        val opus_manager = this.get_opus_manager()
        opus_manager.remove_at_cursor(1)
    }

    fun insert_leaf(repeat: Int? = null) {
        val activity = this.get_activity()
        val opus_manager = this.get_opus_manager()

        this.dialog_number_input(R.string.dlg_insert, 1, 29, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.InsertLeaf, listOf(count))
            println("BUH? $count")

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
        // this.track(TrackedAction.TogglePercussion)

        // val opus_manager = this.get_opus_manager()
        // val cursor = opus_manager.cursor
        // val beat_key = cursor.get_beatkey()
        // val position = cursor.get_position()
        // val current_tree_position = opus_manager.get_actual_position(beat_key, position)

        // if (opus_manager.get_tree(current_tree_position.first, current_tree_position.second).has_event()) {
        //     opus_manager.unset()
        // } else {
        //     opus_manager.set_percussion_event_at_cursor()
        //     val event_note = opus_manager.get_percussion_instrument(beat_key.channel, beat_key.line_offset)
        //     this.get_activity().play_event(beat_key.channel, event_note)
        // }
    }

    fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>? = null) {
        // val activity = this.get_activity()
        // val supported_instrument_names = activity.get_supported_preset_names()
        // val sorted_keys = supported_instrument_names.keys.toList().sortedBy {
        //     it.first + (it.second * 128)
        // }

        // val opus_manager = this.get_opus_manager()
        // val is_percussion = opus_manager.is_percussion(channel)
        // val default_position = opus_manager.get_channel_instrument(channel)

        // val options = mutableListOf<Triple<Pair<Int, Int>, Int?, String>>()
        // val current_instrument_supported = sorted_keys.contains(default_position)

        // fun padded_hex(i: Int): String {
        //     var s = Integer.toHexString(i)
        //     while (s.length < 2) {
        //         s = "0$s"
        //     }
        //     return s.uppercase()
        // }

        // for (key in sorted_keys) {
        //     val name = supported_instrument_names[key]
        //     if (is_percussion) {
        //         if (key.first == 128) {
        //             options.add(Triple(key, null, "${padded_hex(key.second)}] $name"))
        //         }
        //     } else if (key.first != 128) {
        //         val pairstring = if (key.first == 0) {
        //             padded_hex(key.second)
        //         } else {
        //             "${padded_hex(key.second)}.${padded_hex(key.first)}"
        //         }
        //         options.add(Triple(key, null, "$pairstring) $name"))
        //     } else if (activity.configuration.allow_std_percussion) {
        //         options.add(Triple(key, null, "${padded_hex(key.second)}] $name"))
        //     }
        // }

        // // Separated KIts and tuned instruments. Kits are always bank 128, but the other instruments are defined by program with variants using the bank
        // options.sortBy { (key, _) ->
        //     if (key.first == 128) {
        //         (key.first * 128) + key.second
        //     } else {
        //         (key.second * 128) + key.first
        //     }
        // }

        // if (is_percussion) {
        //     val use_menu_dialog = options.isNotEmpty() && (!current_instrument_supported || options.size > 1)

        //     if (use_menu_dialog) {
        //         this.dialog_popup_menu(activity.getString(R.string.dropdown_choose_instrument), options, default = default_position, stub_output = instrument) { _: Int, instrument: Pair<Int, Int> ->
        //             this.track(
        //                 TrackedAction.SetChannelInstrument,
        //                 listOf(channel, instrument.first, instrument.second)
        //             )
        //             opus_manager.channel_set_instrument(channel, instrument)
        //         }
        //     } else {
        //         this.dialog_number_input(activity.getString(R.string.dropdown_choose_instrument), 0, 127, default_position.second, stub_output = instrument?.second) { program: Int ->
        //             this.track(
        //                 TrackedAction.SetChannelInstrument,
        //                 listOf(channel, instrument?.first ?: 1, program)
        //             )
        //             opus_manager.channel_set_instrument(channel, Pair(instrument?.first ?: 1, program))
        //         }
        //     }
        // } else if (options.size > 1 || !current_instrument_supported) {
        //     this.dialog_popup_menu(activity.getString(R.string.dropdown_choose_instrument), options, default = default_position, stub_output = instrument) { _: Int, instrument: Pair<Int, Int> ->
        //         this.track(
        //             TrackedAction.SetChannelInstrument,
        //             listOf(channel, instrument.first, instrument.second)
        //         )
        //         opus_manager.channel_set_instrument(channel, instrument)


        //         val radix = opus_manager.get_radix()
        //         thread {
        //             activity.play_event(channel, (radix * 3))
        //             Thread.sleep(200)
        //             activity.play_event(channel, (radix * 3) + (radix / 2))
        //             Thread.sleep(200)
        //             activity.play_event(channel, (radix * 4))
        //             Thread.sleep(200)
        //         }
        //     }
        // }
    }

    fun insert_percussion_channel(index: Int? = null) {
        this.track(TrackedAction.InsertChannel, listOf(index ?: -1, 1))
        val opus_manager = this.get_opus_manager()
        if (index != null) {
            opus_manager.new_channel(index, is_percussion = true)
        } else {
            val channel = opus_manager.cursor.channel
            opus_manager.new_channel(channel + 1, is_percussion = true)
        }
    }

    fun insert_channel(index: Int? = null) {
        this.track(TrackedAction.InsertChannel, listOf(index ?: -1, 0))
        val opus_manager = this.get_opus_manager()
        if (index != null) {
            opus_manager.new_channel(index)
        } else {
            val channel = opus_manager.cursor.channel
            opus_manager.new_channel(channel + 1)
        }
    }

    fun remove_channel(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.channels.size > 1) {
            val use_index = index ?: opus_manager.cursor.channel
            this.track(TrackedAction.RemoveChannel, listOf(use_index))
            opus_manager.remove_channel(use_index)
        }
    }

    fun move_channel(index_from: Int, index_to: Int, before: Boolean = true) {
        val opus_manager = this.get_opus_manager()
        val adj_to_index = index_to + if (before) 0 else 1
        if (adj_to_index == index_from) return

        this.track(TrackedAction.MoveChannel, listOf(index_from, adj_to_index))
        opus_manager.move_channel(index_from, adj_to_index)
    }


    fun insert_line(count: Int? = null) {
        // val main = this.get_activity()
        // val opus_manager = main.get_opus_manager()
        // this.dialog_number_input(main.getString(R.string.dlg_insert_lines), 1, 9, stub_output = count) { count: Int ->
        //     this.track(TrackedAction.InsertLine, listOf(count))
        //     opus_manager.insert_line_at_cursor(count)
        // }
    }

    fun remove_line(count: Int? = null) {
        // val main = this.get_activity()
        // val opus_manager = main.get_opus_manager()
        // val lines = opus_manager.get_all_channels()[opus_manager.cursor.channel].size
        // val max_lines = Integer.min(lines - 1, lines - opus_manager.cursor.line_offset)
        // this.dialog_number_input(main.getString(R.string.dlg_remove_lines), 1, max_lines, stub_output = count) { count: Int ->
        //     this.track(TrackedAction.RemoveLine, listOf(count))
        //     opus_manager.remove_line_at_cursor(count)
        // }
    }

    fun set_percussion_instrument(value: Int? = null) {
        // val main = this.get_activity()
        // val opus_manager = this.get_opus_manager()
        // val cursor = opus_manager.cursor
        // val default_instrument = opus_manager.get_percussion_instrument(cursor.channel, cursor.line_offset)

        // val options = mutableListOf<Triple<Int, Int?, String>>()
        // this.dialog_popup_menu(main.getString(R.string.dropdown_choose_percussion), options, default_instrument, stub_output = value) { _: Int, value: Int ->
        //     this.track(TrackedAction.SetPercussionInstrument, listOf(value))
        //     opus_manager.set_percussion_instrument(value)
        //     main.play_event(cursor.channel, value)
        // }
    }

    fun set_pan_at_cursor(value: Int) {
        // val main = this.get_activity()

        // this.track(TrackedAction.SetPanAtCursor, listOf(value))
        // val context_menu = main.active_context_menu
        // if (context_menu !is ContextMenuWithController<*>) {
        //     return
        // }

        // val widget = context_menu.get_widget() as ControlWidgetPan
        // val new_event = widget.get_event().copy()
        // new_event.value = (value.toFloat() / widget.max.toFloat()) * -1F
        // widget.set_event(new_event)
    }

    fun set_tempo_at_cursor(input_value: Float) {
        // val main = this.get_activity()

        // val context_menu = main.active_context_menu
        // if (context_menu !is ContextMenuWithController<*>) {
        //     return
        // }
        // val widget = context_menu.get_widget() as ControlWidgetTempo
        // val rounded_value = (input_value * 1000F).roundToInt().toFloat() / 1000F
        // this.track(TrackedAction.SetTempoAtCursor, listOf(input_value.toBits()))

        // val new_event = widget.get_event().copy()
        // new_event.value = rounded_value

        // widget.set_event(new_event)
    }

    fun set_delay_at_cursor(numerator: Int, denominator: Int, fade: Float, repeat: Int) {
        // val main = this.get_activity()

        // this.track(TrackedAction.SetDelayAtCursor, listOf(numerator, denominator, fade.toBits(), repeat))
        // val context_menu = main.active_context_menu
        // if (context_menu !is ContextMenuWithController<*>) {
        //     return
        // }

        // val widget = context_menu.get_widget() as ControlWidgetDelay
        // val new_event = widget.get_event().copy()
        // new_event.numerator = numerator
        // new_event.denominator = denominator
        // new_event.fade = fade
        // new_event.echo = repeat

        // widget.set_event(new_event)
    }

    fun insert_beat(beat: Int, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(R.string.dlg_insert_beats, 1, 4096, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.InsertBeatAt, listOf(beat, count))
            opus_manager.insert_beats(beat, count)
        }
    }

    fun remove_beat(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(R.string.dlg_remove_beats, 1, opus_manager.length - 1, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.RemoveBeat, listOf(count))
            opus_manager.remove_beat_at_cursor(count)
        }
    }

    fun insert_beat_after_cursor(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(R.string.dlg_insert_beats, 1, 4096, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.InsertBeat, listOf(count))
            opus_manager.insert_beat_after_cursor(count)
        }
    }

    fun remove_beat_at_cursor(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(R.string.dlg_remove_beats, 1, opus_manager.length - 1, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.RemoveBeat, listOf(count))
            opus_manager.remove_beat_at_cursor(count)
        }
    }

    fun set_relative_mode(mode: RelativeInputMode) {
        this.track(TrackedAction.SetRelativeMode, listOf(mode.ordinal))

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
                    RelativeInputMode.Absolute -> {
                        try {
                            opus_manager.convert_event_to_absolute()
                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            (new_event as AbsoluteNoteEvent).note
                        } catch (_: NoteOutOfRange) {
                            opus_manager.set_event_at_cursor(
                                AbsoluteNoteEvent(0, event.duration)
                            )
                            0
                        }
                    }
                    /* + */
                    RelativeInputMode.Positive -> {
                        if (event.offset < 0) {
                            val new_event = RelativeNoteEvent(0 - event.offset, event.duration)
                            opus_manager.set_event_at_cursor(new_event)
                        }
                        abs(event.offset)
                    }
                    /* - */
                    RelativeInputMode.Negative -> {
                        if (event.offset > 0) {
                            val new_event = RelativeNoteEvent(0 - event.offset, event.duration)
                            opus_manager.set_event_at_cursor(new_event)
                        }
                        abs(event.offset)
                    }
                }
            }
            is AbsoluteNoteEvent -> {
                when (mode) {
                    /* Abs */
                    RelativeInputMode.Absolute -> {
                        event.note
                    }
                    /* + */
                    RelativeInputMode.Positive -> {
                        val cursor = opus_manager.cursor
                        val value = opus_manager.get_relative_value(cursor.get_beatkey(), cursor.get_position())
                        if (value >= 0) {
                            opus_manager.convert_event_to_relative()

                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            abs((new_event as RelativeNoteEvent).offset)
                        } else {
                            opus_manager.relative_mode = RelativeInputMode.Positive
                            null
                        }

                    }
                    /* - */
                    RelativeInputMode.Negative -> {
                        val cursor = opus_manager.cursor
                        val value = opus_manager.get_relative_value(cursor.get_beatkey(), cursor.get_position())
                        if (value <= 0) {
                            opus_manager.convert_event_to_relative()

                            //val current_tree = opus_manager.get_tree()
                            val new_event = current_tree.get_event()!!
                            abs((new_event as RelativeNoteEvent).offset)
                        } else {
                            opus_manager.relative_mode = RelativeInputMode.Negative
                            null
                        }
                    }
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
            nsOctave.set_state(new_value / radix, manual = true, suppress_callback = true)
            nsOffset.set_state(new_value % radix, manual = true, suppress_callback = true)
        }

        opus_manager.relative_mode = mode
    }

    fun set_project_name_and_notes(project_name_and_notes: Pair<String, String>? = null) {
        val opus_manager = this.get_opus_manager()
        val default = Pair(opus_manager.project_name ?: "", opus_manager.project_notes ?: "")

        this.dialog_name_and_notes_popup(default, project_name_and_notes) { name: String, notes: String ->
            val name_ints = ActionTracker.string_to_ints(name)
            this.track(
                TrackedAction.SetProjectNameAndNotes,
                listOf(name_ints.size) + name_ints + ActionTracker.string_to_ints(notes)
            )

            val opus_manager = this.get_opus_manager()
            opus_manager.set_name_and_notes(
                if (name == "") null else name,
                if (notes == "") null else notes
            )
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
            // this.get_activity().dialog_confirm(title, callback)
        }
    }

    /**
     *  wrapper around MainActivity::dialog_number_input
     *  will subvert the popup on replay
     */
    private fun dialog_number_input(title_string_id: Int, min_value: Int, max_value: Int, default: Int? = null, stub_output: Int? = null, callback: (value: Int) -> Unit) {
        if (stub_output != null) {
            callback(stub_output)
        } else {
            this.activity?.view_model?.dialog_queue?.value?.new_dialog { dialog_queue, dialog_key ->
                @Composable {
                    val value: MutableState<Int> = remember { mutableIntStateOf(default ?: min_value) }
                    Column(Modifier.padding(dimensionResource(R.dimen.dialog_padding))) {
                        Row {
                            SText(title_string_id)
                        }
                        Row {
                            IntegerInput(
                                value = value,
                                minimum = min_value,
                                maximum = max_value,
                                modifier = Modifier.fillMaxWidth()
                            ) { new_value ->
                                dialog_queue.remove(dialog_key)
                                callback(new_value)
                            }
                        }

                        Row {
                            Button(
                                modifier = Modifier.fillMaxWidth().weight(1F),
                                onClick = { dialog_queue.remove(dialog_key) },
                                content = { SText(android.R.string.cancel) }
                            )

                            Button(
                                modifier = Modifier.fillMaxWidth().weight(1F),
                                onClick = {
                                    callback(value.value)
                                    dialog_queue.remove(dialog_key)
                                },
                                content = { SText(android.R.string.ok) }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     *  wrapper around MainActivity::dialog_float_input
     *  will subvert the popup on replay
     */
    private fun dialog_float_input(title: String, min_value: Float, max_value: Float, default: Float? = null, stub_output: Float? = null, callback: (value: Float) -> Unit) {
        // if (stub_output != null) {
        //     callback(stub_output)
        // } else {
        //     val activity = this.get_activity()
        //     activity.dialog_float_input(title, min_value, max_value, default, callback)
        // }
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
    private fun <T> dialog_popup_menu(title: String, options: List<Triple<T, Int?, String>>, default: T? = null, stub_output: T? = null, callback: (index: Int, value: T) -> Unit) {
        // if (stub_output != null) {
        //     callback(-1, stub_output)
        // } else {
        //     val activity = this.get_activity()
        //     activity.dialog_popup_menu(title, options, default, callback)
        // }
    }

    private fun dialog_text_popup(title: String, default: String? = null, stub_output: String? = null, callback: (String) -> Unit) {
        // val activity = this.get_activity()
        // if (stub_output != null) {
        //     callback(stub_output)
        // } else {
        //     activity.dialog_text_popup(title, default, callback)
        // }
    }

    private fun dialog_name_and_notes_popup(default: Pair<String, String>? = null, stub_output: Pair<String, String>? = null, callback: (String, String) -> Unit) {
        // val activity = this.get_activity()
        // if (stub_output != null) {
        //     callback(stub_output.first, stub_output.second)
        // } else {
        //     activity.dialog_name_and_notes_popup(default, callback)
        // }
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
        return this.get_activity().model_editor.opus_manager
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
            TrackedAction.SetDelayAtCursor -> {
                this.set_delay_at_cursor(
                    integers[0]!!,
                    integers[1]!!,
                    Float.fromBits(integers[2]!!),
                    integers[3]!!
                )
            }
            TrackedAction.ApplyUndo -> {
                this.apply_undo()
            }
            TrackedAction.NewProject -> {
                this.new_project()
            }
            TrackedAction.LoadProject -> {
                this.load_project(ActionTracker.string_from_ints(integers).toUri())
            }
            TrackedAction.CursorSelectColumn -> {
                this.cursor_select_column(integers[0]!!)
            }
            TrackedAction.CursorSelectGlobalCtlRange -> {
                this.cursor_select_global_ctl_range(
                    ActionTracker.type_from_ints(integers),
                    integers[1]!!,
                    integers[2]!!
                )
            }
            TrackedAction.CursorSelectChannelCtlRange -> {
                val offset = integers[0]!!
                this.cursor_select_channel_ctl_range(
                    ActionTracker.type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!,
                    integers[3 + offset]!!
                )
            }
            TrackedAction.CursorSelectLineCtlRange -> {
                val offset = integers[0]!!
                this.cursor_select_line_ctl_range(
                    ActionTracker.type_from_ints(integers),
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
                    ActionTracker.type_from_ints(integers),
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
                    ActionTracker.type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!,
                    List(integers.size - 3 - offset) { i: Int -> integers[i + 3 + offset]!! }
                )
            }
            TrackedAction.CursorSelectLeafCtlGlobal -> {
                val offset = integers[0]!!
                this.cursor_select_ctl_at_global(
                    ActionTracker.type_from_ints(integers),
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
                    ActionTracker.type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!
                )
            }
            TrackedAction.CursorSelectChannelCtlLine -> {
                val offset = integers[0]!!
                this.cursor_select_channel_ctl_line(
                    ActionTracker.type_from_ints(integers),
                    integers[1 + offset]!!
                )
            }
            TrackedAction.CursorSelectGlobalCtlLine -> {
                this.cursor_select_global_ctl_line(ActionTracker.type_from_ints(integers))
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
                    ActionTracker.type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!,
                    integers[3 + offset]
                )
            }
            TrackedAction.RepeatSelectionCtlChannel -> {
                val offset = integers[0]!!
                this.repeat_selection_ctl_channel(
                    ActionTracker.type_from_ints(integers),
                    integers[1 + offset]!!,
                    integers[2 + offset]!!
                )
            }
            TrackedAction.RepeatSelectionCtlGlobal -> {
                val offset = integers[0]!!
                this.repeat_selection_ctl_global(
                    ActionTracker.type_from_ints(integers),
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
            TrackedAction.AdjustSelection -> {
                this.adjust_selection(integers[0])
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
                this.set_ctl_duration<EffectEvent>(integers[0])
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
                this.set_ctl_transition(EffectTransition.valueOf(string_from_ints(integers)))
            }
            TrackedAction.SetVolumeAtCursor -> {
                this.set_volume(integers[0])
            }
            TrackedAction.SetVelocityAtCursor -> {
                this.set_velocity(integers[0])
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
                val is_percussion = integers[1]!! != 0
                if (index == -1) {
                    if (is_percussion) {
                        this.insert_percussion_channel(null)
                    } else {
                        this.insert_channel(null)
                    }
                } else if (is_percussion) {
                    this.insert_percussion_channel(index)
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
                        ActionTracker.string_from_ints(integers)
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

            TrackedAction.SetProjectNameAndNotes -> {
                val size = integers[0]!!
                val project_name = ActionTracker.string_from_ints(integers.subList(1, size + 1))
                val project_notes = ActionTracker.string_from_ints(integers.subList(size + 1, integers.size))

                this.set_project_name_and_notes(Pair(project_name, project_notes))
            }

            TrackedAction.ShowLineController -> {
                this.show_hidden_line_controller(
                    EffectType.valueOf(string_from_ints(integers))
                )
            }
            TrackedAction.ShowChannelController -> {
                this.show_hidden_channel_controller(
                    EffectType.valueOf(string_from_ints(integers))
                )
            }
            TrackedAction.ShowGlobalController -> {
                this.show_hidden_global_controller(
                    EffectType.valueOf(string_from_ints(integers))
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
                //val uri_string = string_from_ints(integers)
                //val uri = Uri.parse(uri_string)
                //this.import(uri)
            }

            TrackedAction.SetRelativeMode -> {
                this.set_relative_mode(RelativeInputMode.entries[integers[0]!!])
            }

            TrackedAction.SwapLines -> {
                this.swap_lines(integers[0]!!, integers[1]!!, integers[2]!!, integers[3]!!)
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
            TrackedAction.TagColumn -> {
                if (integers.size > 1) {
                    this.tag_column(integers[0]!!, ActionTracker.string_from_ints(integers.subList(1, integers.size)))
                } else {
                    this.tag_column(integers[0]!!, null, true)
                }
            }
            TrackedAction.UntagColumn -> {
                this.untag_column(integers[0]!!)
            }
            TrackedAction.MoveLine -> {
                this.move_line(integers[0]!!, integers[1]!!, integers[2]!!, integers[3]!!)
            }
            TrackedAction.MoveChannel -> {
                this.move_channel(integers[0]!!, integers[1]!!)
            }
            TrackedAction.InsertBeatAt -> {
                this.insert_beat(integers[0]!!, integers[1]!!)
            }
        }
    }

    fun set_copy_mode(mode: PaganConfiguration.MoveMode) {
        // val main = this.get_activity()
        // val opus_manager = this.get_opus_manager()

        // main.configuration.move_mode = mode
        // main.save_configuration()

        // val context_menu = main.active_context_menu
        // if (context_menu !is ContextMenuRange) return

        // this.track(TrackedAction.SetCopyMode, ActionTracker.string_to_ints(mode.name))

        // context_menu.label.text = when (mode) {
        //     PaganConfiguration.MoveMode.MOVE -> {
        //         if (opus_manager.cursor.mode == CursorMode.Range) {
        //             main.resources.getString(R.string.label_move_range)
        //         } else {
        //             main.resources.getString(R.string.label_move_beat)
        //         }
        //     }
        //     PaganConfiguration.MoveMode.COPY -> {
        //         if (opus_manager.cursor.mode == CursorMode.Range) {
        //             main.resources.getString(R.string.label_copy_range)
        //         } else {
        //             main.resources.getString(R.string.label_copy_beat)
        //         }
        //     }
        //     PaganConfiguration.MoveMode.MERGE -> {
        //         if (opus_manager.cursor.mode == CursorMode.Range) {
        //             main.resources.getString(R.string.label_merge_range)
        //         } else {
        //             main.resources.getString(R.string.label_merge_beat)
        //         }
        //     }
        // }
    }

    fun remove_controller() {
        this.track(TrackedAction.RemoveController)
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> opus_manager.remove_line_controller(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
            CtlLineLevel.Channel -> opus_manager.remove_channel_controller(cursor.ctl_type!!, cursor.channel)
            CtlLineLevel.Global -> opus_manager.remove_global_controller(cursor.ctl_type!!)
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

            CtlLineLevel.Global -> {
                opus_manager.toggle_global_controller_visibility(cursor.ctl_type!!)
            }
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

    fun tag_column(beat: Int? = null, description: String? = null, force_null_description: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        val use_beat = beat ?: opus_manager.cursor.beat

        if (force_null_description && description == null) {
            val integers = mutableListOf(use_beat)
            this.track(TrackedAction.TagColumn, integers)
            opus_manager.tag_section(use_beat, null)
            return
        }

        this.dialog_text_popup(this.get_activity().getString(R.string.dialog_mark_section), opus_manager.marked_sections[use_beat], description) { result: String ->
            val integers = mutableListOf(use_beat)
            for (byte in result.toByteArray()) {
                integers.add(byte.toInt())
            }

            this.track(TrackedAction.TagColumn, integers)
            opus_manager.tag_section(use_beat, if (result == "") null else result)
        }
    }

    fun untag_column(beat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val use_beat = beat ?: opus_manager.cursor.beat
        this.track(TrackedAction.UntagColumn, listOf(use_beat))
        opus_manager.remove_tagged_section(use_beat)
    }

    internal fun _track_tuning_map_and_transpose(tuning_map: Array<Pair<Int, Int>>, transpose: Pair<Int, Int>) {
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
        // if (tuning_map == null || transpose == null) {
        //     this.get_activity().dialog_tuning_table()
        // } else {
        //     val opus_manager = this.get_opus_manager()
        //     this._track_tuning_map_and_transpose(tuning_map, transpose)
        //     opus_manager.set_tuning_map_and_transpose(tuning_map, transpose)
        // }
    }

    fun move_line(channel_from: Int, line_offset_from: Int, channel_to: Int, line_offset_to: Int, before: Boolean = true) {
       // try {
       //     val adj_to_index = line_offset_to + if (before) 0 else 1
       //     if (adj_to_index == line_offset_from && channel_from == channel_to) return

       //     this.get_opus_manager().move_line(
       //         channel_from,
       //         line_offset_from,
       //         channel_to,
       //         adj_to_index
       //     )
       //     this.track(
       //         TrackedAction.MoveLine,
       //         listOf(channel_from, line_offset_from, channel_to, adj_to_index)
       //     )
       // } catch (e: IncompatibleChannelException) {
       //     this.toast(R.string.std_percussion_swap)
       // }
    }


    // TODO: Reimplement once i figure out how action tracking will work with split activities
    //fun import(uri: Uri? = null) {
    //
    //    // TODO: Track action
    //    val activity = this.get_activity()
    //    if (uri == null) {
    //        val intent = Intent()
    //        intent.setAction(Intent.ACTION_GET_CONTENT)
    //        intent.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
    //        activity.general_import_intent_launcher.launch(intent)
    //    } else {
    //        // TODO: Right now this still needs to be manually handled during playback. Not sure if its even possible to automate
    //        val intent = Intent()
    //        intent.setAction(Intent.ACTION_GET_CONTENT)
    //        intent.setType("*/*") // Allow all, for some reason the emulators don't recognize midi files
    //        activity.general_import_intent_launcher.launch(intent)
    //    }
    //}

    fun to_json(): JSONObject {
        return JSONList(
            *Array(this.action_queue.size) { i: Int ->
                item_to_json(this.action_queue[i])
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
