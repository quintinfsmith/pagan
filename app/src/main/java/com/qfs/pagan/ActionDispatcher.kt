/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.composable.ColorPicker
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.composable.NumberPicker
import com.qfs.pagan.composable.TextInput
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.composable.DivisorSeparator
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.IncompatibleChannelException
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.cursor.InvalidCursorState
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import com.qfs.pagan.OpusLayerInterface as OpusManager

/**
 * Handle all (or as much of as possible) of the logic between a user action and the OpusManager.
 * Used to be used in order to record and playback actions for testing and debugging BUT is now a way to separate Compose Components from Activity
 */
class ActionDispatcher(val context: Context, var vm_controller: ViewModelEditorController) {
    class UnexpectedBranch: Exception()
    class MissingProjectManager: Exception()
    class IncompatibleEffectMerge(type_a: EffectType?, type_b: EffectType?): Exception("Can't merge types $type_a and $type_b")

    lateinit var vm_top: ViewModelPagan
    val persistent_number_input_values = HashMap<Int, Int>()

    private fun _gen_string_list(int_list: List<Int>): String {
        return int_list.joinToString(", ", "listOf(", ")")
    }

    fun attach_top_model(model: ViewModelPagan) {
        this.vm_top = model
    }

    fun apply_undo() {
        this.get_opus_manager().apply_undo()
    }
    fun apply_redo() {
        this.get_opus_manager().apply_redo()
    }

