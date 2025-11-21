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
import kotlin.collections.get

@Composable
fun ContextMenuLinePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val active_line = ui_facade.line_data[cursor.ints[0]]

    Row {
        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.show_hidden_line_controller() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_ctl),
                    contentDescription = stringResource(R.string.cd_show_effect_controls)
                )
            }
        )

        active_line.assigned_offset?.let {
            Button(
                modifier = Modifier.weight(1F),
                onClick = { dispatcher.set_percussion_instrument() },
                content = {
                    Text(ui_facade.instrument_names[active_line.channel]?.get(it) ?: "???")
                }
            )
        } ?: Spacer(Modifier.weight(1F))

        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.remove_line() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_remove_line),
                    contentDescription = stringResource(R.string.cd_remove_line)
                )
            }
        )

        Button(
            modifier = Modifier.width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.insert_line() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_insert_line),
                    contentDescription = stringResource(R.string.cd_insert_line)
                )
            }
        )
    }
}

@Composable
fun ContextMenuLineSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val y = cursor.ints[0]
    val line = ui_facade.line_data[y]
    val icon_resource = if (line.is_mute) {
        R.drawable.icon_unmute
    } else {
        R.drawable.icon_mute
    }
    Row {
        Button(
            onClick = { dispatcher.line_mute() },
            content = {
                Icon(
                    painter = painterResource(icon_resource),
                    contentDescription = stringResource(R.string.cd_line_mute)
                )
            }
        )
        Text("TODO: Volume Widget")
    }
}


