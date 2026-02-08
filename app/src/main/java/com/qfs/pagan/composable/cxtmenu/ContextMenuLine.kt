/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.composable.effectwidget.DelayEventMenu
import com.qfs.pagan.composable.effectwidget.PanEventMenu
import com.qfs.pagan.composable.effectwidget.ReverbEventMenu
import com.qfs.pagan.composable.effectwidget.TempoEventMenu
import com.qfs.pagan.composable.effectwidget.VelocityEventMenu
import com.qfs.pagan.composable.effectwidget.VolumeEventMenu
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun AdjustLineButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.adjust_selection() },
        shape = Shapes.ContextMenuButtonPrimary,
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun ToggleLineControllerButton(
    dispatcher: ActionTracker,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        onClick = { dispatcher.show_hidden_line_controller() },
        shape = shape,
        icon = R.drawable.icon_ctl,
        description = R.string.cd_show_effect_controls
    )
}

@Composable
fun InsertLineButton(dispatcher: ActionTracker, shape: Shape = Shapes.ContextMenuButtonPrimaryStart) {
    IconCMenuButton(
        onClick = { dispatcher.insert_line(1) },
        onLongClick = { dispatcher.insert_line() },
        icon = R.drawable.icon_add,
        shape = shape,
        description = R.string.cd_insert_line
    )
}

@Composable
fun RemoveLineButton(dispatcher: ActionTracker, size: Int) {
    IconCMenuButton(
        enabled = size > 1,
        onClick = { dispatcher.remove_line(1) },
        onLongClick = { dispatcher.remove_line() },
        icon = R.drawable.icon_subtract,
        description = R.string.cd_remove_line
    )
}

@Composable
fun RemoveEffectButton(dispatcher: ActionTracker, shape: Shape) {
    IconCMenuButton(
        onClick = { dispatcher.remove_controller() },
        onLongClick = { dispatcher.remove_controller() },
        icon = R.drawable.icon_subtract,
        shape = shape,
        description = R.string.cd_remove_line
    )
}
@Composable
fun PercussionSetInstrumentButton(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, y: Int, use_name: Boolean) {
    val active_line = vm_state.line_data[y]
    val assigned_offset = active_line.assigned_offset.value ?: return
    val active_channel = vm_state.channel_data[active_line.channel.value!!]
    val label = if (use_name) {
        vm_state.get_instrument_name(active_channel.instrument.value, assigned_offset)
    } else {
        "!${"%02d".format(assigned_offset)}"
    }
    key(vm_state.soundfont_active.value) {
        TextCMenuButton(
            modifier = modifier,
            onClick = {
                dispatcher.set_percussion_instrument(
                    active_line.channel.value!!,
                    active_line.line_offset.value!!
                )
            },
            text = label ?: if (active_channel.instrument.value.first == 128) {
                val midi_instruments = stringArrayResource(R.array.midi_drums)
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
                "${stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.first]} @${assigned_offset}"
            }
        )
    }
}

@Composable
fun SetLineColorButton(
    modifier: Modifier = Modifier,
    ui_facade: ViewModelEditorState,
    dispatcher: ActionTracker,
    channel: Int,
    line_offset: Int,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {

    IconCMenuButton(
        onClick = { dispatcher.set_line_color(channel, line_offset) },
        shape = shape,
        icon = R.drawable.icon_palette,
        description = R.string.cd_line_mute
    )
}

@Composable
fun MuteButton(
    dispatcher: ActionTracker,
    line: ViewModelEditorState.LineData,
) {
    IconCMenuButton(
        onClick = {
            if (line.is_mute.value) {
                dispatcher.line_unmute()
            } else {
                dispatcher.line_mute()
            }
        },
        icon = if (!line.is_mute.value) R.drawable.icon_unmute
        else R.drawable.icon_mute,
        shape = Shapes.ContextMenuSecondaryButtonStart,
        description = R.string.cd_line_mute
    )
}

@Composable
fun HideEffectButton(dispatcher: ActionTracker, shape: Shape) {
    IconCMenuButton(
        onClick = { dispatcher.toggle_controller_visibility() },
        icon = R.drawable.icon_hide,
        shape = shape,
        description = R.string.cd_hide_control_line
    )
}

@Composable
fun ContextMenuLinePrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]
    if (active_line.ctl_type.value == null) {
        ContextMenuLineStdPrimary(modifier, vm_state, dispatcher, layout)
    } else {
        ContextMenuLineCtlPrimary(modifier, vm_state, dispatcher, layout)
    }
}