    fun move_selection_to_beat(beat_key: BeatKey) {
        when (this.vm_top.configuration.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this.vm_controller.opus_manager.move_to_beat(beat_key)
            }
            PaganConfiguration.MoveMode.COPY -> {
                this.vm_controller.opus_manager.copy_to_beat(beat_key)
            }
            PaganConfiguration.MoveMode.MERGE -> {
                this.vm_controller.opus_manager.merge_into_beat(beat_key)
            }
        }
    }

    fun save() {
        this.vm_top.project_manager?.let {
            val uri = it.save(this.vm_controller.opus_manager, this.vm_controller.active_project)
            this.vm_top.has_saved_project.value = true
            this.vm_controller.active_project = uri
            this.vm_controller.project_exists.value = true
            Toast.makeText(
                this@ActionDispatcher.context,
                R.string.feedback_project_saved,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun project_copy() {
        this.vm_controller.active_project ?: return
        this.save_before {
            val opus_manager = this.vm_controller.opus_manager
            val old_title = opus_manager.project_name
            opus_manager.set_project_name(
                if (old_title == null) null
                else this.context.resources.getString(R.string.copied_title, old_title)
            )
            this.vm_controller.active_project = null
            this.vm_controller.project_exists.value = false
            Toast.makeText(this.context, R.string.feedback_on_copy, Toast.LENGTH_SHORT).show()
        }
    }

    fun tap_leaf(beat: Int, position: List<Int>, channel: Int?, line_offset: Int?, ctl_type: EffectType?) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val selecting_range = cursor.mode == CursorMode.Range
        if (selecting_range && cursor.ctl_type == ctl_type) {
            try {
                if (ctl_type == null) {
                    this.move_selection_to_beat(BeatKey(channel!!, line_offset!!, beat))
                } else if (line_offset != null) {
                    this.move_line_ctl_to_beat(BeatKey(channel!!, line_offset, beat))
                } else if (channel != null) {
                    this.move_channel_ctl_to_beat(channel, beat)
                } else {
                    this.move_global_ctl_to_beat(beat)
                }
            } catch (e: MixedInstrumentException) {
                Toast.makeText(this.context, R.string.feedback_mixed_copy, Toast.LENGTH_SHORT).show()
            }
        } else if (ctl_type == null) {
            this.cursor_select(BeatKey(channel!!, line_offset!!, beat), position)
        } else if (line_offset != null) {
            this.cursor_select_ctl_at_line(ctl_type, BeatKey(channel!!, line_offset, beat), position)
        } else if (channel != null) {
            this.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)
        } else {
            this.cursor_select_ctl_at_global(ctl_type, beat, position)
        }
    }

    fun long_tap_leaf(beat: Int, position: List<Int>, channel: Int?, line_offset: Int?, ctl_type: EffectType?) {
        if (ctl_type == null) {
            this.cursor_select_range_next(BeatKey(channel!!, line_offset!!, beat))
        } else if (line_offset != null) {
            this.cursor_select_line_ctl_range_next(ctl_type, BeatKey(channel!!, line_offset, beat))
        } else if (channel != null) {
            this.cursor_select_channel_ctl_range_next(ctl_type, channel, beat)
        } else {
            this.cursor_select_global_ctl_range_next(ctl_type, beat)
        }
    }


    fun tap_line(channel: Int?, line_offset: Int?, ctl_type: EffectType?) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.mode) {
            CursorMode.Range -> {
                try {
                    if (ctl_type == null) this.repeat_selection_std(channel!!, line_offset!!)
                    else if (line_offset != null) this.repeat_selection_ctl_line(ctl_type, channel!!, line_offset)
                    else if (channel != null) this.repeat_selection_ctl_channel(ctl_type, channel)
                    else this.repeat_selection_ctl_global(ctl_type)
                } catch (e: Exception) {
                    this.cursor_select_line(channel, line_offset, ctl_type)
                }
            }
            CursorMode.Line -> {
                val ctl_level = if (ctl_type == null) null
                else if (channel == null) CtlLineLevel.Global
                else if (line_offset == null) CtlLineLevel.Channel
                else CtlLineLevel.Line


                if (channel == cursor.channel && line_offset == cursor.line_offset && ctl_type == cursor.ctl_type && ctl_level == cursor.ctl_level) {
                    this.cursor_select_channel(channel)
                } else {
                    this.cursor_select_line(channel, line_offset, ctl_type)
                }
            }
            CursorMode.Single,
            CursorMode.Column,
            CursorMode.Channel,
            CursorMode.Unset -> {
                this.cursor_select_line(channel, line_offset, ctl_type)
            }
        }
    }

    fun set_effect_transition(event: EffectEvent, transition: EffectTransition? = null) {
        transition?.let {
            event.transition = transition
            this.set_effect_at_cursor(event.copy())
            return
        }

        val title = R.string.dialog_transition

        val options = mutableListOf<Pair<EffectTransition, @Composable RowScope.() -> Unit>>()
        for (transition_option in OpusManager.get_available_transitions(event.event_type)) {
            options.add(
                Pair(transition_option) {
                    Icon(
                        modifier = Modifier.height(Dimensions.EffectTransitionDialogIconHeight),
                        painter = painterResource(when (transition_option) {
                            EffectTransition.Instant -> R.drawable.icon_transition_immediate
                            EffectTransition.Linear -> R.drawable.icon_transition_linear
                            EffectTransition.RInstant -> R.drawable.icon_transition_rimmediate
                            EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
                        }),
                        contentDescription = null
                    )
                    Box(
                        modifier = Modifier.weight(1F),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (transition_option) {
                                EffectTransition.Instant -> R.string.effect_transition_instant
                                EffectTransition.Linear -> R.string.effect_transition_linear
                                EffectTransition.RInstant -> R.string.effect_transition_rinstant
                                EffectTransition.RLinear -> R.string.effect_transition_rlinear_out
                            }
                        )
                    }
                }
            )
        }

        this.dialog_popup_menu(title = title, default = event.transition, options = options) { it ->
            this.set_effect_transition(event, it)
        }
    }

    fun long_tap_line(channel: Int?, line_offset: Int?, ctl_type: EffectType?, fallback: () -> Unit = {}) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        when (cursor.mode) {
            CursorMode.Range -> {
                try {
                    if (ctl_type == null) this.repeat_selection_std(channel!!, line_offset!!, -1)
                    else if (line_offset != null) this.repeat_selection_ctl_line(ctl_type, channel!!, line_offset, -1)
                    else if (channel != null) this.repeat_selection_ctl_channel(ctl_type, channel, -1)
                    else this.repeat_selection_ctl_global(ctl_type, -1)
                } catch (e: Exception) {
                    this.cursor_select_line(channel, line_offset, ctl_type)
                }
            }
            else -> fallback()
        }

    }

    fun cursor_clear() {
        this.vm_controller.opus_manager.cursor_clear()
    }

    fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        val opus_manager = this.vm_controller.opus_manager
        opus_manager.cursor_select(beat_key, position)

        val tree = opus_manager.get_tree() ?: return
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
        when (this.vm_top.configuration.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this._move_line_ctl_to_beat(beat_key)
            }
            PaganConfiguration.MoveMode.MERGE -> TODO("Merge not implemented yet")
            PaganConfiguration.MoveMode.COPY -> {
                this._copy_line_ctl_to_beat(beat_key)
            }
        }
    }

    fun _move_line_ctl_to_beat(beat_key: BeatKey) {
        this.get_opus_manager().move_line_ctl_to_beat(beat_key)
    }
    fun _copy_line_ctl_to_beat(beat_key: BeatKey) {
        this.get_opus_manager().copy_line_ctl_to_beat(beat_key)
    }
    fun move_channel_ctl_to_beat(channel: Int, beat: Int) {
        when (this.vm_top.configuration.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this._move_channel_ctl_to_beat(channel, beat)
            }
            PaganConfiguration.MoveMode.MERGE -> TODO("Merge not implemented yet")
            PaganConfiguration.MoveMode.COPY -> {
                this._copy_channel_ctl_to_beat(channel, beat)
            }
        }
    }
    fun _move_channel_ctl_to_beat(channel: Int, beat: Int) {
        this.get_opus_manager().move_channel_ctl_to_beat(channel, beat)
    }
    fun _copy_channel_ctl_to_beat(channel: Int, beat: Int) {
        this.get_opus_manager().copy_channel_ctl_to_beat(channel, beat)
    }
    fun move_global_ctl_to_beat(beat: Int) {
        when (this.vm_top.configuration.move_mode.value)  {
            PaganConfiguration.MoveMode.MOVE -> {
                this._move_global_ctl_to_beat(beat)
            }
            PaganConfiguration.MoveMode.MERGE -> TODO("Merge not implemented yet")
            PaganConfiguration.MoveMode.COPY -> {
                this._copy_global_ctl_to_beat(beat)
            }
        }
    }
    fun _move_global_ctl_to_beat(beat: Int) {
        this.get_opus_manager().move_global_ctl_to_beat(beat)
    }

    fun _copy_global_ctl_to_beat(beat: Int) {
        this.get_opus_manager().copy_global_ctl_to_beat(beat)
    }

    fun cursor_select_ctl_at_line(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        this.get_opus_manager().cursor_select_ctl_at_line(type, beat_key, position)
    }

    fun cursor_select_ctl_at_channel(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        this.get_opus_manager().cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    fun cursor_select_ctl_at_global(type: EffectType, beat: Int, position: List<Int>) {
        this.get_opus_manager().cursor_select_ctl_at_global(type, beat, position)
    }

    fun cursor_select_channel(channel: Int) {
        this.vm_controller.opus_manager.cursor_select_channel(channel)
    }

    fun cursor_select_line_std(channel: Int, line_offset: Int) {
        this.vm_controller.opus_manager.cursor_select_line(channel, line_offset)
    }

    fun cursor_select_line_ctl_line(type: EffectType, channel: Int, line_offset: Int) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.mode == CursorMode.Line && cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == CtlLineLevel.Line && type == cursor.ctl_type) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_line_ctl_line(type, channel, line_offset)
        }
    }

    fun cursor_select_channel_ctl_line(type: EffectType, channel: Int) {
        val opus_manager = this.get_opus_manager()

        val cursor = opus_manager.cursor
        if (cursor.mode == CursorMode.Line && cursor.channel == channel && cursor.ctl_level == CtlLineLevel.Channel && type == cursor.ctl_type) {
            opus_manager.cursor_select_channel(channel)
        } else {
            opus_manager.cursor_select_channel_ctl_line(type, channel)
        }
    }

    fun cursor_select_global_ctl_line(type: EffectType) {
        val opus_manager = this.get_opus_manager()
        opus_manager.cursor_select_global_ctl_line(type)
    }

    fun repeat_selection_std(channel: Int, line_offset: Int, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val (first_key, second_key) = cursor.get_ordered_range()!!

        // a value of negative 1 means use default value, where a null would show the dialog
        if (repeat != null && repeat != -1) {
            if (first_key != second_key) {
                opus_manager.overwrite_beat_range_horizontally(
                    channel,
                    line_offset,
                    first_key,
                    second_key,
                    repeat
                )
            } else if (opus_manager.is_percussion(first_key.channel) == opus_manager.is_percussion(channel)) {
                opus_manager.overwrite_line(channel, line_offset, first_key, repeat)
            }

            return
        }

        val default_count = ceil((opus_manager.length.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
        if (repeat == -1) {
            this.repeat_selection_std(channel, line_offset, default_count)
        } else if (first_key != second_key) {
            this.dialog_number_input(R.string.repeat_selection, 1, default = default_count) { repeat: Int ->
                this.repeat_selection_std(channel, line_offset, repeat)
            }
        } else if (opus_manager.is_percussion(first_key.channel) == opus_manager.is_percussion(channel)) {
            this.dialog_number_input(R.string.repeat_selection, 1, default = default_count) { repeat: Int ->
                this.repeat_selection_std(channel, line_offset, repeat)
            }
        } else {
            throw MixedInstrumentException(first_key, second_key)
        }
    }

    private fun repeat_selection_ctl_line(type: EffectType, channel: Int, line_offset: Int, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.ctl_type != type) throw IncompatibleEffectMerge(cursor.ctl_type, type)

        val (first, second) = cursor.get_ordered_range()!!

        if (repeat != null && repeat != -1) {
            if (first != second) {
                when (cursor.ctl_level!!) {
                    CtlLineLevel.Line -> opus_manager.controller_line_overwrite_range_horizontally(type, channel, line_offset, first, second, repeat)
                    CtlLineLevel.Channel -> opus_manager.controller_channel_to_line_overwrite_range_horizontally(type, channel, line_offset, first.channel, first.beat, second.beat, repeat)
                    CtlLineLevel.Global -> opus_manager.controller_global_to_line_overwrite_range_horizontally(type, channel, line_offset, first.beat, second.beat, repeat)
                }
            } else {
                when (cursor.ctl_level!!) {
                    CtlLineLevel.Line -> opus_manager.controller_line_overwrite_line(type, channel, line_offset, first, repeat)
                    CtlLineLevel.Channel -> opus_manager.controller_channel_to_line_overwrite_line(type, channel, line_offset, first.channel, first.beat, repeat)
                    CtlLineLevel.Global -> opus_manager.controller_global_to_line_overwrite_line(type, first.beat, channel, line_offset, repeat)
                }
            }

            return
        }

        val default_count = ceil((opus_manager.length.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        if (repeat == -1) {
            this.repeat_selection_ctl_line(type, channel, line_offset, default_count)
        } else {
            this.dialog_number_input(R.string.repeat_selection, 1, default = default_count) { repeat: Int ->
                this.repeat_selection_ctl_line(type, channel, line_offset, repeat)
            }
        }
    }

    private fun repeat_selection_ctl_channel(type: EffectType, channel: Int, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (cursor.ctl_type != type) throw IncompatibleEffectMerge(cursor.ctl_type, type)

        val (first, second) = cursor.get_ordered_range()!!
        if (repeat != null && repeat != -1) {
            when (cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first != second) {
                        opus_manager.controller_line_to_channel_overwrite_range_horizontally(type, channel, first, second, repeat)
                    } else {
                        opus_manager.controller_line_to_channel_overwrite_line(type, channel, first, repeat)
                    }
                }
                CtlLineLevel.Channel -> {
                    if (first != second) {
                        opus_manager.controller_channel_overwrite_range_horizontally(type, channel, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_channel_overwrite_line(type, channel, first.channel, first.beat, repeat)
                    }
                }
                CtlLineLevel.Global -> {
                    if (first != second) {
                        opus_manager.controller_global_to_channel_overwrite_range_horizontally(type, first.channel, first.beat, second.beat, repeat)
                    } else {
                        opus_manager.controller_global_to_channel_overwrite_line(type, channel, first.beat, repeat)
                    }
                }
                null -> throw UnexpectedBranch()
            }
            return
        }

        val default_count = ceil((opus_manager.length.toFloat() - first.beat) / (second.beat - first.beat + 1).toFloat()).toInt()
        if (repeat == -1) {
            this.repeat_selection_ctl_channel(type, channel, default_count)
        } else {
            this.dialog_number_input(R.string.repeat_selection, 1, default = default_count) { repeat: Int ->
                this.repeat_selection_ctl_channel(type,  channel, repeat)
            }
        }
    }

    private fun repeat_selection_ctl_global(type: EffectType, repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        if (type != cursor.ctl_type) throw IncompatibleEffectMerge(type, cursor.ctl_type)

        val (first_key, second_key) = cursor.get_ordered_range()!!

        if (repeat != null && repeat != -1) {
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
                null -> throw UnexpectedBranch()
            }
            return
        }

        val default_count = ceil((opus_manager.length.toFloat() - first_key.beat) / (second_key.beat - first_key.beat + 1).toFloat()).toInt()
        if (repeat == -1) {
            this.repeat_selection_ctl_global(type, default_count)
        } else {
            this.dialog_number_input(R.string.repeat_selection, 1, default = default_count) {
                this.repeat_selection_ctl_global(type, it)
            }
        }
    }

    fun cursor_select_column(beat: Int? = null) {
        val opus_manager = this.get_opus_manager()

        beat?.let {
            opus_manager.cursor_select_column(beat)
            return
        }

        this.vm_top.create_dialog { close ->
            @Composable {
                val slider_position = remember {
                    mutableFloatStateOf(
                        when (opus_manager.cursor.mode) {
                            CursorMode.Column,
                            CursorMode.Single -> {
                                opus_manager.cursor.beat
                            }

                            CursorMode.Range -> {
                                opus_manager.cursor.get_ordered_range()!!.first.beat
                            }

                            CursorMode.Channel,
                            CursorMode.Unset,
                            CursorMode.Line -> {
                                opus_manager.vm_state.scroll_state_x.value.firstVisibleItemIndex
                            }
                        }.toFloat()
                    )
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DialogTitle(
                        stringResource(
                            R.string.label_shortcut_scrollbar,
                            slider_position.floatValue.toInt(),
                            opus_manager.length - 1
                        )
                    )
                }

                val default_colors = SliderDefaults.colors()
                val colors = SliderColors(
                    thumbColor = default_colors.thumbColor,
                    activeTrackColor = default_colors.activeTrackColor,
                    activeTickColor = default_colors.inactiveTickColor,
                    inactiveTrackColor = default_colors.inactiveTrackColor,
                    inactiveTickColor = default_colors.activeTickColor,
                    disabledThumbColor = default_colors.disabledThumbColor,
                    disabledActiveTrackColor = default_colors.disabledActiveTrackColor,
                    disabledActiveTickColor = default_colors.disabledActiveTickColor,
                    disabledInactiveTrackColor = default_colors.disabledInactiveTrackColor,
                    disabledInactiveTickColor = default_colors.disabledInactiveTickColor
                )
                Slider(
                    value = slider_position.value,
                    steps = opus_manager.length,
                    colors = colors,
                    valueRange = 0F .. (opus_manager.length - 1).toFloat(),
                    onValueChange = { value ->
                        slider_position.value = value
                        this@ActionDispatcher.cursor_select_column(value.toInt())
                    }
                )

                if (opus_manager.marked_sections.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val expanded = remember { mutableStateOf(false) }
                        Box {
                            Button(
                                onClick = { expanded.value = !expanded.value },
                                content = { Text(R.string.jump_to_section) }
                            )
                            DropdownMenu(
                                onDismissRequest = { expanded.value = false },
                                expanded = expanded.value
                            ) {
                                var section_index = 0
                                for ((i, tag) in opus_manager.marked_sections.toList().sortedBy { it.first }) {
                                    DropdownMenuItem(
                                        onClick = {
                                            close()
                                            expanded.value = false
                                            this@ActionDispatcher.cursor_select_column(i)
                                        },
                                        text = {
                                            if (tag == null) {
                                                Text(stringResource(R.string.section_spinner_item, i, section_index))
                                            } else {
                                                Text("${"%02d".format(i)}: $tag")
                                            }
                                        }
                                    )
                                    section_index++
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun cursor_select_range_next(beat_key: BeatKey) {
        this.vm_controller.opus_manager.cursor_select_range_next(beat_key)
    }
    fun cursor_select_line_ctl_range_next(type: EffectType, beat_key: BeatKey) {
        this.vm_controller.opus_manager.cursor_select_line_ctl_range_next(type, beat_key)
    }
    fun cursor_select_channel_ctl_range_next(type: EffectType, channel: Int, beat: Int) {
        this.vm_controller.opus_manager.cursor_select_channel_ctl_range_next(type, channel, beat)
    }
    fun cursor_select_global_ctl_range_next(type: EffectType, beat: Int) {
        this.vm_controller.opus_manager.cursor_select_global_ctl_range_next(type, beat)
    }
    fun cursor_select_range(first_key: BeatKey, second_key: BeatKey) {
        this.vm_controller.opus_manager.cursor_select_range(first_key, second_key)
    }
    fun cursor_select_line_ctl_range(type: EffectType, first_key: BeatKey, second_key: BeatKey) {
        this.vm_controller.opus_manager.cursor_select_line_ctl_range(type, first_key, second_key)
    }
    fun cursor_select_channel_ctl_range(type: EffectType, channel: Int, first_beat: Int, second_beat: Int) {
        this.vm_controller.opus_manager.cursor_select_channel_ctl_range(type, channel, first_beat, second_beat)
    }
    fun cursor_select_global_ctl_range(type: EffectType, first_beat: Int, second_beat: Int) {
        this.vm_controller.opus_manager.cursor_select_global_ctl_range(type, first_beat, second_beat)
    }

    fun new_project() {
        val opus_manager = this.get_opus_manager()
        opus_manager.project_change_new()

        for ((c, channel) in opus_manager.channels.enumerate()) {
            if (!opus_manager.is_percussion(c)) continue
            val i = this.vm_controller.audio_interface.get_minimum_instrument_index(channel.get_preset())
            for (l in 0 until opus_manager.get_channel(c).size) {
                opus_manager.percussion_set_instrument(c, l, max(0, i - 27))
            }
        }

        this.vm_controller.update_soundfont_instruments()
        opus_manager.clear_history()
    }

    fun <T: EffectEvent> set_initial_effect(type: EffectType, event: T, channel: Int?, line_offset: Int?, lock_cursor: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        val callback = {
            if (channel == null) {
                opus_manager.controller_global_set_initial_event(type, event)
            } else if (line_offset == null) {
                opus_manager.controller_channel_set_initial_event(type, channel, event)
            } else {
                opus_manager.controller_line_set_initial_event(type, channel, line_offset, event)
            }
        }

        if (lock_cursor) {
            opus_manager.lock_cursor(callback)
        } else {
            callback()
        }
    }

    fun <T: EffectEvent> set_effect(type: EffectType, event: T, channel: Int?, line_offset: Int?, beat: Int, position: List<Int>, lock_cursor: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        val callback = {
            if (channel == null) {
                opus_manager.controller_global_set_event(type, beat, position, event)
            } else if (line_offset == null) {
                opus_manager.controller_channel_set_event(type, channel, beat, position, event)
            } else {
                opus_manager.controller_line_set_event(type, BeatKey(channel, line_offset, beat), position, event)
            }
        }

        if (lock_cursor) {
            opus_manager.lock_cursor(callback)
        } else {
            callback()
        }
    }

    fun <T: EffectEvent> set_effect_at_cursor(event: T) {
        val opus_manager = this.get_opus_manager()
        opus_manager.set_event_at_cursor(event)
    }

    fun set_duration(duration: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        duration?.let {
            if (cursor.mode != CursorMode.Single) throw InvalidCursorState()
            when (cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    val (beat_key, position) = opus_manager.controller_line_get_actual_position(
                        cursor.ctl_type!!,
                        cursor.get_beatkey(),
                        cursor.get_position()
                    )
                    opus_manager.set_duration(cursor.ctl_type!!, beat_key, position, duration)
                }
                CtlLineLevel.Channel -> {
                    val (beat, position) = opus_manager.controller_channel_get_actual_position(
                        cursor.ctl_type!!,
                        cursor.channel,
                        cursor.beat,
                        cursor.get_position()
                    )
                    opus_manager.set_duration(cursor.ctl_type!!, cursor.channel, beat, position, duration)
                }
                CtlLineLevel.Global -> {
                    val (beat, position) = opus_manager.controller_global_get_actual_position(cursor.ctl_type!!, cursor.beat, cursor.get_position())
                    opus_manager.set_duration(cursor.ctl_type!!, beat, position, duration)
                }
                null -> {
                    val (beat_key, position) = opus_manager.get_actual_position(
                        cursor.get_beatkey(),
                        cursor.get_position()
                    )
                    opus_manager.set_duration(beat_key, position, max(1, it))
                }
            }
            return
        }

        val event_duration = opus_manager.get_event_at_cursor()?.duration ?: 1
        this.dialog_number_input(R.string.dlg_duration, 1, default = event_duration) {
            this.set_duration(it)
        }
    }

    private fun generate_effect_menu_option(ctl_type: EffectType, icon_id: Int): Pair<EffectType, @Composable RowScope.() -> Unit> {
        return Pair(ctl_type) {
            Icon(
                modifier = Modifier.width(Dimensions.EffectDialogIconWidth),
                painter = painterResource(icon_id),
                contentDescription = stringResource(
                    when (ctl_type) {
                        EffectType.Tempo -> R.string.ctl_desc_tempo
                        EffectType.Velocity -> R.string.ctl_desc_velocity
                        EffectType.Volume -> R.string.ctl_desc_volume
                        EffectType.Pan -> R.string.ctl_desc_pan
                        EffectType.Delay -> R.string.ctl_desc_delay
                        EffectType.LowPass -> TODO()
                        EffectType.Reverb -> TODO()
                    }
                )
            )
            Text(
                ctl_type.name,
                Modifier.weight(1F)
            )
        }
    }

    fun show_hidden_line_controller(forced_value: EffectType? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        forced_value?.let {
            opus_manager.toggle_line_controller_visibility(it, cursor.channel, cursor.line_offset)
            return
        }

        val options = mutableListOf<Pair<EffectType, @Composable RowScope.() -> Unit>>( )
        for ((ctl_type, icon_id) in OpusLayerInterface.line_controller_domain) {
            if (opus_manager.is_line_ctl_visible(ctl_type, cursor.channel, cursor.line_offset)) continue
            options.add(this.generate_effect_menu_option(ctl_type, icon_id))
        }

        this.dialog_popup_menu(R.string.show_line_controls, options) { ctl_type: EffectType ->
            this.show_hidden_line_controller(ctl_type)
        }
    }

    fun show_hidden_channel_controller(forced_value: EffectType? =  null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        forced_value?.let {
            opus_manager.toggle_channel_controller_visibility(it, cursor.channel)
            return
        }

        val options = mutableListOf<Pair<EffectType, @Composable RowScope.() -> Unit>>( )
        for ((ctl_type, icon_id) in OpusLayerInterface.channel_controller_domain) {
            if (opus_manager.is_channel_ctl_visible(ctl_type, cursor.channel)) continue
            options.add(this.generate_effect_menu_option(ctl_type, icon_id))
        }

        this.dialog_popup_menu(R.string.show_channel_controls, options) {
            this.show_hidden_channel_controller(it)
        }
    }

    fun show_hidden_global_controller(forced_value: EffectType? =  null) {
        val opus_manager = this.get_opus_manager()

        forced_value?.let {
            opus_manager.toggle_global_controller_visibility(it)
            return
        }

        val options = mutableListOf<Pair<EffectType, @Composable RowScope.() -> Unit>>( )
        for ((ctl_type, icon_id) in OpusLayerInterface.global_controller_domain) {
            if (opus_manager.is_global_ctl_visible(ctl_type)) continue
            options.add(this.generate_effect_menu_option(ctl_type, icon_id))
        }

        this.dialog_popup_menu(R.string.show_global_controls, options) {
            this.show_hidden_global_controller(it)
        }
    }

    fun split(split: Int? = null) {
        split?.let {
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            when (cursor.ctl_level) {
                CtlLineLevel.Global -> opus_manager.controller_global_split_tree(cursor.ctl_type!!, cursor.beat, cursor.get_position(), it)
                CtlLineLevel.Channel -> opus_manager.controller_channel_split_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position(), it)
                CtlLineLevel.Line -> opus_manager.controller_line_split_tree(cursor.ctl_type!!, cursor.get_beatkey(), cursor.get_position(), it)
                null -> opus_manager.split_tree_at_cursor(it)
            }
            return
        }

        this.dialog_number_input(R.string.dlg_split, 2, 32) {
            this.split(it)
        }
    }

    fun set_offset(new_offset: Int, mode: RelativeInputMode) {
        val opus_manager = this.get_opus_manager()
        opus_manager.set_note_offset_at_cursor(new_offset, mode)

        val beat_key = opus_manager.cursor.get_beatkey()
        val position = opus_manager.cursor.get_position()
        val event_note = opus_manager.get_absolute_value(beat_key, position) ?: return
        this.play_event(beat_key.channel, event_note)
    }

    fun set_octave(new_octave: Int, mode: RelativeInputMode) {
        val opus_manager = this.get_opus_manager()
        opus_manager.set_note_octave_at_cursor(new_octave, mode)

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
        if (this.vm_controller.active_midi_device != null) {
            try {
                this.vm_controller.virtual_midi_device.play_note(
                    midi_channel,
                    note,
                    bend,
                    (velocity * 127F).toInt(),
                    !this.get_opus_manager().is_tuning_standard()
                )
            } catch (_: VirtualMidiInputDevice.DisconnectedException) {
                // Feedback shouldn't be necessary here. But i'm sure that'll come back to bite me
            }
        } else if (audio_interface.has_soundfont()) {
            audio_interface.play_feedback(midi_channel, note, bend, (velocity * 127F).toInt() shl 8)
        }
    }

    fun adjust_selection(amount: Int? = null) {
        val opus_manager = this.get_opus_manager()

        amount?.let {
            opus_manager.offset_selection(amount)
            return
        }

        val radix = opus_manager.get_radix()
        this.vm_top.create_dialog { close ->
            @Composable {
                val octave = remember { mutableIntStateOf(0) }
                val offset = remember { mutableIntStateOf(0) }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    val max_abs = radix - 1
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(R.string.offset_dialog_octaves)
                        NumberPicker(Modifier, -7..7, octave)
                    }
                    Spacer(Modifier.width(Dimensions.DialogAdjustInnerSpace))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(R.string.offset_dialog_offset)
                        NumberPicker(Modifier, 0 - max_abs .. max_abs, offset)
                    }
                }
                DialogBar(
                    positive = {
                        close()
                        this@ActionDispatcher.adjust_selection((octave.intValue * radix) + offset.intValue)
                    },
                    neutral = close
                )
            }
        }
    }

    fun unset() {
        this.vm_controller.opus_manager.unset()
    }

    fun unset_root() {
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
        val opus_manager = this.get_opus_manager()
        opus_manager.remove_at_cursor()
    }

    fun insert_leaf(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        repeat?.let {
            val position = opus_manager.cursor.get_position().toMutableList()
            val cursor = opus_manager.cursor
            if (position.isEmpty()) {
                when (cursor.ctl_level) {
                    CtlLineLevel.Global -> opus_manager.controller_global_split_tree(cursor.ctl_type!!, cursor.beat, position, 2)
                    CtlLineLevel.Channel -> opus_manager.controller_channel_split_tree(cursor.ctl_type!!, cursor.channel, cursor.beat, position, 2)
                    CtlLineLevel.Line -> opus_manager.controller_line_split_tree(cursor.ctl_type!!, cursor.get_beatkey(), position, 2)
                    null -> opus_manager.split_tree_at_cursor(it + 1)
                }
            } else {
                when (cursor.ctl_level) {
                    CtlLineLevel.Global -> opus_manager.controller_global_insert_after(cursor.ctl_type!!, cursor.beat, position)
                    CtlLineLevel.Channel -> opus_manager.controller_channel_insert_after(cursor.ctl_type!!, cursor.channel, cursor.beat, position)
                    CtlLineLevel.Line -> opus_manager.controller_line_insert_after(cursor.ctl_type!!, cursor.get_beatkey(), position)
                    null -> opus_manager.insert_after_cursor(it)
                }
            }
            return
        }

        this.dialog_number_input(R.string.dlg_insert, 1, 63) {
            this.insert_leaf(it)
        }
    }

    fun toggle_percussion() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = cursor.get_beatkey()
        val position = cursor.get_position()
        val current_tree_position = opus_manager.get_actual_position(beat_key, position)

        if (opus_manager.get_tree(current_tree_position.first, current_tree_position.second).has_event()) {
            opus_manager.unset()
        } else {
            opus_manager.set_percussion_event_at_cursor()

            val event_note = opus_manager.get_percussion_instrument(beat_key.channel, beat_key.line_offset)
            this.play_event(beat_key.channel, event_note)
        }
    }

    fun set_percussion_instrument(channel: Int, line_offset: Int, instrument: Int? = null) {
        val opus_manager = this.get_opus_manager()

        instrument?.let {
            opus_manager.percussion_set_instrument(channel, line_offset, it)
            this.play_event(channel, it)
            return
        }

        val options = mutableListOf<Pair<Int, @Composable RowScope.() -> Unit>>()

        if (this.vm_controller.active_midi_device == null && this.vm_controller.audio_interface.soundfont != null) {
            val preset = opus_manager.get_channel_instrument(channel)
            val instruments = opus_manager.vm_state.get_available_instruments(preset)
            for ((name, index) in instruments) {
                if (index < 0) continue
                options.add(Pair(index, { Text("$index: $name") }))
            }
        } else {
            for (i in 0 .. 60) {
                options.add(
                    Pair(i, { Text("$i: ${stringArrayResource(R.array.midi_drums)[i]}")})
                )
            }
        }


        val current_instrument = (opus_manager.get_channel(channel).lines[line_offset] as OpusLinePercussion).instrument

        if (options.isNotEmpty()) {
            this.vm_top.unsortable_list_dialog(R.string.dropdown_choose_instrument, options, current_instrument) {
                this.set_percussion_instrument(channel, line_offset, it)
            }
        }
    }

    private fun color_picker_dialog(color: Color, callback: (Color?) -> Unit) {
        val color_state: MutableState<Color> = mutableStateOf(color)
        this.vm_top.create_dialog { close ->
            @Composable {
                Row(horizontalArrangement = Arrangement.Center) {
                    ColorPicker(
                        modifier = Modifier.fillMaxWidth(),
                        color = color_state
                    )
                }
                DialogBar(
                    negative = {
                        close()
                        callback(null)
                    },
                    negative_label = R.string.use_default_color,
                    neutral = close,
                    positive = {
                        callback(color_state.value)
                        close()
                    }

                )
            }
        }
    }
    fun set_line_color(channel: Int, line_offset: Int, color: Color? = null) {
        val opus_manager = this.get_opus_manager()
        color?.let {
            opus_manager.set_line_event_color(channel, line_offset, color)
        }

        val default_color = opus_manager.get_channel(channel).lines[line_offset].palette.event
            ?: opus_manager.get_channel(channel).palette.event
            ?: Colors.LEAF_COLOR

        this.color_picker_dialog(default_color) { new_color: Color? ->
            if (new_color == null) {
                this.unset_line_color(channel, line_offset)
            } else {
                this.set_line_color(channel, line_offset, new_color)
            }
        }
    }
    fun unset_line_color(channel: Int, line_offset: Int) {
        val opus_manager = this.get_opus_manager()
        opus_manager.set_line_event_color(channel, line_offset, null)
    }

    fun set_channel_color(channel: Int, color: Color? = null) {
        val opus_manager = this.get_opus_manager()
        color?.let {
            opus_manager.set_channel_event_color(channel, color)
        }

        this.color_picker_dialog(opus_manager.get_channel(channel).palette.event ?: Colors.LEAF_COLOR) { new_color: Color? ->
            if (new_color == null) {
                this.unset_channel_color(channel)
            } else {
                this.set_channel_color(channel, new_color)
            }
        }
    }
    fun unset_channel_color(channel: Int) {
        val opus_manager = this.get_opus_manager()
        opus_manager.set_channel_event_color(channel, null)
    }

    fun set_channel_preset(channel: Int, instrument: Pair<Int, Int>? = null) {
        val opus_manager = this.get_opus_manager()

        instrument?.let {
            opus_manager.channel_set_preset(channel, instrument)

            val radix = opus_manager.get_radix()
            this.play_event(channel, (3 * radix))
            Thread.sleep(200)
            this.play_event(channel, (3 * radix) + (4 * radix / 12))
            Thread.sleep(200)
            this.play_event(channel, (3 * radix) + (7 * radix / 12))
            return
        }

        fun padded_hex(i: Int): String {
            var s = Integer.toHexString(i)
            while (s.length < 2) {
                s = "0$s"
            }
            return s.uppercase()
        }

        val default = opus_manager.get_channel_instrument(channel)
        val preset_names =  mutableListOf<Triple<Int, Int, String?>>()
        val options = mutableListOf<Pair<Pair<Int, Int>, @Composable RowScope.() -> Unit>>()
        val is_percussion = opus_manager.is_percussion(channel)

        val pre_option = if (this.vm_controller.audio_interface.soundfont == null || this.vm_controller.active_midi_device != null) {
            val std_hashmap = HashMap<Int, String?>()
            for (i in 0 until 128) std_hashmap[i] = null
            sortedMapOf<Int, HashMap<Int, String?>>(
                0 to std_hashmap,
                128 to hashMapOf(0 to null)
            )
        } else {
            opus_manager.vm_state.preset_names.toSortedMap()
        }

        for ((bank, bank_map) in pre_option) {
            if (is_percussion && bank != 128) continue
            if (!this.vm_top.configuration.allow_std_percussion.value && !is_percussion && bank == 128) continue

            for ((program, name) in bank_map.toSortedMap()) {
                preset_names.add(Triple(bank, program, name))
                options.add(
                    Pair(
                        Pair(bank, program),
                        {
                            Text("${padded_hex(bank)} | ${padded_hex(program)}")
                            Text(
                                name ?: if (bank == 128) {
                                    stringResource(R.string.gm_kit)
                                } else {
                                    stringArrayResource(R.array.general_midi_presets)[program]
                                },
                                modifier = Modifier.weight(1F),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    )
                )
            }
        }
        val default_presets = this.context.resources.getStringArray(R.array.general_midi_presets)
        val sort_options = listOf(
            Pair(R.string.sort_option_bank) { a: Int, b: Int -> preset_names[a].first.compareTo(preset_names[b].first) },
            Pair(R.string.sort_option_program) { a: Int, b: Int -> preset_names[a].second.compareTo(preset_names[b].second) },
            Pair(R.string.sort_option_abc) { a: Int, b: Int ->
                val a_name = preset_names[a].third ?: default_presets[preset_names[a].second]
                val b_name = preset_names[b].third ?: default_presets[preset_names[b].second]
                a_name.lowercase().compareTo(b_name.lowercase())
            }
        )

        this.vm_top.sortable_list_dialog(
            R.string.dropdown_choose_instrument,
            default_menu = options,
            sort_options = sort_options,
            selected_sort = mutableIntStateOf(0),
            default_value = default,
            onClick = { this.set_channel_preset(channel, it) }
        )
    }

    fun insert_percussion_channel(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (index != null) {
            val adj_index = if (index == -1) {
                opus_manager.channels.size
            } else {
                index
            }
            opus_manager.new_channel(adj_index, is_percussion = true)
        } else {
            val channel = opus_manager.cursor.channel
            opus_manager.new_channel(channel + 1, is_percussion = true)
        }
    }

    fun insert_channel(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (index != null) {
            if (index == -1) {
                opus_manager.new_channel()
            } else {
                opus_manager.new_channel(index)
            }
        } else {
            val channel = opus_manager.cursor.channel
            opus_manager.new_channel(channel + 1)
        }
    }

    fun remove_channel(index: Int? = null) {
        val opus_manager = this.get_opus_manager()
        if (opus_manager.channels.size > 1) {
            val use_index = index ?: opus_manager.cursor.channel
            opus_manager.remove_channel(use_index)
        }
    }

    fun move_channel(index_from: Int, index_to: Int, before: Boolean = true) {
        val opus_manager = this.get_opus_manager()
        val adj_to_index = index_to + if (before) 0 else 1
        if (adj_to_index == index_from) return

        opus_manager.move_channel(index_from, min(opus_manager.channels.size, adj_to_index))
    }


    fun insert_line(count: Int? = null) {
        val opus_manager = this.get_opus_manager()
        count?.let {
            opus_manager.insert_line_at_cursor(it)
            return
        }
        this.dialog_number_input(R.string.dlg_insert_lines, 1, 9) {
            this.insert_line(it)
        }
    }

    fun remove_line(count: Int? = null) {
        val opus_manager = this.get_opus_manager()
        count?.let {
            opus_manager.remove_line_at_cursor(it)
            return
        }

        val lines = opus_manager.get_all_channels()[opus_manager.cursor.channel].size
        val max_lines = Integer.min(lines - 1, lines - opus_manager.cursor.line_offset)
        this.dialog_number_input(R.string.dlg_remove_lines, 1, max_lines) {
            this.remove_line(it)
        }
    }

    fun set_percussion_instrument(value: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        value?.let {
            opus_manager.set_percussion_instrument(it)
            if (value >= 0) {
                this.play_event(cursor.channel, value)
            }
            return
        }

        val default_instrument = opus_manager.get_percussion_instrument(cursor.channel, cursor.line_offset)
        val options = mutableListOf<Pair<Int, @Composable RowScope.() -> Unit>>()
        this.dialog_popup_menu(R.string.dropdown_choose_percussion, options, default_instrument) {
            this.set_percussion_instrument(it)
        }
    }

    fun insert_beat(beat: Int, repeat: Int? = null) {
        repeat?.let {
            val opus_manager = this.get_opus_manager()
            opus_manager.insert_beats(beat, it)
            return
        }

        this.dialog_number_input(R.string.dlg_insert_beats, 1, 4096) { count: Int ->
            this.insert_beat(beat, count)
        }
    }

    fun append_beats(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        this.insert_beat(opus_manager.length, repeat)
    }

    fun remove_beat(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()

        repeat?.let {
            opus_manager.remove_beat_at_cursor(it)
            return
        }

        this.dialog_number_input(R.string.dlg_remove_beats, 1, opus_manager.length - 1) { count: Int ->
            this.remove_beat(count)
        }
    }

    fun insert_beat_after_cursor(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()

        repeat?.let {
            opus_manager.insert_beat_after_cursor(it)
            return
        }

        this.dialog_number_input(R.string.dlg_insert_beats, 1, 4096) {
            this.insert_beat_after_cursor(it)
        }
    }

    fun remove_beat_at_cursor(repeat: Int? = null) {
        val opus_manager = this.get_opus_manager()

        repeat?.let {
            opus_manager.remove_beat_at_cursor(it)
            return
        }

        this.dialog_number_input(R.string.dlg_remove_beats, 1, opus_manager.length - 1) {
            this.remove_beat_at_cursor(it)
        }
    }

    fun set_project_name_and_notes(project_name_and_notes: Pair<String, String>? = null) {
        val opus_manager = this.get_opus_manager()
        project_name_and_notes?.let { (name, notes) ->
            opus_manager.set_name_and_notes(
                if (name == "") null else name,
                if (notes == "") null else notes
            )
            return
        }

        this.vm_top.create_dialog { close ->
            @Composable {
                val project_name = remember { mutableStateOf(opus_manager.project_name ?: "") }
                val project_notes = remember { mutableStateOf(opus_manager.project_notes ?: "") }
                TextInput(
                    label = { Text(R.string.dlg_project_name) },
                    input = project_name,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.Default
                ) {}
                Spacer(modifier = Modifier.height(Dimensions.Space.Medium))
                TextInput(
                    label = { Text(R.string.dlg_project_notes) },
                    input = project_notes,
                    textAlign = TextAlign.Start,
                    lineLimits = TextFieldLineLimits.MultiLine(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F, fill = false)
                ) {}
                DialogBar(
                    neutral = close,
                    positive = {
                        close()
                        this@ActionDispatcher.set_project_name_and_notes(Pair(project_name.value, project_notes.value))
                    }
                )
            }
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

     fun dialog_number_input(title_string_id: Int, min_value: Int, max_value: Int? = null, default: Int? = null, callback: (value: Int) -> Unit) {
        this.vm_top.create_dialog { close ->
            @Composable {
                val focus_requester = remember { FocusRequester() }
                val value = remember {
                    mutableStateOf(
                        default ?: this@ActionDispatcher.persistent_number_input_values[title_string_id] ?: min_value
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IntegerInput(
                        value = value,
                        label = { Text(title_string_id) },
                        modifier = Modifier
                            .testTag(TestTag.DialogNumberInput)
                            .focusRequester(focus_requester),
                        contentPadding = PaddingValues(Dimensions.NumberInputDialogPadding),
                        text_align = TextAlign.Center,
                        minimum = min_value,
                        maximum = max_value
                    ) { new_value ->
                        close()
                        this@ActionDispatcher.persistent_number_input_values[title_string_id] = new_value
                        callback(new_value)
                    }
                }

                DialogBar(
                    neutral = close,
                    positive = {
                        close()
                        this@ActionDispatcher.persistent_number_input_values[title_string_id] = value.value
                        callback(value.value)
                    }
                )

               LaunchedEffect(Unit) {
                   focus_requester.requestFocus()
               }
            }
        }
    }

    private fun needs_save(): Boolean {
        val opus_manager = this.get_opus_manager()

        val active_project = this.vm_controller.active_project ?: return opus_manager.history_cache.has_undoable_actions()
        val other = this.vm_top.project_manager?.open_project(active_project)
        return (opus_manager as OpusLayerBase) != other
    }

    fun save_before(callback: (Boolean) -> Unit) {
        if (!this.needs_save()) {
            callback(false)
            return
        }

        this.vm_top.create_dialog { close ->
            @Composable {
                Row { DialogSTitle(R.string.dialog_save_warning_title, modifier = Modifier.weight(1F)) }
                DialogBar(
                    negative = {
                        close()
                        callback(false)
                    },
                    neutral = close,
                    positive = {
                        close()
                        this@ActionDispatcher.save()

                        callback(true)
                    }
                )
            }
        }
    }


    /**
     * wrapper around MainActivity::dialog_popup_menu
     * will subvert popup on replay
     */
    private fun <T> dialog_popup_menu(title: Int, options: List<Pair<T, @Composable RowScope.() -> Unit>>, default: T? = null, callback: (value: T) -> Unit) {
        this.vm_top.create_dialog(0) { close ->
            @Composable {
                DialogSTitle(title)
                UnSortableMenu(Modifier, options, default) { value ->
                    close()
                    callback(value)
                }
                DialogBar(neutral = close)
            }
        }
    }

    private fun dialog_text_popup(title: Int, default: String? = null, callback: (String) -> Unit) {
        this.vm_top.create_dialog { close ->
            @Composable {
                val value = remember { mutableStateOf(default ?: "") }
                DialogSTitle(title)
                Row {
                    TextInput(
                        input = value,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(),
                        lineLimits = TextFieldLineLimits.Default,
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

    fun get_opus_manager(): OpusManager {
        return this.vm_controller.opus_manager
    }

    fun play_opus_midi(loop_playback: Boolean = false) {
        if (this.vm_controller.active_midi_device == null) return
        val opus_manager = this.get_opus_manager()
        this.vm_controller.update_playback_state_midi(PlaybackState.Queued)
        opus_manager.vm_state.playback_state_midi.value = this.vm_controller.playback_state_midi

        val cursor = opus_manager.cursor
        val start_beat = when (cursor.mode) {
            CursorMode.Single,
            CursorMode.Column -> if (cursor.beat == opus_manager.length - 1) {
                0
            } else {
                cursor.beat
            }

            CursorMode.Range -> cursor.get_ordered_range()!!.first.beat

            CursorMode.Line,
            CursorMode.Channel,
            CursorMode.Unset -> 0
        }

        val midi = opus_manager.get_midi(start_beat, include_pointers = true)

        if (!this.vm_controller.update_playback_state_midi(PlaybackState.Playing)) return
        opus_manager.vm_state.playback_state_midi.value = this.vm_controller.playback_state_midi

        thread {
            try {
                //this._midi_interface.open_output_device(this.editor_view_model.active_midi_device!!)
                this.vm_controller.virtual_midi_device.play_midi(midi, loop_playback) {
                    this.stop_opus_midi()
                }
                opus_manager.vm_state.looping_playback.value = loop_playback
            } catch (_: IOException) {
                this.stop_opus_midi()
            }
        }
    }

    fun stop_opus_midi() {
        this.vm_controller.update_playback_state_midi(PlaybackState.Stopping)
        this.vm_controller.virtual_midi_device.stop()
        this.vm_controller.update_playback_state_midi(PlaybackState.Ready)
        this.get_opus_manager().vm_state.playback_state_midi.value = this.vm_controller.playback_state_midi
    }

    fun play_opus(scope: CoroutineScope, loop_playback: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        this.vm_controller.playback_device?.let {
            it.play_opus(
                when (opus_manager.cursor.mode) {
                    CursorMode.Single,
                    CursorMode.Column -> if (opus_manager.cursor.beat == opus_manager.length - 1) {
                        0
                    } else {
                        opus_manager.cursor.beat
                    }


                    CursorMode.Range -> opus_manager.cursor.get_ordered_range()!!.first.beat

                    CursorMode.Line,
                    CursorMode.Channel,
                    CursorMode.Unset -> {
                        opus_manager.vm_state.scroll_state_x.value.firstVisibleItemIndex
                    }
                },
                loop_playback
            )
            opus_manager.vm_state.looping_playback.value = loop_playback
        }
    }

    fun stop_opus() {
        this.vm_controller.playback_device?.kill()
    }

    fun set_copy_mode(mode: PaganConfiguration.MoveMode) {
        this.vm_top.configuration.move_mode.value = mode
        this.vm_top.save_configuration()
    }

    fun remove_controller() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        opus_manager.lock_cursor {
            when (cursor.ctl_level!!) {
                CtlLineLevel.Line -> opus_manager.remove_line_controller(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
                CtlLineLevel.Channel -> opus_manager.remove_channel_controller(cursor.ctl_type!!, cursor.channel)
                CtlLineLevel.Global -> opus_manager.remove_global_controller(cursor.ctl_type!!)
            }
        }
    }

    fun toggle_controller_visibility() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        opus_manager.lock_cursor {
            when (cursor.ctl_level!!) {
                CtlLineLevel.Line -> opus_manager.toggle_line_controller_visibility(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
                CtlLineLevel.Channel -> opus_manager.toggle_channel_controller_visibility(cursor.ctl_type!!, cursor.channel)
                CtlLineLevel.Global -> opus_manager.toggle_global_controller_visibility(cursor.ctl_type!!)
            }
        }
    }

    fun channel_mute(channel: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel

        opus_manager.lock_cursor {
            opus_manager.mute_channel(w_channel)
        }
    }

    fun channel_unmute(channel: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel

        opus_manager.lock_cursor {
            opus_manager.unmute_channel(w_channel)
        }
    }

    fun line_mute(channel: Int? = null, line_offset: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel
        val w_line_offset = line_offset ?: cursor.line_offset

        opus_manager.mute_line(w_channel, w_line_offset)
    }

    fun line_unmute(channel: Int? = null, line_offset: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val w_channel = channel ?: cursor.channel
        val w_line_offset = line_offset ?: cursor.line_offset

        opus_manager.lock_cursor {
            opus_manager.unmute_line(w_channel, w_line_offset)
        }
    }

    fun tag_column(beat: Int? = null, description: String? = null, force_null_description: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        val use_beat = beat ?: opus_manager.cursor.beat

        if (force_null_description && description == null) {
            opus_manager.lock_cursor {
                opus_manager.tag_section(use_beat, null)
            }
            return
        } else if (description != null) {
            opus_manager.lock_cursor {
                opus_manager.tag_section(use_beat, description)
            }
            return
        }

        this.dialog_text_popup(R.string.dialog_mark_section, opus_manager.marked_sections[use_beat]) { result: String ->
            opus_manager.lock_cursor {
                opus_manager.tag_section(use_beat, if (result == "") null else result)
            }
        }
    }

    fun untag_column(beat: Int? = null) {
        val opus_manager = this.get_opus_manager()
        val use_beat = beat ?: opus_manager.cursor.beat
        opus_manager.lock_cursor {
            opus_manager.remove_tagged_section(use_beat)
        }
    }

    @Composable
    fun TuningDialogTopLine(
        transpose_numerator: MutableState<Int>,
        transpose_denominator: MutableState<Int>,
        radix: MutableState<Int>,
        mutable_map: MutableList<Pair<MutableState<Int>, MutableState<Int>>>
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(R.string.dlg_transpose, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IntegerInput(
                        value = transpose_numerator,
                        minimum = 0,
                        contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                        modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                        callback = { }
                    )
                    DivisorSeparator()
                    IntegerInput(
                        value = transpose_denominator,
                        minimum = 1,
                        contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                        modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                        callback = { }
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    R.string.dlg_set_radix,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
                IntegerInput(
                    value = radix,
                    minimum = 1,
                    maximum = 36,
                    contentPadding = PaddingValues(Dimensions.TransposeDialogInnerPadding),
                    modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                    callback = {
                        mutable_map.clear()
                        for (i in 0 until it) {
                            mutable_map.add(
                                Pair(
                                    mutableStateOf(i),
                                    mutableStateOf(it)
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun ColumnScope.TuningDialogNormal(
        close_callback: () -> Unit,
        transpose_numerator: MutableState<Int>,
        transpose_denominator: MutableState<Int>,
        radix: MutableState<Int>,
        mutable_map: MutableList<Pair<MutableState<Int>, MutableState<Int>>>
    ) {

        TuningDialogTopLine(transpose_numerator, transpose_denominator, radix, mutable_map)

        Spacer(Modifier.height(Dimensions.TransposeDialogInnerPadding))

        Surface(
            modifier = Modifier.weight(1F, fill = false),
            border = BorderStroke(Dimensions.TuningDialogStrokeWidth, MaterialTheme.colorScheme.onSurface),
            shape = Shapes.TuningDialogBox,
            tonalElevation = 1.dp
        ) {
            key(radix.value) {
                FlowRow(
                    modifier = Modifier
                        .padding(Dimensions.TuningDialogBoxPadding)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    for ((i, state) in mutable_map.enumerate()) {
                        val (numer, denom) = state
                        Surface(
                            modifier = Modifier.padding(Dimensions.TuningDialogLineSpacing),
                            shape = Shapes.TuningDialogBox,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(Dimensions.TuningDialogLinePadding),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "%02d".format(i),
                                    modifier = Modifier.padding(horizontal = Dimensions.TuningDialogLinePadding)
                                )
                                Spacer(Modifier.weight(1F))
                                IntegerInput(
                                    value = numer,
                                    minimum = 0,
                                    modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                                    contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                    callback = {}
                                )
                                DivisorSeparator()
                                IntegerInput(
                                    value = denom,
                                    minimum = 1,
                                    modifier = Modifier.width(Dimensions.TransposeDialogInputWidth),
                                    contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                    callback = { }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(
            Modifier
                .height(Dimensions.Space.Medium)
                .fillMaxWidth()
        )

        DialogBar(
            neutral = close_callback,
            positive = {
                close_callback()
                this@ActionDispatcher.set_tuning_table_and_transpose(
                    Array(mutable_map.size) { i ->
                        Pair(
                            mutable_map[i].first.value,
                            mutable_map[i].second.value
                        )
                    },
                    Pair(
                        transpose_numerator.value,
                        transpose_denominator.value
                    )
                )
            }
        )
    }

    @Composable
    fun ColumnScope.TuningDialogTiny(
        close_callback: () -> Unit,
        transpose_numerator: MutableState<Int>,
        transpose_denominator: MutableState<Int>,
        radix: MutableState<Int>,
        mutable_map: MutableList<Pair<MutableState<Int>, MutableState<Int>>>
    ) {
        val actively_editting_index = remember { mutableStateOf(0) }
        val expanded = remember { mutableStateOf(false) }
        Row(Modifier.height(IntrinsicSize.Min)) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(1F, fill = false),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                ProvideTextStyle(Typography.TinyTuningDialogLabel) {
                    TuningDialogTopLine(transpose_numerator, transpose_denominator, radix, mutable_map)
                }
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        R.string.label_tuning,
                        style = Typography.TinyTuningDialogLabel
                    )
                    Surface(
                        shape = Shapes.Container,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(Dimensions.TinyTuningDialogInnerPadding)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DropdownMenu(
                                expanded = expanded.value,
                                onDismissRequest = { expanded.value = false }
                            ) {
                                for (i in 0 until radix.value) {
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .then(
                                                if (i == actively_editting_index.value) {
                                                    Modifier.background(MaterialTheme.colorScheme.tertiary)
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        text = {
                                            Text(
                                                "${"%02d".format(i)}: ${mutable_map[i].first.value} / ${mutable_map[i].second.value}",
                                                style = LocalTextStyle.current.copy(
                                                    color = if (i == actively_editting_index.value) {
                                                        MaterialTheme.colorScheme.onTertiary
                                                    } else {
                                                        LocalTextStyle.current.color
                                                    }
                                                )
                                            )
                                        },
                                        onClick = {
                                            actively_editting_index.value = i
                                            expanded.value = false
                                        }
                                    )
                                }
                            }
                            key(radix.value) {
                                Button(
                                    content = { Text("%02d".format(actively_editting_index.value)) },
                                    onClick = { expanded.value = !expanded.value }
                                )
                                Spacer(Modifier.weight(1F))
                                key(actively_editting_index.value) {
                                    IntegerInput(
                                        value = mutable_map[actively_editting_index.value].first,
                                        minimum = 0,
                                        modifier = Modifier.width(Dimensions.TinyTuningDialogInputWidth),
                                        contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                        callback = {}
                                    )
                                    DivisorSeparator()
                                    IntegerInput(
                                        value = mutable_map[actively_editting_index.value].second,
                                        minimum = 1,
                                        modifier = Modifier.width(Dimensions.TinyTuningDialogInputWidth),
                                        contentPadding = PaddingValues(Dimensions.TransposeDialogInputPadding),
                                        callback = { }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(Dimensions.TinyTuningDialogInnerPadding))

            Column(
                Modifier
                    .fillMaxHeight()
                    .background(color = MaterialTheme.colorScheme.surface, shape = Shapes.Container)
                    .border(width = Dimensions.TuningDialogStrokeWidth, color = MaterialTheme.colorScheme.onSurface, shape = Shapes.Container)
                    .padding(horizontal = Dimensions.TuningDialogBoxPadding),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .width(Dimensions.TinyTuningDialogButtonSize)
                        .height(Dimensions.TinyTuningDialogButtonSize),
                    contentPadding = PaddingValues(Dimensions.TinyTuningDialogButtonPadding),
                    shape = CircleShape,
                    onClick = close_callback,
                    content = {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.icon_x),
                            contentDescription = stringResource(android.R.string.cancel)
                        )
                    }
                )
                Button(
                    modifier = Modifier
                        .width(Dimensions.TinyTuningDialogButtonSize)
                        .height(Dimensions.TinyTuningDialogButtonSize),
                    contentPadding = PaddingValues(Dimensions.TinyTuningDialogButtonPadding),
                    shape = CircleShape,
                    onClick = {
                        close_callback()
                        this@ActionDispatcher.set_tuning_table_and_transpose(
                            Array(mutable_map.size) { i ->
                                Pair(
                                    mutable_map[i].first.value,
                                    mutable_map[i].second.value
                                )
                            },
                            Pair(
                                transpose_numerator.value,
                                transpose_denominator.value
                            )
                        )
                    },
                    content = {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            painter = painterResource(R.drawable.icon_check),
                            contentDescription = stringResource(android.R.string.ok)
                        )
                    }
                )
            }
        }
    }

    fun set_tuning_table_and_transpose(tuning_map: Array<Pair<Int, Int>>? = null, transpose: Pair<Int, Int>? = null) {
        val opus_manager = this.get_opus_manager()
        if (tuning_map != null && transpose != null) {
            opus_manager.set_tuning_map_and_transpose(tuning_map, transpose)
            return
        }

        this.vm_top.create_dialog { close ->
            @Composable {
                val original_radix = opus_manager.get_radix()
                val transpose_numerator = remember { mutableIntStateOf(opus_manager.transpose.first) }
                val transpose_denominator = remember { mutableIntStateOf(opus_manager.transpose.second) }
                val radix = remember { mutableIntStateOf(original_radix) }
                val mutable_map = MutableList(radix.intValue) { i ->
                    if (radix.intValue == original_radix) {
                        Pair(
                            mutableStateOf(opus_manager.tuning_map[i].first),
                            mutableStateOf(opus_manager.tuning_map[i].second)
                        )
                    } else {
                        Pair(mutableStateOf(i), mutableStateOf(radix.intValue))
                    }
                }

                if(this@ActionDispatcher.vm_top.active_layout_size.value == LayoutSize.SmallLandscape) {
                    TuningDialogTiny(close, transpose_numerator, transpose_denominator, radix, mutable_map)
                } else {
                    TuningDialogNormal(close, transpose_numerator, transpose_denominator, radix, mutable_map)
                }
            }
        }
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
       } catch (e: IncompatibleChannelException) {
          Toast.makeText(this.context, R.string.std_percussion_swap, Toast.LENGTH_SHORT).show()
       }
    }

    fun load_from_bkp() {
        val (backup_uri, bytes) = this.vm_top.project_manager?.read_backup() ?: throw MissingProjectManager()
        this.get_opus_manager().load(bytes) {
            this.vm_controller.active_project = backup_uri
            backup_uri?.let {
                this.vm_controller.project_exists.value = DocumentFile.fromTreeUri(this.context, it)?.exists() == true
            }
        }
    }

}
