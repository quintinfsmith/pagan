package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
fun ContextMenuChannelPrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    Row {
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.show_hidden_channel_controller() },
            icon = R.drawable.icon_ctl,
            description = R.string.cd_show_effect_controls
        )

        Spacer(Modifier.weight(1F))

        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.adjust_selection() },
            icon = R.drawable.icon_adjust,
            description = R.string.cd_adjust_selection
        )

        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.remove_channel() },
            icon = R.drawable.icon_remove_channel,
            description = R.string.cd_remove_channel
        )

        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.insert_percussion_channel() },
            icon = R.drawable.icon_add_channel_kit,
            description = R.string.cd_insert_channel_percussion
        )
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.insert_channel() },
            icon = R.drawable.icon_add_channel,
            description = R.string.cd_insert_channel
        )
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

    Row(modifier = modifier) {
        IconCMenuButton(
            modifier = Modifier.fillMaxHeight(),
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
        TextCMenuButton(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            onClick = { dispatcher.set_channel_preset(channel_index) },
            text = active_channel.active_name.value ?: stringResource(R.string.unavailable_preset, stringArrayResource(R.array.general_midi_presets)[active_channel.instrument.value.second])
        )
    }
}

