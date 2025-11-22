package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.uibill.UIFacade

@Composable
fun ContextMenuChannelPrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val channel_index = cursor.ints[0]
    val active_channel = ui_facade.channel_data[channel_index]

    Row {
        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.show_hidden_channel_controller() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_ctl),
                    contentDescription = stringResource(R.string.cd_show_effect_controls)
                )
            }
        )

        Spacer(Modifier.weight(1F))

        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.adjust_selection() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_adjust),
                    contentDescription = stringResource(R.string.cd_adjust_selection)
                )
            }
        )

        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.remove_channel() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_remove_channel),
                    contentDescription = stringResource(R.string.cd_remove_channel)
                )
            }
        )

        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.insert_percussion_channel() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_add_channel_kit),
                    contentDescription = stringResource(R.string.cd_insert_channel_percussion)
                )
            }
        )
        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.insert_channel() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_add_channel),
                    contentDescription = stringResource(R.string.cd_insert_channel)
                )
            }
        )
    }
}

@Composable
fun ContextMenuChannelSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val channel_index = cursor.ints[0]
    val active_channel = ui_facade.channel_data[channel_index]

    val icon_resource = if (active_channel.is_mute) {
        R.drawable.icon_unmute
    } else {
        R.drawable.icon_mute
    }

    Row {
        Button(
            onClick = {
                if (active_channel.is_mute) {
                    dispatcher.channel_unmute()
                } else {
                    dispatcher.channel_mute()
                }
            },
            content = {
                Icon(
                    painter = painterResource(icon_resource),
                    contentDescription = stringResource(R.string.cd_line_mute)
                )
            }
        )
        Button(
            onClick = {
                dispatcher.set_channel_instrument(channel_index)
            },
            content = {
                Text("TODO")
            }
        )
    }
}
