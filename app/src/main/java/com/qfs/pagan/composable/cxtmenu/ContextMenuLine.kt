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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor
import com.qfs.pagan.EffectResourceMap
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.Values
import com.qfs.pagan.composable.ColorPickerDialog
import com.qfs.pagan.composable.DialogBar
import com.qfs.pagan.composable.DialogMenu
import com.qfs.pagan.composable.IntegerInputDialog
import com.qfs.pagan.composable.LargeSpacer
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.NumberPicker
import com.qfs.pagan.composable.PaganDialog
import com.qfs.pagan.composable.SoundfontLoadingIndicator
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.OutlinedButton
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
    vm_state: ViewModelEditorState,
    active_line: ViewModelEditorState.LineData,
    opus_manager: OpusLayerInterface,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    val dialog_visibility = remember { mutableStateOf(false) }
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LineEffectsShow),
        onClick = { dialog_visibility.value = true },
        shape = shape,
        icon = R.drawable.icon_ctl,
        description = R.string.cd_show_effect_controls
    )

    LineEffectMenuDialog(
        dialog_visibility,
        active_line,
        opus_manager,
        vm_state
    )
}

@Composable
fun InsertLineButton(
    active_line: ViewModelEditorState.LineData,
    opus_manager: OpusLayerInterface,
    dialog_value: MutableIntState,
    shape: Shape = Shapes.ContextMenuButtonPrimaryStart
) {
    val visibility = remember { mutableStateOf(false) }
    Box {
        IconCMenuButton(
            modifier = Modifier.testTag(TestTag.LineNew),
            onClick = {
                val channel = active_line.channel.value ?: return@IconCMenuButton
                val line_offset = active_line.line_offset.value ?: return@IconCMenuButton
                opus_manager.new_line(channel, line_offset + 1)
            },
            onLongClick = {
                visibility.value = true
            },
            icon = R.drawable.icon_add,
            shape = shape,
            description = R.string.cd_insert_line
        )

        IntegerInputDialog(
            R.string.dlg_insert_lines,
            visibility,
            dialog_value,
            Values.DialogInput.Min.InsertLine,
            max_value = Values.DialogInput.Max.InsertLine
        ) { i ->
            val channel = active_line.channel.value ?: return@IntegerInputDialog
            val line_offset = active_line.line_offset.value ?: return@IntegerInputDialog
            opus_manager.new_line_repeat(channel, line_offset, i)
        }
    }
}

