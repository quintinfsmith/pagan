package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
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
fun ToggleLineControllerButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = Modifier.height(dimensionResource(R.dimen.contextmenu_primary_height)),
        onClick = { dispatcher.show_hidden_line_controller() },
        icon = R.drawable.icon_ctl,
        description = R.string.cd_show_effect_controls
    )
}

@Composable
fun InsertLineButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = Modifier.height(dimensionResource(R.dimen.contextmenu_primary_height)),
        onClick = { dispatcher.insert_line(1) },
        onLongClick = { dispatcher.insert_line() },
        icon = R.drawable.icon_insert_line,
        description = R.string.cd_insert_line
    )
}

@Composable
fun RemoveLineButton(dispatcher: ActionTracker, size: Int) {
    IconCMenuButton(
        modifier = Modifier.height(dimensionResource(R.dimen.contextmenu_primary_height)),
        enabled = size > 1,
        onClick = { dispatcher.remove_line(1) },
        onLongClick = { dispatcher.remove_line() },
        icon = R.drawable.icon_remove_line,
        description = R.string.cd_remove_line
    )
}
@Composable
fun PercussionSetInstrumentButton(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, dispatcher: ActionTracker, y: Int) {
    val active_line = vm_state.line_data[y]
    val assigned_offset = active_line.assigned_offset.value ?: return
    val active_channel = vm_state.channel_data[active_line.channel.value!!]
    val label = vm_state.get_instrument_name(active_channel.instrument.value, assigned_offset)

    TextCMenuButton(
        modifier = modifier.height(dimensionResource(R.dimen.contextmenu_primary_height)),
        onClick = { dispatcher.set_percussion_instrument(active_line.channel.value!!, active_line.line_offset.value!!) },
        text = label
    )
}

@Composable
fun ContextMenuLinePrimary(vm_state: ViewModelEditorState, dispatcher: ActionTracker, landscape: Boolean) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]

    Row {
        ToggleLineControllerButton(dispatcher)

        if (active_line.assigned_offset.value != null) {
            PercussionSetInstrumentButton(Modifier.weight(1F), vm_state, dispatcher, cursor.ints[0])
        } else {
            Spacer(Modifier.weight(1F))
        }

        if (active_line.ctl_type.value == null) {
            RemoveLineButton(
                dispatcher,
                vm_state.channel_data[active_line.channel.value!!].size.value
            )
        }

        InsertLineButton(dispatcher)
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
    Row(modifier
        .padding(
            vertical = 1.dp,
            horizontal = 12.dp
        )
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
fun ContextMenuLineStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, volume_event: OpusVolumeEvent, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MuteButton(Modifier.fillMaxHeight(), dispatcher, line)
        VolumeEventMenu(ui_facade, dispatcher, volume_event)
    }
}

