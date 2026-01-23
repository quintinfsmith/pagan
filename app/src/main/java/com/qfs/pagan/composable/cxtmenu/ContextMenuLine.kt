package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.viewmodel.ViewModelEditorState
import com.qfs.pagan.viewmodel.ViewModelPagan

@Composable
fun AdjustLineButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun ToggleLineControllerButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.show_hidden_line_controller() },
        icon = R.drawable.icon_ctl,
        description = R.string.cd_show_effect_controls
    )
}

@Composable
fun InsertLineButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.insert_line(1) },
        onLongClick = { dispatcher.insert_line() },
        icon = R.drawable.icon_add,
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
fun RemoveEffectButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.remove_controller() },
        onLongClick = { dispatcher.remove_controller() },
        icon = R.drawable.icon_trash,
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

    TextCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.set_percussion_instrument(active_line.channel.value!!, active_line.line_offset.value!!) },
        text = label ?: if (active_channel.instrument.value.first == 128) {
            if (vm_state.soundfont_active.value && !vm_state.use_midi_playback.value) {
                stringResource(R.string.unavailable_preset, stringArrayResource(R.array.midi_drums)[assigned_offset])
            } else {
                stringArrayResource(R.array.midi_drums)[assigned_offset]
            }
        } else {
            "${stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.first]} @${assigned_offset}"
        }
    )
}

@Composable
fun MuteButton(dispatcher: ActionTracker, line: ViewModelEditorState.LineData) {
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
        description = R.string.cd_line_mute
    )
}

@Composable
fun HideEffectButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.toggle_controller_visibility() },
        icon = R.drawable.icon_hide,
        description = R.string.cd_hide_control_line
    )
}

@Composable
fun ContextMenuLinePrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, layout: ViewModelPagan.LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]
    if (active_line.ctl_type.value == null) {
        ContextMenuLineStdPrimary(modifier, vm_state, dispatcher, layout)
    } else {
        ContextMenuLineCtlPrimary(modifier, vm_state, dispatcher, layout)
    }
}

@Composable
fun ContextMenuLineStdPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, layout: ViewModelPagan.LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]
    val active_channel = vm_state.channel_data[active_line.channel.value!!]
    when (layout) {
        ViewModelPagan.LayoutSize.SmallPortrait,
        ViewModelPagan.LayoutSize.MediumPortrait,
        ViewModelPagan.LayoutSize.LargePortrait,
        ViewModelPagan.LayoutSize.XLargePortrait,
        ViewModelPagan.LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                ToggleLineControllerButton(dispatcher)

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
                InsertLineButton(dispatcher)
            }
        }
        ViewModelPagan.LayoutSize.SmallLandscape,
        ViewModelPagan.LayoutSize.LargeLandscape,
        ViewModelPagan.LayoutSize.MediumLandscape -> {
            Column(Modifier.width(dimensionResource(R.dimen.contextmenu_button_width))) {
                InsertLineButton(dispatcher)

                CMPadding()
                RemoveLineButton(dispatcher, active_channel.size.intValue)

                if (active_line.assigned_offset.value != null) {
                    CMPadding()
                    PercussionSetInstrumentButton(
                        Modifier
                            .height(Dimensions.ButtonHeight.Normal)
                            .fillMaxWidth(),
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

                ToggleLineControllerButton(dispatcher)
            }
        }
    }
}
@Composable
fun ContextMenuLineCtlPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, layout: ViewModelPagan.LayoutSize) {
    when (layout) {
        ViewModelPagan.LayoutSize.SmallPortrait,
        ViewModelPagan.LayoutSize.MediumPortrait,
        ViewModelPagan.LayoutSize.LargePortrait,
        ViewModelPagan.LayoutSize.XLargePortrait,
        ViewModelPagan.LayoutSize.XLargeLandscape -> {
            ContextMenuPrimaryRow(modifier) {
                HideEffectButton(dispatcher)
                Spacer(Modifier.weight(1F))
                RemoveEffectButton(dispatcher)
            }
        }
        ViewModelPagan.LayoutSize.SmallLandscape,
        ViewModelPagan.LayoutSize.LargeLandscape,
        ViewModelPagan.LayoutSize.MediumLandscape -> {
            Column(Modifier.width(dimensionResource(R.dimen.contextmenu_button_width))) {
                RemoveEffectButton(dispatcher)
                Spacer(Modifier.weight(1F))
                HideEffectButton(dispatcher)
            }
        }
    }
}

@Composable
fun ContextMenuLineSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    val initial_event = ui_facade.active_event.value?.copy()
    if (line.ctl_type.value == null) {
        ContextMenuLineStdSecondary(ui_facade, dispatcher, initial_event as OpusVolumeEvent, modifier = modifier)
    } else {
        ContextMenuLineCtlSecondary(ui_facade, dispatcher, initial_event as EffectEvent, modifier = modifier)
    }
}

@Composable
fun ContextMenuLineCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, initial_event: EffectEvent, modifier: Modifier = Modifier) {
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
fun ContextMenuLineStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, volume_event: OpusVolumeEvent, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]

    ContextMenuSecondaryRow {
        MuteButton(dispatcher, line)
        CMPadding()
        VolumeEventMenu(ui_facade, dispatcher, volume_event)
    }
}


