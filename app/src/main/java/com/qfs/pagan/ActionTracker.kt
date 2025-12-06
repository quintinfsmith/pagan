package com.qfs.pagan

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.net.toUri
import com.qfs.json.JSONBoolean
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.json.JSONObject
import com.qfs.json.JSONString
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.SortableMenu
import com.qfs.pagan.composable.TextInput
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.IncompatibleChannelException
import com.qfs.pagan.structure.opusmanager.base.InvalidOverwriteCall
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlinx.coroutines.CoroutineScope
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import com.qfs.pagan.OpusLayerInterface as OpusManager


/**
 * Handle all (or as much of as possible) of the logic between a user action and the OpusManager.
 * This class is meant for recording and playing back UI tests and eventually debugging so
 * not every action directed through here at the moment.
 */
class ActionTracker(var vm_controller: ViewModelEditorController) {
    var DEBUG_ON = false
    class NoActivityException: Exception()
    class OpusManagerDetached: Exception()
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
        CursorSelectChannel,
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
        RemoveBeat,
        InsertBeat,
        InsertBeatAt,
        SetCopyMode,
        DrawerOpen,
        DrawerClose,
        SetProjectNameAndNotes,
        SetTuningTable,
        ImportSong,
        SetRelativeMode,
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
                    // STRING
                    TrackedAction.ImportSong,
                    TrackedAction.ShowLineController,
                    TrackedAction.ShowChannelController,
                    TrackedAction.ShowGlobalController,
                    TrackedAction.SetCopyMode,
                    TrackedAction.LoadProject -> {
                        val string = entry.get_string(1)
                        this.string_to_ints(string)
                    }

                    TrackedAction.CursorSelectChannel -> {
                        listOf(entry.get_int(1))
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

                    // STRING
                    TrackedAction.ShowLineController,
                    TrackedAction.ShowChannelController,
                    TrackedAction.ShowGlobalController,
                    TrackedAction.SetCopyMode,
                    TrackedAction.ImportSong,
                    TrackedAction.LoadProject -> {
                        arrayOf(JSONString(this.string_from_ints(integers)))
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

    private var ignore_flagged: Boolean = false
    private val action_queue = mutableListOf<Pair<TrackedAction, List<Int?>?>>()
    var lock: Boolean = false
    lateinit var vm_top: ViewModelPagan

    fun attach_top_model(model: ViewModelPagan) {
        this.vm_top = model
    }

    fun apply_undo() {
        this.track(TrackedAction.ApplyUndo)
        this.get_opus_manager().apply_undo()
    }

    fun _move_selection_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MoveSelectionToBeat,
            beat_key.to_list()
        )
        this.vm_controller.opus_manager.move_to_beat(beat_key)
    }

