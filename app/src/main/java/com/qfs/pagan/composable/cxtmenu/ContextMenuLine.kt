package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
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
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun AdjustLineButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun ToggleLineControllerButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.show_hidden_line_controller() },
        icon = R.drawable.icon_ctl,
        description = R.string.cd_show_effect_controls
    )
}

@Composable
fun InsertLineButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.insert_line(1) },
        onLongClick = { dispatcher.insert_line() },
        icon = R.drawable.icon_insert_line,
        description = R.string.cd_insert_line
    )
}

@Composable
fun RemoveLineButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, size: Int) {
    IconCMenuButton(
        modifier = modifier,
        enabled = size > 1,
        onClick = { dispatcher.remove_line(1) },
        onLongClick = { dispatcher.remove_line() },
        icon = R.drawable.icon_remove_line,
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
        text = label
    )
}

@Composable
fun MuteButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, line: ViewModelEditorState.LineData) {
    IconCMenuButton(
        modifier = modifier,
        onClick = {
            if (line.is_mute.value) {
                dispatcher.line_unmute()
            } else {
                dispatcher.line_mute()
            }
        },
        icon = if (line.is_mute.value) R.drawable.icon_unmute
        else R.drawable.icon_mute,
        description = R.string.cd_line_mute
    )
}

@Composable
fun ContextMenuLinePrimary(vm_state: ViewModelEditorState, dispatcher: ActionTracker, landscape: Boolean) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]

    if (landscape) {
        Column(Modifier.width(dimensionResource(R.dimen.contextmenu_button_width))) {
            if (active_line.assigned_offset.value != null) {
                PercussionSetInstrumentButton(
                    Modifier
                        .height(dimensionResource(R.dimen.contextmenu_button_height))
                        .fillMaxWidth(),
                    vm_state,
                    dispatcher,
                    cursor.ints[0],
                    false
                )
            }

            if (active_line.ctl_type.value == null) {
                RemoveLineButton(
                    Modifier
                        .height(dimensionResource(R.dimen.contextmenu_button_height))
                        .fillMaxWidth(),
                    dispatcher,
                    vm_state.channel_data[active_line.channel.value!!].size.intValue
                )
            }

            InsertLineButton(
                Modifier
                    .height(dimensionResource(R.dimen.contextmenu_button_height))
                    .fillMaxWidth(),
                dispatcher
            )
            AdjustLineButton(
                Modifier
                    .height(dimensionResource(R.dimen.contextmenu_button_height))
                    .fillMaxWidth(),
                dispatcher
            )
            Spacer(Modifier.weight(1F))
            ToggleLineControllerButton(
                Modifier
                    .height(dimensionResource(R.dimen.contextmenu_button_height))
                    .fillMaxWidth(),
                dispatcher
            )
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.contextmenu_primary_height))
        ) {
            ToggleLineControllerButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            CMPadding()

            if (active_line.assigned_offset.value != null) {
                PercussionSetInstrumentButton(
                    Modifier
                        .fillMaxHeight()
                        .weight(1F),
                    vm_state,
                    dispatcher,
                    cursor.ints[0],
                    true
                )
                CMPadding()
            } else {
                Spacer(Modifier.weight(1F))
            }

            AdjustLineButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            CMPadding()

            if (active_line.ctl_type.value == null) {
                RemoveLineButton(
                    Modifier
                        .fillMaxHeight()
                        .width(dimensionResource(R.dimen.contextmenu_button_width)),
                    dispatcher,
                    vm_state.channel_data[active_line.channel.value!!].size.intValue
                )
                CMPadding()
            }

            InsertLineButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
        }
    }
}

@Composable
fun ContextMenuLineSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    val initial_event = ui_facade.active_event.value
    if (line.ctl_type.value == null) {
        ContextMenuLineStdSecondary(ui_facade, dispatcher, initial_event as OpusVolumeEvent, modifier = modifier)
    } else {
        ContextMenuLineCtlSecondary(ui_facade, dispatcher, initial_event as EffectEvent, modifier = modifier)
    }
}

@Composable
fun ContextMenuLineCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, initial_event: EffectEvent, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(dimensionResource(R.dimen.contextmenu_button_height))
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    Row(
        modifier = modifier
            .height(dimensionResource(R.dimen.contextmenu_button_height))
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MuteButton(
            Modifier
                .fillMaxHeight()
                .width(dimensionResource(R.dimen.contextmenu_button_width)),
            dispatcher,
            line
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))
        VolumeEventMenu(ui_facade, dispatcher, volume_event)
    }
}

