package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.uibill.UIFacade
import kotlin.collections.get

@Composable
fun ContextMenuLinePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val active_line = ui_facade.line_data[cursor.ints[0]]

    Row(Modifier.height(dimensionResource(R.dimen.icon_button_height))) {
        Button(
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier
                .padding(2.dp)
                .width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.show_hidden_line_controller() },
            content = {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.icon_ctl),
                    contentDescription = stringResource(R.string.cd_show_effect_controls)
                )
            }
        )

        active_line.assigned_offset?.let {
            Button(
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxHeight()
                    .weight(1F),
                onClick = { dispatcher.set_percussion_instrument() },
                content = {
                    Text(ui_facade.instrument_names[active_line.channel]?.get(it) ?: "???")
                }
            )
        } ?: Spacer(Modifier.weight(1F))

        Button(
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier
                .padding(3.dp)
                .width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.remove_line() },
            content = {
                Icon(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.icon_remove_line),
                    contentDescription = stringResource(R.string.cd_remove_line)
                )
            }
        )

        Button(
            contentPadding = PaddingValues(10.dp),
            modifier = Modifier
                .padding(3.dp)
                .width(dimensionResource(R.dimen.icon_button_width)),
            onClick = { dispatcher.insert_line() },
            content = {
                Icon(
                    modifier = Modifier.fillMaxSize(),
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
    Row {
        Button(
            onClick = {
                if (line.is_mute) {
                    dispatcher.line_unmute()
                } else {
                    dispatcher.line_mute()
                }
            },
            content = {
                Icon(
                    painter = if (line.is_mute) {
                        painterResource(R.drawable.icon_unmute)
                    } else {
                        painterResource(R.drawable.icon_mute)
                    },
                    contentDescription = stringResource(R.string.cd_line_mute)
                )
            }
        )
        Text("TODO: Volume Widget")
    }
}


