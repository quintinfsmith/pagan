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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.documentfile.provider.DocumentFile
import com.qfs.apres.VirtualMidiInputDevice
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogMenu
import com.qfs.pagan.composable.DialogSTitle
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.composable.LargeSpacer
import com.qfs.pagan.composable.PaganDialog
import com.qfs.pagan.composable.SortableMenu
import com.qfs.pagan.composable.TextInput
import com.qfs.pagan.composable.UnSortableMenu
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.OutlinedButton
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.RangeOverflow
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.viewmodel.ViewModelEditorController
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
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

    fun cursor_clear() {
        this.vm_controller.opus_manager.cursor_clear()
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
                    value = slider_position.floatValue,
                    steps = opus_manager.length,
                    colors = colors,
                    valueRange = 0F .. (opus_manager.length - 1).toFloat(),
                    onValueChange = { value ->
                        slider_position.floatValue = value
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

    fun hide_all_hidden_line_controller(effect_type: EffectType, all_channels: Boolean = false) {
        val opus_manager = this.get_opus_manager()
        if (all_channels) {
            opus_manager.unset_all_line_controller_visibility(effect_type)
        } else {
            val cursor = opus_manager.cursor
            opus_manager.unset_all_line_controller_visibility(effect_type, cursor.channel)
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
}
