package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.uibill.UIFacade

@Composable
fun ContextMenuColumnPrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = ui_facade.column_data[beat].value
    val button_width = dimensionResource(R.dimen.icon_button_width)

    Row {
        Button(
            onClick = {},
            modifier = Modifier
                .width(button_width)
                .combinedClickable(
                    onClick = { dispatcher.tag_column(beat, null, true) },
                    onLongClick = {
                        dispatcher.tag_column(beat)
                    }
                ),
            content = {
                val (icon_resource, string_resource) = if (column_data.is_tagged) {
                    Pair(
                        R.drawable.icon_untag,
                        R.string.cd_remove_section_mark
                    )
                } else {
                    Pair(
                        R.drawable.icon_tag,
                        R.string.cd_mark_section
                    )
                }
                Icon(
                    painter = painterResource(icon_resource),
                    contentDescription = stringResource(string_resource)
                )
            }
        )
        Spacer(Modifier.weight(1F))
        Button(
            modifier = Modifier.width(button_width),
            onClick = { dispatcher.adjust_selection() },
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_adjust),
                    contentDescription = stringResource(R.string.cd_adjust_selection)
                )
            }
        )
        Button(
            onClick = {},
            modifier = Modifier
                .width(button_width)
                .combinedClickable(
                    onClick = { dispatcher.remove_beat_at_cursor(1) },
                    onLongClick = { dispatcher.remove_beat_at_cursor() }
                ),
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_remove_beat),
                    contentDescription = stringResource(R.string.cd_remove_beat)
                )
            }
        )
        Button(
            onClick = {},
            modifier = Modifier
                .width(button_width)
                .combinedClickable(
                    onClick = { dispatcher.insert_beat_after_cursor(1) },
                    onLongClick = {
                        dispatcher.insert_beat_after_cursor()
                    }
                ),
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_insert_beat),
                    contentDescription = stringResource(R.string.cd_insert_beat)
                )
            }
        )
    }
}

@Composable
fun ContextMenuColumnSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {}