@Composable
fun ContextMenuLineStdPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize) {
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
                    dispatcher,
                    shape = Shapes.ContextMenuButtonPrimaryStart
                )

                if (active_line.assigned_offset.value != null) {
                    CMPadding()
                    PercussionSetInstrumentButton(
                        Modifier.weight(1F),
                        vm_state,
                        dispatcher,
                        cursor.ints[0],
                        true
                    )
                } else {
                    Spacer(Modifier.weight(1F))
                    AdjustLineButton(dispatcher)
                }

                CMPadding()
                RemoveLineButton(dispatcher, active_channel.size.intValue)
                CMPadding()
                InsertLineButton(dispatcher, Shapes.ContextMenuButtonPrimaryEnd)
            }
        }
        LayoutSize.SmallLandscape,
        LayoutSize.LargeLandscape,
        LayoutSize.MediumLandscape -> {
            Column {
                InsertLineButton(dispatcher)

                CMPadding()
                RemoveLineButton(dispatcher, active_channel.size.intValue)

                if (active_line.assigned_offset.value != null) {
                    CMPadding()
                    PercussionSetInstrumentButton(
                        Modifier
                            .width(Dimensions.ButtonHeight.Normal)
                            .height(Dimensions.ButtonHeight.Normal),
                        vm_state,
                        dispatcher,
                        cursor.ints[0],
                        false
                    )
                } else {
                    CMPadding()
                    AdjustLineButton(dispatcher)
                }

                Spacer(Modifier.weight(1F))

                ToggleLineControllerButton(
                    dispatcher,
                    shape = Shapes.ContextMenuButtonPrimaryBottom
                )
            }
        }
    }
}
@Composable
fun ContextMenuLineCtlPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize) {
    when (layout) {
        LayoutSize.SmallPortrait,
        LayoutSize.MediumPortrait,
        LayoutSize.LargePortrait,
        LayoutSize.XLargePortrait,
        LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                HideEffectButton(dispatcher, Shapes.ContextMenuButtonPrimaryStart)
                Spacer(Modifier.weight(1F))
                RemoveEffectButton(dispatcher, Shapes.ContextMenuButtonPrimaryEnd)
            }
        }
        LayoutSize.SmallLandscape,
        LayoutSize.LargeLandscape,
        LayoutSize.MediumLandscape -> {
            Column {
                RemoveEffectButton(dispatcher, Shapes.ContextMenuButtonPrimaryStart)
                Spacer(Modifier.weight(1F))
                HideEffectButton(dispatcher, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
    }
}

@Composable
fun ContextMenuLineSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, layout: LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    val initial_event = ui_facade.active_event.value?.copy()
    if (line.ctl_type.value == null) {
        ContextMenuLineStdSecondary(ui_facade, dispatcher, initial_event as OpusVolumeEvent, modifier = modifier, layout = layout)
    } else {
        ContextMenuLineCtlSecondary(ui_facade, dispatcher, initial_event as EffectEvent, modifier = modifier, layout = layout)
    }
}

@Composable
fun ContextMenuLineCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, initial_event: EffectEvent, modifier: Modifier = Modifier, layout: LayoutSize) {
    ContextMenuSecondaryRow {
        when (initial_event) {
            is OpusVolumeEvent -> VolumeEventMenu(ui_facade, dispatcher, initial_event)
            is OpusTempoEvent -> TempoEventMenu(ui_facade, dispatcher, initial_event)
            is OpusPanEvent -> PanEventMenu(ui_facade, dispatcher, initial_event)
            is OpusReverbEvent -> ReverbEventMenu(ui_facade, dispatcher, initial_event)
            is DelayEvent -> DelayEventMenu(ui_facade, dispatcher, initial_event)
            is OpusVelocityEvent -> VelocityEventMenu(ui_facade, dispatcher, initial_event)
            else -> {}
        }
    }
}

@Composable
fun ContextMenuLineStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, volume_event: OpusVolumeEvent, modifier: Modifier = Modifier, layout: LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]

    ContextMenuSecondaryRow {
        MuteButton(dispatcher, line)
        CMPadding()
        VolumeEventMenu(ui_facade, dispatcher, volume_event)
        CMPadding()
        SetLineColorButton(
            Modifier,
            ui_facade,
            dispatcher,
            line.channel.value!!,
            line.line_offset.value!!,
            shape = Shapes.ContextMenuSecondaryButtonEnd
        )
    }
}


