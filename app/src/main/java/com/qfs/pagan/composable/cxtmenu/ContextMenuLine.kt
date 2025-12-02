package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.BetterButton
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ContextMenuLinePrimary(vm_state: ViewModelEditorState, dispatcher: ActionTracker) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]

    Row(Modifier.height(dimensionResource(R.dimen.icon_button_height))) {
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.show_hidden_line_controller() },
            icon = R.drawable.icon_ctl,
            description = R.string.cd_show_effect_controls
        )

        active_line.assigned_offset.value?.let {
            val active_channel = vm_state.channel_data[active_line.channel.value!!]
            val label = vm_state.get_instrument_name(active_channel.instrument.value, it)
            BetterButton(
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier
                    .padding(3.dp)
                    .height(dimensionResource(R.dimen.icon_button_height))
                    .weight(1F),
                onClick = { dispatcher.set_percussion_instrument(active_line.channel.value!!, active_line.line_offset.value!!) },
                content = {
                    Text(label)
                }
            )
        } ?: Spacer(Modifier.weight(1F))

        IconCMenuButton(
            onClick = { dispatcher.remove_line(1) },
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { dispatcher.remove_line() }
                ),
            icon = R.drawable.icon_remove_line,
            description = R.string.cd_remove_line
        )
        IconCMenuButton(
            onClick = { dispatcher.insert_line(1) },
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .combinedClickable(
                    onClick = {},
                    onLongClick = { dispatcher.insert_line() }
                ),
            icon = R.drawable.icon_insert_line,
            description = R.string.cd_insert_line
        )
    }
}

@Composable
fun ContextMenuLineSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    val initial_event = ui_facade.active_event.value
    if (line.ctl_type.value == null) {
        ContextMenuLineStdSecondary(ui_facade, dispatcher, initial_event as OpusVolumeEvent)
    } else {
        ContextMenuLineCtlSecondary(ui_facade, dispatcher, initial_event as EffectEvent)
    }
}

@Composable
fun ContextMenuLineCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, initial_event: EffectEvent) {
    val height = dimensionResource(R.dimen.contextmenu_secondary_button_height)
    Row(modifier = Modifier.height(height)) {
        when (initial_event) {
            is OpusVolumeEvent -> VolumeEventMenu(modifier = Modifier.height(height), ui_facade, dispatcher, initial_event)
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
fun ContextMenuLineStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, volume_event: OpusVolumeEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    Row(
        modifier = Modifier.padding(4.dp)
    ) {
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.contextmenu_secondary_button_height)),
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
        VolumeEventMenu(
            modifier = Modifier.height(dimensionResource(R.dimen.contextmenu_secondary_button_height)),
            ui_facade, dispatcher, volume_event)
    }
}

