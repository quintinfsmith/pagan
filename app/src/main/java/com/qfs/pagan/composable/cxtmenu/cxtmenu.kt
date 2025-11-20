package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.uibill.UIFacade

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

@Composable
fun ContextMenuSinglePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val active_event = ui_facade.active_event.value
    val (offset, octave) = when (active_event) {
        is AbsoluteNoteEvent -> {
            Pair(active_event.note / ui_facade.radix.value, active_event.note % ui_facade.radix.value)
        }
        is RelativeNoteEvent -> {
            Pair(active_event.offset / ui_facade.radix.value, active_event.offset % ui_facade.radix.value)
        }
        null -> return
        else -> {
            throw Exception("Invalid Event Type") // TODO: Specify
        }
    }

    Column {
        Row {
            Button(
                onClick = { dispatcher.split(2) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { dispatcher.split() }
                    ),
                content = {
                    Icon(
                        painter = painterResource(R.drawable.icon_split),
                        contentDescription = stringResource(R.string.btn_split)
                    )
                }
            )
            Icon(
                painter = painterResource(R.drawable.icon_insert),
                contentDescription = stringResource(R.string.btn_insert),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .combinedClickable(
                        onClick = { dispatcher.insert_leaf(1) },
                        onLongClick = { dispatcher.insert_leaf() }
                    )
            )
            Button(
                onClick = { dispatcher.remove_at_cursor() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                content = {
                    Icon(
                        painter = painterResource(R.drawable.icon_remove),
                        contentDescription = stringResource(R.string.btn_remove)
                    )
                }
            )
            Button(
                onClick = { dispatcher.set_duration() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { dispatcher.set_duration(1) }
                    ),
                content = { Text("x${active_event.duration}") }
            )
            Button(
                onClick = { dispatcher.unset() },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { dispatcher.unset_root() }
                    ),
                content = {
                    Icon(
                        painter = painterResource(R.drawable.icon_unset),
                        contentDescription = stringResource(R.string.btn_unset)
                    )
                }
            )
        }
        // Octave Selector
        Row {
            for (i in 0 until 8) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = if (octave == i) Color.Green else colorResource(R.color.ns_default))
                        .weight(1F),
                    onClick = { dispatcher.set_octave(i) },
                    content = { Text("$i", maxLines = 1) }
                )
            }
        }

        // Offset Selector
        Row() {
            for (i in 0 until ui_facade.radix.value) {
                TextButton(
                    onClick = { dispatcher.set_offset(i) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = if (offset == i) Color.Green else Color.Transparent)
                        .weight(1F),
                    content = { Text("$i", maxLines = 1) }
                )
            }
        }
    }
}

@Composable
fun ContextMenuSingleSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    Text("SINGLE SECONDARY")
}

@Composable
fun ContextMenuRangePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {

}
@Composable
fun ContextMenuRangeSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {}

@Composable
fun ContextMenuChannelPrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {}
@Composable
fun ContextMenuChannelSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {}
