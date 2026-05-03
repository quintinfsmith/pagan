/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2026  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.ColorPickerDialog
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.IntegerInputDialog
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.NumberInput
import com.qfs.pagan.composable.NumberPicker
import com.qfs.pagan.composable.PaganDialog
import com.qfs.pagan.composable.SoundfontLoadingIndicator
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.composable.effectwidget.DelayEventMenu
import com.qfs.pagan.composable.effectwidget.PanEventMenu
import com.qfs.pagan.composable.effectwidget.ReverbEventMenu
import com.qfs.pagan.composable.effectwidget.TempoEventMenu
import com.qfs.pagan.composable.effectwidget.VelocityEventMenu
import com.qfs.pagan.composable.effectwidget.VolumeEventMenu
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun AdjustSelectionDialog(visibility: MutableState<Boolean>, radix: Int, callback: (Int) -> Unit) {
    PaganDialog(visibility) {
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
                visibility.value = false
                callback((octave.intValue * radix) + offset.intValue)
            },
            neutral = {
                visibility.value = false
            }
        )
    }
}
@Composable
fun AdjustLineButton(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface) {
    val visibility = remember { mutableStateOf(false) }
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.AdjustSelection),
        onClick = {
            visibility.value = !visibility.value
        },
        shape = Shapes.ContextMenuButtonPrimary,
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )

    if (visibility.value) {
        AdjustSelectionDialog(visibility, vm_state.radix.value) { offset ->
            opus_manager.offset_selection(offset)
        }
    }
}
@Composable
fun DuplicateLineButton(active_line: ViewModelEditorState.LineData, opus_manager: OpusLayerInterface) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineDuplicate),
        onClick = {
            val channel = active_line.channel.value ?: return@IconCMenuButton
            val line_offset = active_line.line_offset.value ?: return@IconCMenuButton
            opus_manager.duplicate_line(channel, line_offset)
        },
        shape = Shapes.ContextMenuButtonPrimary,
        icon = R.drawable.icon_ic_baseline_content_copy_24,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun ToggleLineControllerButton(
    opus_manager: OpusLayerInterface,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineEffectsShow),
        onClick = { opus_manager.toggle_controller_visibility_at_cursor() },
        shape = shape,
        icon = R.drawable.icon_ctl,
        description = R.string.cd_show_effect_controls
    )
}

@Composable
fun InsertLineButton(active_line: ViewModelEditorState.LineData, opus_manager: OpusLayerInterface, shape: Shape = Shapes.ContextMenuButtonPrimaryStart) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineNew),
        onClick = {
            val channel = active_line.channel.value ?: return@IconCMenuButton
            val line_offset = active_line.line_offset.value ?: return@IconCMenuButton
            opus_manager.new_line(channel, line_offset + 1)
        },
        onLongClick = {
            TODO("NUMBER DIALOG")
        },
        icon = R.drawable.icon_add,
        shape = shape,
        description = R.string.cd_insert_line
    )
}

@Composable
fun RemoveLineButton(active_line: ViewModelEditorState.LineData, opus_manager: OpusLayerInterface, size: Int) {
    val visibility = remember { mutableStateOf(false) }
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineRemove),
        enabled = size > 1,
        onClick = {
            val channel = active_line.channel.value ?: return@IconCMenuButton
            val line_offset = active_line.line_offset.value ?: return@IconCMenuButton
            opus_manager.remove_line_repeat(channel, line_offset, 1)
        },
        onLongClick = {
            visibility.value = true
        },
        icon = R.drawable.icon_subtract,
        description = R.string.cd_remove_line
    )

    IntegerInputDialog(visibility, R.string.dlg_remove_lines, 0) { i ->
        val channel = active_line.channel.value ?: return@IntegerInputDialog
        val line_offset = active_line.line_offset.value ?: return@IntegerInputDialog
        opus_manager.remove_line_repeat(channel, line_offset, i)
    }
}

