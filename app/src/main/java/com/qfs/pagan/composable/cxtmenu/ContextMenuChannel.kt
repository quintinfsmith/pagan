package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ContextMenuChannelPrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val channel_index = cursor.ints[0]

    Row(Modifier.height(dimensionResource(R.dimen.icon_button_height))) {
        IconCMenuButton(
            onClick = { dispatcher.show_hidden_channel_controller() },
            icon = R.drawable.icon_ctl,
            description = R.string.cd_show_effect_controls
        )

        Spacer(Modifier.weight(1F))

        IconCMenuButton(
            onClick = { dispatcher.adjust_selection() },
            icon = R.drawable.icon_adjust,
            description = R.string.cd_adjust_selection
        )

        IconCMenuButton(
            onClick = { dispatcher.remove_channel() },
            icon = R.drawable.icon_remove_channel,
            description = R.string.cd_remove_channel
        )

        IconCMenuButton(
            onClick = { dispatcher.insert_percussion_channel() },
            icon = R.drawable.icon_add_channel_kit,
            description = R.string.cd_insert_channel_percussion
        )
        IconCMenuButton(
            onClick = { dispatcher.insert_channel() },
            icon = R.drawable.icon_add_channel,
            description = R.string.cd_insert_channel
        )
    }
}

@Composable
fun ContextMenuChannelSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val channel_index = cursor.ints[0]
    val active_channel = ui_facade.channel_data[channel_index]

    Row(modifier = Modifier.height(dimensionResource(R.dimen.contextmenu_secondary_button_height))) {
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
        Button(
            modifier = Modifier
                .padding(3.dp)
                .fillMaxSize()
                .weight(1f),
            onClick = { dispatcher.set_channel_preset(channel_index) },
            content = { Text(ui_facade.available_preset_names?.get(active_channel.instrument.value) ?: "???") }
        )
    }
}
