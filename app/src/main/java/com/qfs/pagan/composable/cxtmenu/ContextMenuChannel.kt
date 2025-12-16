package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ToggleEffectsButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.show_hidden_channel_controller() },
        icon = R.drawable.icon_ctl,
        description = R.string.cd_show_effect_controls
    )
}

@Composable
fun AdjustChannelButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun RemoveChannelButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.remove_channel() },
        icon = R.drawable.icon_remove_channel,
        description = R.string.cd_remove_channel
    )


}

@Composable
fun AddKitButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.insert_percussion_channel() },
        icon = R.drawable.icon_add_channel_kit,
        description = R.string.cd_insert_channel_percussion
    )
}

@Composable
fun AddChannelButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.insert_channel() },
        icon = R.drawable.icon_add_channel,
        description = R.string.cd_insert_channel
    )
}

@Composable
fun MuteChannelButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, active_channel: ViewModelEditorState.ChannelData) {
    IconCMenuButton(
        modifier = modifier,
        onClick = {
            if (active_channel.is_mute.value) {
                dispatcher.channel_unmute()
            } else {
                dispatcher.channel_mute()
            }
        },
        icon = if (active_channel.is_mute.value) R.drawable.icon_unmute
        else R.drawable.icon_mute,
        description = R.string.cd_line_mute
    )
}

@Composable
fun SetPresetButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, channel_index: Int, active_channel: ViewModelEditorState.ChannelData) {
    TextCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.set_channel_preset(channel_index) },
        text = active_channel.active_name.value ?: stringResource(R.string.unavailable_preset, stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.second])
    )
}

@Composable
fun ContextMenuChannelPrimary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, landscape: Boolean) {
    if (landscape) {
        Column(modifier.width(dimensionResource(R.dimen.contextmenu_primary_width))) {
            RemoveChannelButton(Modifier, dispatcher)
            AddKitButton(Modifier, dispatcher)
            AddChannelButton(Modifier, dispatcher)
            AdjustChannelButton(Modifier, dispatcher)
            Spacer(Modifier.weight(1F))
            ToggleEffectsButton(Modifier, dispatcher)
        }
    } else {
        Row(
            modifier.height(dimensionResource(R.dimen.contextmenu_primary_height))
        ) {
            ToggleEffectsButton(
                Modifier.width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            Spacer(Modifier.weight(1F))
            AdjustChannelButton(
                Modifier.width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            RemoveChannelButton(
                Modifier.width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            AddKitButton(
                Modifier.width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            AddChannelButton(
                Modifier.width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
        }
    }
}

@Composable
fun ContextMenuChannelSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    val channel_index = cursor.ints[0]
    val active_channel = try {
        ui_facade.channel_data[channel_index]
    } catch (e: Exception) {
        return
    }

    Row(modifier = modifier.height(dimensionResource(R.dimen.contextmenu_secondary_height))) {
        MuteChannelButton(
            Modifier
                .width(dimensionResource(R.dimen.contextmenu_button_width))
                .fillMaxHeight(),
            dispatcher,
            active_channel
        )
        SetPresetButton(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            dispatcher,
            channel_index,
            active_channel
        )
    }
}