@Composable
fun RemoveEffectButton(active_line: ViewModelEditorState.LineData, opus_manager: OpusLayerInterface, shape: Shape) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineEffectRemove),
        onClick = {
            active_line.ctl_type.value?.let { type ->
                val channel = active_line.channel.value ?: return@let
                val line_offset = active_line.line_offset.value ?: return@let
                opus_manager.remove_line_controller(type, channel, line_offset)
            }
        },
        icon = R.drawable.icon_subtract,
        shape = shape,
        description = R.string.cd_remove_line
    )
}
@Composable
fun PercussionSetInstrumentButton(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, y: Int, use_name: Boolean) {
    if (vm_state.soundfont_ready.value) {
        val active_line = vm_state.line_data[y]
        val assigned_offset = active_line.assigned_offset.value ?: return
        val active_channel = vm_state.channel_data[active_line.channel.value!!]
        val midi_instruments = stringArrayResource(R.array.midi_drums)
        val label = if (use_name) {
            if (vm_state.soundfont_active.value != null && !vm_state.use_midi_playback.value) {
                vm_state.get_instrument_name(active_channel.instrument.value, assigned_offset)
            } else {
                midi_instruments[assigned_offset]
            }
        } else {
            "!${"%02d".format(assigned_offset)}"
        }

        key(vm_state.soundfont_active.value) {
            TextCMenuButton(
                modifier = modifier.testTag(TestTag.InstrumentSet),
                onClick = {
                    opus_manager.set_percussion_instrument(
                        active_line.channel.value!!,
                        active_line.line_offset.value!!
                    )
                },
                text = label ?: if (active_channel.instrument.value.bank == 128) {
                    if (assigned_offset < midi_instruments.size) {
                        if (vm_state.soundfont_active.value != null && !vm_state.use_midi_playback.value) {
                            stringResource(R.string.unavailable_preset, midi_instruments[assigned_offset])
                        } else {
                            midi_instruments[assigned_offset]
                        }
                    } else {
                        stringResource(R.string.unknown_instrument, assigned_offset)
                    }
                } else {
                    "${stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.bank]} @${assigned_offset}"
                }
            )
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            SoundfontLoadingIndicator()
        }
    }
}

@Composable
fun SetLineColorButton(
    modifier: Modifier = Modifier,
    ui_facade: ViewModelEditorState,
    opus_manager: OpusLayerInterface,
    channel: Int,
    line_offset: Int,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    val visibility = remember { mutableStateOf(false) }

    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineColor),
        onClick = { visibility.value = true },
        shape = shape,
        icon = R.drawable.icon_palette,
        description = R.string.cd_line_mute
    )

    if (visibility.value) {
        val default_color = opus_manager.get_channel(channel).lines[line_offset].palette.event
            ?: opus_manager.get_channel(channel).palette.event
            ?: Colors.LEAF_COLOR
        ColorPickerDialog(default_color, visibility) { new_color ->
            opus_manager.set_line_event_color(channel, line_offset, new_color)
        }
    }
}

@Composable
fun MuteButton(
    opus_manager: OpusLayerInterface,
    line: ViewModelEditorState.LineData,
) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineMute),
        onClick = {
            if (line.is_mute.value) {
                opus_manager.unmute_line_at_cursor()
            } else {
                opus_manager.mute_line_at_cursor()
            }
        },
        icon = if (!line.is_mute.value) R.drawable.icon_unmute
        else R.drawable.icon_mute,
        shape = Shapes.ContextMenuSecondaryButtonStart,
        description = R.string.cd_line_mute
    )
}

@Composable
fun HideEffectButton(active_line: ViewModelEditorState.LineData, opus_manager: OpusLayerInterface, shape: Shape) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.EffectHide),
        onClick = {
            opus_manager.toggle_controller_visibility_at_cursor()
            active_line.ctl_type.value?.let { type ->
                val channel = active_line.channel.value ?: return@let
                val line_offset = active_line.line_offset.value ?: return@let
                opus_manager.set_line_controller_visibility(type, channel, line_offset, false)
            }
        },
        icon = R.drawable.icon_hide,
        shape = shape,
        description = R.string.cd_hide_control_line
    )
}

@Composable
fun ContextMenuLinePrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]
    if (active_line.ctl_type.value == null) {
        ContextMenuLineStdPrimary(modifier, vm_state, opus_manager, layout)
    } else {
        ContextMenuLineCtlPrimary(modifier, vm_state, opus_manager, layout)
    }
}