    fun _copy_selection_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.CopySelectionToBeat,
            beat_key.to_list()
        )
        this.vm_controller.opus_manager.copy_to_beat(beat_key)
    }

    fun move_selection_to_beat(beat_key: BeatKey) {
        when (this.vm_controller.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this._move_selection_to_beat(beat_key)
            }
            PaganConfiguration.MoveMode.COPY -> {
                this._copy_selection_to_beat(beat_key)
            }
            PaganConfiguration.MoveMode.MERGE -> {
                this._merge_selection_into_beat(beat_key)
            }
        }
    }

    fun _merge_selection_into_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MergeSelectionIntoBeat,
            beat_key.to_list()
        )
        this.vm_controller.opus_manager.merge_into_beat(beat_key)
    }

    fun save() {
        this.track(TrackedAction.SaveProject)
        this.vm_top.project_manager?.let {
            val uri = it.save(this.vm_controller.opus_manager, this.vm_controller.active_project)
            this.vm_top.has_saved_project.value = true
            this.vm_controller.active_project = uri
            this.vm_controller.project_exists.value = true
        }
    }

    fun delete() {
        this.vm_controller.active_project?.let { project ->
            this.vm_top.delete_project(project)
        }
        TODO("Track, Close Drawer && show toast")
    }

    fun project_copy() {
        this.vm_controller.active_project ?: return
        this.save_before {
            val opus_manager = this.vm_controller.opus_manager
            val old_title = opus_manager.project_name
            opus_manager.set_project_name(
                if (old_title == null) null
                else "$old_title (Copy)"
            )
            this.vm_controller.active_project = null
            this.vm_controller.project_exists.value = false
        }
    }

    fun cursor_clear() {
        // TODO Track
        //this.track(TrackedAction.CursorClear)
        this.vm_controller.opus_manager.cursor_clear()
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        this.track(
            TrackedAction.CursorSelectLeaf,
            beat_key.to_list() + position
        )

        val opus_manager = this.vm_controller.opus_manager
        opus_manager.cursor_select(beat_key, position)

        val tree = opus_manager.get_tree()
        if (tree.has_event()) {
            val note = if (opus_manager.is_percussion(beat_key.channel)) {
                opus_manager.get_percussion_instrument(beat_key.channel, beat_key.line_offset)
            } else {
                opus_manager.get_absolute_value(beat_key, position) ?: return
            }
            if (note >= 0) {
                this.play_event(
                    beat_key.channel,
                    note
                )
            }
        }
    }

    fun cursor_select_line(channel: Int?, line_offset: Int?, ctl_type: EffectType?) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.mode == CursorMode.Line && channel == cursor.channel && line_offset == cursor.line_offset && cursor.ctl_type == ctl_type) {
            this.cursor_select_channel(channel)
        } else if (ctl_type == null) {
             this.cursor_select_line_std(channel!!, line_offset!!)
        } else if (line_offset != null) {
            this.cursor_select_line_ctl_line(ctl_type, channel!!, line_offset)
        } else if (channel != null) {
            this.cursor_select_channel_ctl_line(ctl_type, channel)
        } else {
            this.cursor_select_global_ctl_line(ctl_type)
        }
    }

    fun move_line_ctl_to_beat(beat_key: BeatKey) {
        when (this.vm_controller.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this._move_line_ctl_to_beat(beat_key)
            }
            PaganConfiguration.MoveMode.MERGE -> TODO()
            PaganConfiguration.MoveMode.COPY -> {
                this._copy_line_ctl_to_beat(beat_key)
            }
        }
    }
    fun _move_line_ctl_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.MoveLineCtlToBeat,
            beat_key.to_list()
        )
        this.get_opus_manager().move_line_ctl_to_beat(beat_key)
    }

    fun _copy_line_ctl_to_beat(beat_key: BeatKey) {
        this.track(
            TrackedAction.CopyLineCtlToBeat,
            beat_key.to_list()
        )
        this.get_opus_manager().copy_line_ctl_to_beat(beat_key)
    }

    fun move_channel_ctl_to_beat(channel: Int, beat: Int) {
        when (this.vm_controller.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this._move_channel_ctl_to_beat(channel, beat)
            }
            PaganConfiguration.MoveMode.MERGE -> TODO()
            PaganConfiguration.MoveMode.COPY -> {
                this._copy_channel_ctl_to_beat(channel, beat)
            }
        }
    }
    fun _move_channel_ctl_to_beat(channel: Int, beat: Int) {
        this.track(
            TrackedAction.MoveChannelCtlToBeat,
            listOf(channel, beat)
        )
        this.get_opus_manager().move_channel_ctl_to_beat(channel, beat)
    }

    fun _copy_channel_ctl_to_beat(channel: Int, beat: Int) {
        this.track(
            TrackedAction.CopyChannelCtlToBeat,
            listOf(channel, beat)
        )
        this.get_opus_manager().copy_channel_ctl_to_beat(channel, beat)
    }

    fun move_global_ctl_to_beat(beat: Int) {
        when (this.vm_controller.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this._move_global_ctl_to_beat(beat)
            }
            PaganConfiguration.MoveMode.MERGE -> TODO()
            PaganConfiguration.MoveMode.COPY -> {
                this._copy_global_ctl_to_beat(beat)
            }
        }
    }
    fun _move_global_ctl_to_beat(beat: Int) {
        this.track(
            TrackedAction.MoveGlobalCtlToBeat,
            listOf(beat)
        )
        this.get_opus_manager().move_global_ctl_to_beat(beat)
    }

    fun _copy_global_ctl_to_beat(beat: Int) {
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
    fun cursor_select_channel(channel: Int) {
        this.track(TrackedAction.CursorSelectChannel, listOf(channel))

        this.vm_controller.opus_manager.cursor_select_channel(channel)
    }

    fun cursor_select_line_std(channel: Int, line_offset: Int) {
        this.track(TrackedAction.CursorSelectLine, listOf(channel, line_offset))
        this.vm_controller.opus_manager.cursor_select_line(channel, line_offset)
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

    fun cursor_select_column(beat: Int? = null) {
        if (beat != null) {
            this.track(TrackedAction.CursorSelectColumn, listOf(beat))
            this.get_opus_manager().cursor_select_column(beat)
            return
        }

        this.vm_top.create_dialog { close ->
            @Composable {
                val scope = rememberCoroutineScope()
                //val opus_manager = this.get_opus_manager()
                val scrolled_value = remember { mutableStateOf(0) }

                Row {
                    DialogTitle(stringResource(R.string.label_shortcut_scrollbar, scrolled_value))
                }
                Row {

                }
            }
        }
    }

    fun cursor_select_range_next(beat_key: BeatKey) {
        // TODO: Track
        this.vm_controller.opus_manager.cursor_select_range_next(beat_key)
    }
    fun cursor_select_line_ctl_range_next(type: EffectType, beat_key: BeatKey) {
        // TODO: Track
        this.vm_controller.opus_manager.cursor_select_line_ctl_range_next(type, beat_key)
    }
    fun cursor_select_channel_ctl_range_next(type: EffectType, channel: Int, beat: Int) {
        // TODO: Track
        this.vm_controller.opus_manager.cursor_select_channel_ctl_range_next(type, channel, beat)
    }
    fun cursor_select_global_ctl_range_next(type: EffectType, beat: Int) {
        // TODO: Track
        this.vm_controller.opus_manager.cursor_select_global_ctl_range_next(type, beat)
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

        this.vm_controller.opus_manager.cursor_select_range(first_key, second_key)
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

        this.vm_controller.opus_manager.cursor_select_line_ctl_range(type, first_key, second_key)
    }

    fun cursor_select_channel_ctl_range(type: EffectType, channel: Int, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectChannelCtlRange, ActionTracker.enum_to_ints(type) + listOf(first_beat, second_beat))
        this.vm_controller.opus_manager.cursor_select_channel_ctl_range(type, channel, first_beat, second_beat)
    }


    fun cursor_select_global_ctl_range(type: EffectType, first_beat: Int, second_beat: Int) {
        this.track(TrackedAction.CursorSelectGlobalCtlRange, ActionTracker.enum_to_ints(type) + listOf(first_beat, second_beat))
        this.vm_controller.opus_manager.cursor_select_global_ctl_range(type, first_beat, second_beat)
    }

    fun new_project() {
        this.track(TrackedAction.NewProject)
        val opus_manager = this.get_opus_manager()
        opus_manager.project_change_new()

        for ((c, channel) in opus_manager.channels.enumerate()) {
            if (!opus_manager.is_percussion(c)) continue
            val i = this.vm_controller.audio_interface.get_minimum_instrument_index(channel.get_instrument())
            for (l in 0 until opus_manager.get_channel(c).size) {
                opus_manager.percussion_set_instrument(c, l, max(0, i - 27))
            }
        }
        this.vm_controller.update_soundfont_instruments()
        opus_manager.clear_history()
    }

    fun load_project(uri: Uri) {
        this.track(TrackedAction.LoadProject, ActionTracker.string_to_ints(uri.toString()))
        TODO()
        //activity.load_project(uri)
    }

    fun <T: EffectEvent> set_effect_at_cursor(event: T) {
        // TODO: Track
        val opus_manager = this.get_opus_manager()
        opus_manager.set_event_at_cursor(event)
    }

    fun set_volume_at_cursor(volume: Float? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.mode) {
            CursorMode.Line -> {
                val new_event = OpusVolumeEvent(volume ?: 100F)
                when (cursor.ctl_level) {
                    null,
                    CtlLineLevel.Line -> {
                        opus_manager.controller_line_set_initial_event(
                            EffectType.Volume,
                            cursor.channel,
                            cursor.line_offset,
                            new_event
                        )
                    }
                    CtlLineLevel.Channel -> {
                        opus_manager.controller_channel_set_initial_event(
                            EffectType.Volume,
                            cursor.channel,
                            new_event
                        )
                    }
                    CtlLineLevel.Global -> TODO()
                }
            }

            CursorMode.Single -> {

            }

            else -> throw Exception()
        }
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
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val (beat_key, position) = opus_manager.get_actual_position(
            cursor.get_beatkey(),
            cursor.get_position()
        )

        val event_duration = opus_manager.get_tree(beat_key, position).get_event()?.duration ?: return

        this.dialog_number_input(R.string.dlg_duration, 1, 99, event_duration, duration) { value: Int ->
            this.track(TrackedAction.SetDuration, listOf(value))
            opus_manager.set_duration(beat_key, position, max(1, value))
        }
    }

    private fun generate_effect_menu_option(ctl_type: EffectType, icon_id: Int): Pair<EffectType, @Composable () -> Unit> {
        return Pair(
            ctl_type,
            {
                Row(modifier = Modifier.height(dimensionResource(R.dimen.dialog_menu_line_height))) {
                    Icon(
                        modifier = Modifier.fillMaxHeight(),
                        painter = painterResource(icon_id),
                        contentDescription = ctl_type.name // TODO: extract string resource
                    )
                    Text(ctl_type.name)
                }
            }
        )
    }

    fun show_hidden_line_controller(forced_value: EffectType? = null) {
        val opus_manager = this.get_opus_manager()
        val options = mutableListOf<Pair<EffectType, @Composable () -> Unit>>( )
        val cursor = opus_manager.cursor

        for ((ctl_type, icon_id) in OpusLayerInterface.line_controller_domain) {
            if (opus_manager.is_line_ctl_visible(ctl_type, cursor.channel, cursor.line_offset)) continue
            options.add(this.generate_effect_menu_option(ctl_type, icon_id))

        }

        this.dialog_popup_menu(R.string.show_line_controls, options, stub_output = forced_value) { ctl_type: EffectType ->
            this.track(TrackedAction.ShowLineController, ActionTracker.string_to_ints(ctl_type.name))
            opus_manager.toggle_line_controller_visibility(ctl_type, cursor.channel, cursor.line_offset)
        }
    }

    fun show_hidden_channel_controller(forced_value: EffectType? =  null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val options = mutableListOf<Pair<EffectType, @Composable () -> Unit>>( )

        for ((ctl_type, icon_id) in OpusLayerInterface.channel_controller_domain) {
            if (opus_manager.is_channel_ctl_visible(ctl_type, cursor.channel)) continue
            options.add(this.generate_effect_menu_option(ctl_type, icon_id))
        }

        this.dialog_popup_menu(R.string.show_channel_controls, options, stub_output = forced_value) { ctl_type: EffectType ->
            this.track(TrackedAction.ShowChannelController, ActionTracker.string_to_ints(ctl_type.name))
            opus_manager.toggle_channel_controller_visibility(ctl_type, cursor.channel)
        }
    }

    fun show_hidden_global_controller(forced_value: EffectType? =  null) {
        val opus_manager = this.get_opus_manager()
        val options = mutableListOf<Pair<EffectType, @Composable () -> Unit>>( )

        for ((ctl_type, icon_id) in OpusLayerInterface.global_controller_domain) {
            if (opus_manager.is_global_ctl_visible(ctl_type)) continue
            options.add(this.generate_effect_menu_option(ctl_type, icon_id))
        }

        this.dialog_popup_menu(R.string.show_global_controls, options, stub_output = forced_value) { ctl_type: EffectType ->
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
        this.track(TrackedAction.SetOffset, listOf(new_offset))

        val opus_manager = this.get_opus_manager()
        opus_manager.set_note_offset_at_cursor(new_offset)

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        this.play_event(beat_key.channel, event_note)
    }

    fun set_octave(new_octave: Int) {
        this.track(TrackedAction.SetOctave, listOf(new_octave))

        val opus_manager = this.get_opus_manager()
        opus_manager.set_note_octave_at_cursor(new_octave)

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        this.play_event(beat_key.channel, event_note)
    }

    private fun play_event(channel: Int, event_value: Int, velocity: Float = .5F) {
        if (event_value < 0) return // No sound to play

        val opus_manager = this.get_opus_manager()
        val midi_channel = opus_manager.get_midi_channel(channel)

        val radix = opus_manager.get_radix()
        val (note, bend) = if (opus_manager.is_percussion(channel)) { // Ignore the event data and use percussion map
            Pair(event_value + 27, 0)
        } else {
            val octave = event_value / radix
            val offset = opus_manager.tuning_map[event_value % radix]

            val transpose_offset = 12.0 * opus_manager.transpose.first.toDouble() / opus_manager.transpose.second.toDouble()
            val std_offset = 12.0 * offset.first.toDouble() / offset.second.toDouble()

            val bend = (((std_offset - floor(std_offset)) + (transpose_offset - floor(transpose_offset))) * 512.0).toInt()
            val new_note = (octave * 12) + std_offset.toInt() + transpose_offset.toInt() + 21

            Pair(new_note, bend)
        }

        if (note > 127) return

        val audio_interface = this.vm_controller.audio_interface
        if (audio_interface.has_soundfont()) {
            audio_interface.play_feedback(midi_channel, note, bend, (velocity * 127F).toInt() shl 8)
        } else {
            TODO()
            // try {
            //     // this._midi_feedback_dispatcher.play_note(
            //     //     midi_channel,
            //     //     note,
            //     //     bend,
            //     //     (velocity * 127F).toInt(),
            //     //     !opus_manager.is_tuning_standard()
            //     // )
            // } catch (_: VirtualMidiInputDevice.DisconnectedException) {
            //     // Feedback shouldn't be necessary here. But i'm sure that'll come back to bite me
            // }
        }
    }

    fun adjust_selection(amount: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (amount == null) {
            this.vm_top.create_dialog { close ->
                @Composable {
                    Text("TODO")
                }
            }
        } else {
            this.track(TrackedAction.AdjustSelection, listOf(amount))
            opus_manager.offset_selection(amount)
        }
    }

    fun unset() {
        this.track(TrackedAction.Unset)
        this.vm_controller.opus_manager.unset()
    }

    fun unset_root() {
        this.track(TrackedAction.UnsetRoot)
        val opus_manager = this.vm_controller.opus_manager
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
        val opus_manager = this.get_opus_manager()
        opus_manager.remove_at_cursor()
    }

    fun insert_leaf(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()

        this.dialog_number_input(R.string.dlg_insert, 1, 29, stub_output = repeat) { count: Int ->
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

        if (opus_manager.get_tree(current_tree_position.first, current_tree_position.second).has_event()) {
            opus_manager.unset()
        } else {
            opus_manager.set_percussion_event_at_cursor()
            // TODO:
        //  val event_note = opus_manager.get_percussion_instrument(beat_key.channel, beat_key.line_offset)
        //   this.get_activity().play_event(beat_key.channel, event_note)
        }
    }

    fun set_percussion_instrument(channel: Int, line_offset: Int, instrument: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (instrument != null) {
            this.track(TrackedAction.SetPercussionInstrument, listOf(channel, line_offset, instrument))
            opus_manager.percussion_set_instrument(channel, line_offset, instrument)
            return
        }

        this.vm_top.create_dialog { close ->
            val options = mutableListOf<Pair<Int, @Composable () -> Unit>>()
            val preset = opus_manager.get_channel_instrument(channel)
            val instruments = opus_manager.vm_state.get_available_instruments(preset)
            for ((name, index) in instruments) {
                options.add(Pair(index, { Text("$index: $name") }))
            }
            val current_instrument = (opus_manager.get_channel(channel).lines[line_offset] as OpusLinePercussion).instrument
            @Composable {
                Row { DialogSTitle(R.string.dropdown_choose_instrument, modifier = Modifier.weight(1F)) }
                Row {
                    UnSortableMenu(options, default_value = current_instrument) { value ->
                        this@ActionTracker.track(TrackedAction.SetPercussionInstrument, listOf(channel, line_offset, value))
                        opus_manager.percussion_set_instrument(channel, line_offset, value)
                        close()
                    }
                }
                DialogBar(neutral = close)
            }
        }
    }

    fun set_channel_preset(channel: Int, instrument: Pair<Int, Int>? = null) {
        val opus_manager = this.get_opus_manager()
        if (instrument != null) {
            this.track(TrackedAction.SetChannelInstrument, listOf(channel, instrument.first, instrument.second))
            opus_manager.channel_set_instrument(channel, instrument)
            return
        }
        fun padded_hex(i: Int): String {
            var s = Integer.toHexString(i)
            while (s.length < 2) {
                s = "0$s"
            }
            return s.uppercase()
        }

        val default = this.get_opus_manager().get_channel_instrument(channel)
        this.vm_top.create_dialog { close ->
            val preset_names =  mutableListOf<Triple<Int, Int, String>>()
            val options = mutableListOf<Pair<Pair<Int, Int>, @Composable () -> Unit>>()
            val is_percussion = opus_manager.is_percussion(channel)
            for ((bank, bank_map) in opus_manager.vm_state.preset_names.toSortedMap()) {
                if (is_percussion && bank != 128) continue
                if (!this.vm_top.configuration.allow_std_percussion && !is_percussion && bank == 128) continue
                for ((program, name) in bank_map.toSortedMap()) {
                    preset_names.add(Triple(bank, program, name))
                    options.add(
                        Pair(
                            Pair(bank, program),
                            {
                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${padded_hex(bank)} | ${padded_hex(program)}")
                                    Text(name,
                                        modifier = Modifier.weight(1F),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        )
                    )
                }
            }

            @Composable {
                val sort_options = listOf(
                    Pair(R.string.sort_option_bank) { a: Int, b: Int -> preset_names[a].first.compareTo(preset_names[b].first) },
                    Pair(R.string.sort_option_program) { a: Int, b: Int -> preset_names[a].second.compareTo(preset_names[b].second) },
                    Pair(R.string.sort_option_abc) { a: Int, b: Int -> preset_names[a].third.compareTo(preset_names[b].third) }
                )
                Row {
                    SText(R.string.dropdown_choose_instrument)
                }
                Row(Modifier.weight(1F)) {
                    SortableMenu(options, sort_options = sort_options, 0, default) { instrument ->
                        close()
                        opus_manager.channel_set_instrument(channel, instrument)
                    }
                }
                DialogBar(neutral = close)
            }
        }

        // val activity = this.get_activity()
        // val supported_instrument_names = activity.get_supported_preset_names()
        // val sorted_keys = supported_instrument_names.keys.toList().sortedBy {
        //     it.first + (it.second * 128)
        // }

        // val opus_manager = this.get_opus_manager()
        // val is_percussion = opus_manager.is_percussion(channel)
        // val default_position = opus_manager.get_channel_instrument(channel)

        // val current_instrument_supported = sorted_keys.contains(default_position)

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
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(R.string.dlg_insert_lines, 1, 9, stub_output = count) { i: Int ->
            this.track(TrackedAction.InsertLine, listOf(i))
            opus_manager.insert_line_at_cursor(i)
        }
    }

    fun remove_line(count: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val lines = opus_manager.get_all_channels()[opus_manager.cursor.channel].size
        val max_lines = Integer.min(lines - 1, lines - opus_manager.cursor.line_offset)
        this.dialog_number_input(R.string.dlg_remove_lines, 1, max_lines, stub_output = count) { i: Int ->
            this.track(TrackedAction.RemoveLine, listOf(i))
            opus_manager.remove_line_at_cursor(i)
        }
    }

    fun set_percussion_instrument(value: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val default_instrument = opus_manager.get_percussion_instrument(cursor.channel, cursor.line_offset)

        val options = mutableListOf<Pair<Int, @Composable () -> Unit>>()
        // TODO()
        this.dialog_popup_menu(R.string.dropdown_choose_percussion, options, default_instrument, stub_output = value) { value: Int ->
            this.track(TrackedAction.SetPercussionInstrument, listOf(value))
            opus_manager.set_percussion_instrument(value)
            // TODO
            // main.play_event(cursor.channel, value)
        }
    }

    fun insert_beat(beat: Int, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.dialog_number_input(R.string.dlg_insert_beats, 1, 4096, stub_output = repeat) { count: Int ->
            this.track(TrackedAction.InsertBeatAt, listOf(beat, count))
            opus_manager.insert_beats(beat, count)
        }
    }
    fun append_beats(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.insert_beat(opus_manager.length, repeat)
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

        val opus_manager = this.get_opus_manager()
        opus_manager.set_relative_mode(mode)
        when (mode) {
            RelativeInputMode.Absolute -> opus_manager.convert_event_to_absolute()
            RelativeInputMode.Positive -> opus_manager.convert_event_to_relative()
            RelativeInputMode.Negative -> opus_manager.convert_event_to_relative()
        }
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
            this.vm_top.create_dialog { close ->
                @Composable {
                    Row { DialogTitle(title, modifier = Modifier.weight(1F)) }
                    DialogBar(
                        neutral = close,
                        positive = {
                            close()
                            callback()
                        }
                    )
                }
            }
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
            this.vm_top.create_dialog { close ->
                @Composable {
                    val value: MutableState<Int> = remember { mutableIntStateOf(default ?: min_value) }

                    Row { DialogSTitle(title_string_id, modifier = Modifier.weight(1F)) }
                    Row {
                        IntegerInput(
                            value = value,
                            minimum = min_value,
                            maximum = max_value,
                            modifier = Modifier.fillMaxWidth()
                        ) { new_value ->
                            close()
                            callback(new_value)
                        }
                    }

                    DialogBar(
                        neutral = close,
                        positive = {
                            close()
                            callback(value.value)
                        }
                    )
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

    private fun needs_save(): Boolean {
        val opus_manager = this.get_opus_manager()

        val active_project = this.vm_controller.active_project ?: return !opus_manager.history_cache.is_empty()
        val other = this.vm_top.project_manager?.open_project(active_project)
        return (opus_manager as OpusLayerBase) != other
    }

    fun save_before(stub_output: Boolean? = null, callback: (Boolean) -> Unit) {
        if (!this.needs_save()) {
            callback(false)
            return
        }

        if (stub_output == null) {
            this.vm_top.create_dialog { close ->
                @Composable {
                    Row { DialogSTitle(R.string.dialog_save_warning_title, modifier = Modifier.weight(1F)) }
                    Row {
                        Button(
                            modifier = Modifier.weight(1F),
                            onClick = {
                                close()
                                callback(false)

                            },
                            content = { SText(R.string.no) }
                        )
                        TextButton(
                            modifier = Modifier.weight(1F),
                            onClick = { close() },
                            content = { SText(android.R.string.cancel) }
                        )
                        Button(
                            modifier = Modifier.weight(1F),
                            onClick = {
                                close()
                                this@ActionTracker.save()
                                callback(true)
                            },
                            content = { SText(android.R.string.ok) }
                        )
                    }
                }
            }
        } else {
            if (stub_output) {
                this.save()
            }
            callback(stub_output)
        }
    }


    /**
     * wrapper around MainActivity::dialog_popup_menu
     * will subvert popup on replay
     */
    private fun <T> dialog_popup_menu(title: Int, options: List<Pair<T, @Composable () -> Unit>>, default: T? = null, stub_output: T? = null, callback: (value: T) -> Unit) {
        if (stub_output != null) return callback(stub_output)

        this.vm_top.create_dialog { close ->
            @Composable {
                Row { DialogSTitle(title, modifier = Modifier.weight(1F)) }
                Row {
                    UnSortableMenu(options, default, callback)
                }
                DialogBar(neutral = close)
            }
        }
        // if (stub_output != null) {
        //     callback(-1, stub_output)
        // } else {
        //     val activity = this.get_activity()
        //     activity.dialog_popup_menu(title, options, default, callback)
        // }
    }

    private fun dialog_text_popup(title: Int, default: String? = null, stub_output: String? = null, callback: (String) -> Unit) {
        if (stub_output != null) return callback(stub_output)

        this.vm_top.create_dialog { close ->
            @Composable {
                val value = remember { mutableStateOf(default ?: "") }
                Row { DialogSTitle(title, modifier = Modifier.weight(1F)) }
                Row {
                    TextInput(
                        modifier = Modifier,
                        input = value,
                        callback = callback
                    )
                }
                DialogBar(
                    neutral = close,
                    positive = {
                        close()
                        callback(value.value)
                    }
                )
            }
        }
    }

    private fun dialog_name_and_notes_popup(default: Pair<String, String>? = null, stub_output: Pair<String, String>? = null, callback: (String, String) -> Unit) {
        if (stub_output != null) return callback(stub_output.first, stub_output.second)

        this.vm_top.create_dialog { close ->
            @Composable  {
               //      Row {
               //          OutlinedTextField(
               //              maxLines = 1
               //          )
               //      }
               //      Row {
               //          OutlinedTextField(
               //          )
               //      }
            }
        }
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
        return this.vm_controller.opus_manager
    }

    fun play_opus(scope: CoroutineScope) {
        this.vm_controller.playback_device?.play_opus(0)
    }
    fun stop_opus() {
        this.vm_controller.playback_device?.kill()
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
            TrackedAction.CursorSelectChannel -> {
                this.cursor_select_channel(integers[0]!!)
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
                this._move_line_ctl_to_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.MoveChannelCtlToBeat -> {
                this._move_channel_ctl_to_beat(
                    integers[0]!!,
                    integers[1]!!
                )
            }
            TrackedAction.MoveGlobalCtlToBeat -> {
                this._move_global_ctl_to_beat(integers[0]!!)
            }
            TrackedAction.MoveSelectionToBeat -> {
                this._move_selection_to_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.CopyLineCtlToBeat -> {
                this._copy_line_ctl_to_beat(
                    BeatKey(
                        integers[0]!!,
                        integers[1]!!,
                        integers[2]!!
                    )
                )
            }
            TrackedAction.CopyChannelCtlToBeat -> {
                this._copy_channel_ctl_to_beat(
                    integers[0]!!,
                    integers[1]!!
                )
            }
            TrackedAction.CopyGlobalCtlToBeat -> {
                this._copy_global_ctl_to_beat(integers[0]!!)
            }
            TrackedAction.CopySelectionToBeat -> {
                this._copy_selection_to_beat(
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
            TrackedAction.SetPercussionInstrument -> {
                this.set_percussion_instrument(integers[0]!!, integers[1]!!, integers[2])
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
            TrackedAction.SetChannelInstrument -> {
                this.set_channel_preset(integers[0]!!, Pair(integers[1]!!, integers[2]!!))
            }
            TrackedAction.RemoveBeat -> {
                this.remove_beat_at_cursor(integers[0])
            }
            TrackedAction.InsertBeat -> {
                this.insert_beat_after_cursor(integers[0]!!)
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
                TODO()
            }
            TrackedAction.DrawerClose -> {
                TODO()
            }
            TrackedAction.MergeSelectionIntoBeat -> {
                this._merge_selection_into_beat(
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
        this.vm_controller.move_mode.value = mode
        this.vm_top.configuration.move_mode = mode
        this.vm_top.save_configuration()
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
                opus_manager.toggle_channel_controller_visibility(cursor.ctl_type!!, cursor.channel)
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

        this.dialog_text_popup(R.string.dialog_mark_section, opus_manager.marked_sections[use_beat], description) { result: String ->
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
       try {
           val adj_to_index = line_offset_to + if (before) 0 else 1
           if (adj_to_index == line_offset_from && channel_from == channel_to) return

           this.get_opus_manager().move_line(
               channel_from,
               line_offset_from,
               channel_to,
               adj_to_index
           )
           this.track(
               TrackedAction.MoveLine,
               listOf(channel_from, line_offset_from, channel_to, adj_to_index)
           )
       } catch (e: IncompatibleChannelException) {
           // TODO
          //  this.toast(R.string.std_percussion_swap)
       }
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