@Composable
fun RemoveLineButton(
    active_line: ViewModelEditorState.LineData,
    opus_manager: OpusLayerInterface,
    dialog_value: MutableIntState,
    size: Int
) {
    val visibility = remember { mutableStateOf(false) }
    Box {
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

        IntegerInputDialog(
            R.string.dlg_remove_lines,
            visibility,
            dialog_value,
            0,
        ) { i ->
            val channel = active_line.channel.value ?: return@IntegerInputDialog
            val line_offset = active_line.line_offset.value ?: return@IntegerInputDialog
            opus_manager.remove_line_repeat(channel, line_offset, i)
        }
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
            active_line.ctl_type.value?.let { type ->
                val channel = active_line.channel.value
                val line_offset = active_line.line_offset.value
                if (channel == null) {
                    opus_manager.remove_global_controller(type)
                } else if (line_offset == null) {
                    opus_manager.remove_channel_controller(type, channel)
                } else {
                    opus_manager.remove_line_controller(type, channel, line_offset)
                }
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
            } else if (assigned_offset < midi_instruments.size) {
                midi_instruments[assigned_offset]
            } else {
                stringResource(R.string.unknown_instrument, assigned_offset)
            }
        } else {
            "!${"%02d".format(assigned_offset)}"
        }
        val dialog_visibility = remember { mutableStateOf(false) }

        key(vm_state.soundfont_active.value) {
            TextCMenuButton(
                modifier = modifier.testTag(TestTag.InstrumentSet),
                onClick = {
                    dialog_visibility.value = true
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
        PercussionInstrumentDialog(
            dialog_visibility,
            vm_state,
            opus_manager,
            active_line.channel.value!!,
            active_line.line_offset.value!!
        )
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
fun PercussionInstrumentDialog(visibility: MutableState<Boolean>, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, channel: Int, line_offset: Int) {
    val context = LocalContext.current
    val gen_options = { options: MutableList<Pair<Int, @Composable RowScope.() -> Unit>> ->
        var use_defaults = true
        if (!vm_state.use_midi_playback.value) {
            val preset = opus_manager.get_channel_instrument(channel)
            vm_state.get_available_instruments(preset)?.let { instruments ->
                for ((name, index) in instruments) {
                    if (index < 0) continue
                    options.add(
                        Pair(index) {
                            Text("$index:")
                            Text(
                                name,
                                modifier = Modifier.weight(1F),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Box(
                                Modifier
                                    .clickable {
                                        if (context is ComponentActivityEditor) {
                                            context.play_event(channel, index, .7F)
                                        }
                                    }
                                    .height(Dimensions.PreviewIconHeight)
                                    .width(Dimensions.PreviewIconHeight),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icon_volume),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(
                                            top = Dimensions.PreviewIconPadding,
                                            bottom = Dimensions.PreviewIconPadding,
                                            start = Dimensions.PreviewIconPadding
                                        )
                                        .height(Dimensions.PreviewIconHeight - (Dimensions.PreviewIconPadding * 2))
                                        .width(Dimensions.PreviewIconHeight - (Dimensions.PreviewIconPadding))
                                )
                            }
                        }
                    )
                }
                use_defaults = false
            }
        }

        if (use_defaults) {
            for (i in 0..60) {
                options.add(
                    Pair(i, { Text("$i: ${stringArrayResource(R.array.midi_drums)[i]}") })
                )
            }
        }
    }

    val row_index = opus_manager.get_visible_row_from_pair(channel, line_offset)
    val current_instrument = vm_state.line_data[row_index].assigned_offset.value
    DialogMenu(
        visibility,
        R.string.dropdown_choose_instrument,
        gen_options,
        default = current_instrument,
        callback = {
            opus_manager.percussion_set_instrument(channel, line_offset, it)
            opus_manager.vm_controller.play_event(channel, it)
        }
    )
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
    Box(contentAlignment = Alignment.BottomEnd) {
        IconCMenuButton(
            modifier = Modifier.testTag(TestTag.LineColor),
            onClick = { visibility.value = true },
            shape = shape,
            icon = R.drawable.icon_palette,
            description = R.string.cd_line_mute
        )

        ui_facade.line_data[line_offset].palette.value?.event?.let { color ->
            Spacer(
                Modifier
                    .padding(
                        end = Dimensions.PaletteDotPaddingEnd,
                        bottom = Dimensions.PaletteDotPaddingBottom
                    )
                    .size(
                        Dimensions.PaletteDotSize,
                        Dimensions.PaletteDotSize
                    )
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }

    if (visibility.value) {
        val default_color = opus_manager.get_channel(channel).lines[line_offset].palette.event
            ?: opus_manager.get_channel(channel).palette.event
            ?: Colors.active_color_scheme.leaf

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
            active_line.ctl_type.value?.let { type ->
                val channel = active_line.channel.value
                val line_offset = active_line.line_offset.value
                if (channel == null) {
                    opus_manager.set_global_controller_visibility(type, false)
                } else if (line_offset == null) {
                    opus_manager.set_channel_controller_visibility(type, channel, false)
                } else {
                    opus_manager.set_line_controller_visibility(type, channel, line_offset, false)
                }
            }
        },
        icon = R.drawable.icon_hide,
        shape = shape,
        description = R.string.cd_hide_control_line
    )
}

@Composable
fun LineEffectMenuDialog(
    visibility: MutableState<Boolean>,
    active_line: ViewModelEditorState.LineData,
    opus_manager: OpusLayerInterface,
    vm_state: ViewModelEditorState,
) {
    val subdialog_visibility = remember { mutableStateOf(false) }
    val subdialog_ctl_type = remember { mutableStateOf<EffectType?>(null) }

    DialogMenu(
        visibility = visibility,
        title = R.string.show_line_controls,
        options = { options ->
            val available_effects = OpusLayerInterface.line_controller_domain.toMutableList()
            for (line in vm_state.line_data) {
                if (line.channel.value != active_line.channel.value) continue
                if (line.line_offset.value != active_line.line_offset.value) continue
                val ctl_type = line.ctl_type.value ?: continue
                if (!available_effects.contains(ctl_type)) continue

                available_effects.remove(ctl_type)
            }

            for (ctl_type in available_effects) {
                options.add( Pair(ctl_type) { EffectMenuItem(ctl_type) } )
            }
        },
        long_click_callback = {
            subdialog_ctl_type.value = it
            subdialog_visibility.value = true
        },
        callback = {
            opus_manager.set_line_controller_visibility(
                type = it,
                channel_index = active_line.channel.value!!,
                line_offset = active_line.line_offset.value!!,
                visibility = true
            )
        }
    )

    PaganDialog(subdialog_visibility) {
        Icon(
            modifier = Modifier.height(Dimensions.EffectDialogIconHeight),
            painter = painterResource(EffectResourceMap[subdialog_ctl_type.value!!].icon),
            contentDescription = stringResource(EffectResourceMap[subdialog_ctl_type.value!!].name)
        )
        LargeSpacer()
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                opus_manager.set_line_controller_visibility(
                    type = subdialog_ctl_type.value!!,
                    channel_index = active_line.channel.value!!,
                    line_offset = active_line.line_offset.value!!,
                    visibility = true
                )

                subdialog_visibility.value = false
                subdialog_ctl_type.value = null
                visibility.value = false
            },
            content = { Text(stringResource(R.string.show_line_controls_this)) },
        )
        LargeSpacer()
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                opus_manager.set_all_line_controller_visibility(
                    type = subdialog_ctl_type.value!!,
                    channel = active_line.channel.value!!
                )

                subdialog_visibility.value = false
                subdialog_ctl_type.value = null
                visibility.value = false
            },
            content = {
                Text(
                    stringResource(R.string.show_line_controls_channel),
                    maxLines = 1
                )
            },
        )
        LargeSpacer()
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                opus_manager.set_all_line_controller_visibility(subdialog_ctl_type.value!!)

                subdialog_visibility.value = false
                subdialog_ctl_type.value = null
                visibility.value = false
            },
            content = {
                Text(
                    stringResource(R.string.show_line_controls_all),
                    maxLines = 1
                )
            },
        )
        LargeSpacer()
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                subdialog_visibility.value = false
                subdialog_ctl_type.value = null
            },
            content = { Text(android.R.string.cancel) },
        )
    }
}


@Composable
fun GlobalEffectMenuDialog(
    visibility: MutableState<Boolean>,
    opus_manager: OpusLayerInterface,
    vm_state: ViewModelEditorState,
) {

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
fun ContextMenuLineStdPrimary(
    modifier: Modifier = Modifier,
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface,
    layout: LayoutSize
) {
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
                    vm_state,
                    active_line,
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
                RemoveLineButton(
                    active_line,
                    opus_manager,
                    vm_state.dlg_remove_line,
                    active_channel.size.intValue
                )
                MediumSpacer()
                InsertLineButton(
                    active_line,
                    opus_manager,
                    vm_state.dlg_insert_line,
                    Shapes.ContextMenuButtonPrimaryEnd
                )
            }
        }
        LayoutSize.SmallLandscape,
        LayoutSize.LargeLandscape,
        LayoutSize.MediumLandscape -> {
            Column {
                InsertLineButton(
                    active_line,
                    opus_manager,
                    vm_state.dlg_insert_line
                )

                MediumSpacer()
                RemoveLineButton(
                    active_line,
                    opus_manager,
                    vm_state.dlg_remove_line,
                    active_channel.size.intValue
                )

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
                    vm_state,
                    active_line,
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