@Composable
fun ContextMenuLineStdPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]
    val active_channel = vm_state.channel_data[active_line.channel.value!!]
    when (layout) {
        LayoutSize.SmallPortrait,
        LayoutSize.MediumPortrait,
        LayoutSize.LargePortrait,
        LayoutSize.XLargePortrait,
        LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                ToggleLineControllerButton(
                    opus_manager,
                    shape = Shapes.ContextMenuButtonPrimaryStart
                )

                if (active_line.assigned_offset.value != null) {
                    MediumSpacer()
                    PercussionSetInstrumentButton(
                        Modifier.weight(1F),
                        vm_state,
                        opus_manager,
                        cursor.ints[0],
                        true
                    )
                } else {
                    Spacer(Modifier.weight(1F))
                    AdjustLineButton(vm_state, opus_manager)
                }

                MediumSpacer()
                DuplicateLineButton(active_line, opus_manager)
                MediumSpacer()
                RemoveLineButton(active_line, opus_manager, active_channel.size.intValue)
                MediumSpacer()
                InsertLineButton(active_line, opus_manager, Shapes.ContextMenuButtonPrimaryEnd)
            }
        }
        LayoutSize.SmallLandscape,
        LayoutSize.LargeLandscape,
        LayoutSize.MediumLandscape -> {
            Column {
                InsertLineButton(active_line, opus_manager)

                MediumSpacer()
                RemoveLineButton(active_line, opus_manager, active_channel.size.intValue)

                MediumSpacer()
                DuplicateLineButton(active_line, opus_manager)

                if (active_line.assigned_offset.value != null) {
                    MediumSpacer()
                    PercussionSetInstrumentButton(
                        Modifier
                            .width(Dimensions.ContextMenuButtonWidth)
                            .height(Dimensions.ContextMenuButtonHeight),
                        vm_state,
                        opus_manager,
                        cursor.ints[0],
                        false
                    )
                } else {
                    MediumSpacer()
                    AdjustLineButton(vm_state, opus_manager)
                }

                Spacer(Modifier.weight(1F))

                ToggleLineControllerButton(
                    opus_manager,
                    shape = Shapes.ContextMenuButtonPrimaryBottom
                )
            }
        }
    }
}
@Composable
fun ContextMenuLineCtlPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]

    when (layout) {
        LayoutSize.SmallPortrait,
        LayoutSize.MediumPortrait,
        LayoutSize.LargePortrait,
        LayoutSize.XLargePortrait,
        LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                HideEffectButton(active_line, opus_manager, Shapes.ContextMenuButtonPrimaryStart)
                Spacer(Modifier.weight(1F))
                RemoveEffectButton(active_line, opus_manager, Shapes.ContextMenuButtonPrimaryEnd)
            }
        }
        LayoutSize.SmallLandscape,
        LayoutSize.LargeLandscape,
        LayoutSize.MediumLandscape -> {
            Column {
                RemoveEffectButton(active_line, opus_manager, Shapes.ContextMenuButtonPrimaryStart)
                Spacer(Modifier.weight(1F))
                HideEffectButton(active_line, opus_manager, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
    }
}

@Composable
fun ContextMenuLineSecondary(ui_facade: ViewModelEditorState, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, layout: LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    key(ui_facade.event_change_key.value, ui_facade.active_event.value) {
        if (line.ctl_type.value == null) {
            ContextMenuLineStdSecondary(ui_facade, opus_manager, modifier = modifier, layout = layout)
        } else {
            ContextMenuLineCtlSecondary(ui_facade, opus_manager, modifier = modifier, layout = layout)
        }
    }
}

@Composable
fun ContextMenuLineCtlSecondary(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, layout: LayoutSize) {
    ContextMenuSecondaryRow {
        when (val initial_event = vm_state.active_event.value) {
            is OpusVolumeEvent -> VolumeEventMenu(vm_state, opus_manager, initial_event)
            is OpusTempoEvent -> TempoEventMenu(vm_state, opus_manager, initial_event)
            is OpusPanEvent -> PanEventMenu(vm_state, opus_manager, initial_event)
            is OpusReverbEvent -> ReverbEventMenu(vm_state, opus_manager, initial_event)
            is DelayEvent -> DelayEventMenu(vm_state, opus_manager, initial_event)
            is OpusVelocityEvent -> VelocityEventMenu(vm_state, opus_manager, initial_event)
            else -> {}
        }
    }
}

@Composable
fun ContextMenuLineStdSecondary(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = vm_state.line_data[y]

    ContextMenuSecondaryRow {
        MuteButton(opus_manager, line)
        MediumSpacer()
        VolumeEventMenu(vm_state, opus_manager, vm_state.active_event.value!! as OpusVolumeEvent)
        MediumSpacer()
        SetLineColorButton(
            Modifier,
            vm_state,
            opus_manager,
            line.channel.value!!,
            line.line_offset.value!!,
            shape = Shapes.ContextMenuSecondaryButtonEnd
        )
    }
}


