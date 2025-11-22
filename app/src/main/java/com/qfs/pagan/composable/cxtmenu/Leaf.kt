package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
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
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.SText
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.uibill.UIFacade

@Composable
fun ContextMenuSinglePrimary(ui_facade: UIFacade, dispatcher: ActionTracker, show_relative_input: Boolean) {
    val active_event = ui_facade.active_event.value
    val cursor = ui_facade.active_cursor.value ?: return
    val active_line = ui_facade.line_data[cursor.ints[0]]

    val octave = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note / ui_facade.radix.value
        is RelativeNoteEvent -> active_event.offset / ui_facade.radix.value
        is PercussionEvent -> 0
        null -> null
        else -> throw Exception("Invalid Event Type") // TODO: Specify
    }

    Column() {
        Row {
            if (show_relative_input) {
                Column {
                    SText(R.string.absolute_label)
                    Text("+")
                    Text("-")
                }
            }
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensionResource(R.dimen.icon_button_height)),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { dispatcher.split(2) },
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .padding(3.dp)
                            .width(dimensionResource(R.dimen.icon_button_width))
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
                    Button(
                        onClick = { dispatcher.insert_leaf(1) },
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .padding(3.dp)
                            .width(dimensionResource(R.dimen.icon_button_width))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { dispatcher.insert_leaf() }
                            ),
                        content = {
                            Icon(
                                painter = painterResource(R.drawable.icon_insert),
                                contentDescription = stringResource(R.string.btn_insert),
                            )
                        }
                    )
                    Button(
                        onClick = { dispatcher.remove_at_cursor() },
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .padding(3.dp)
                            .width(dimensionResource(R.dimen.icon_button_width)),
                        content = {
                            Icon(
                                painter = painterResource(R.drawable.icon_remove),
                                contentDescription = stringResource(R.string.btn_remove)
                            )
                        }
                    )
                    Button(
                        onClick = { dispatcher.set_duration() },
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .padding(3.dp)
                            .fillMaxHeight()
                            .width(dimensionResource(R.dimen.icon_button_width))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { dispatcher.set_duration(1) }
                            ),
                        content = { Text("x${active_event?.duration ?: 1}") }
                    )
                    Button(
                        onClick = {
                            if (active_line.assigned_offset != null) {
                                dispatcher.toggle_percussion()
                            } else if (active_event != null) {
                                dispatcher.unset()
                            }
                        },
                        contentPadding = PaddingValues(10.dp),
                        modifier = Modifier
                            .padding(3.dp)
                            .width(dimensionResource(R.dimen.icon_button_width))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { dispatcher.unset_root() }
                            ),
                        content = {
                            if (active_line.assigned_offset != null) {
                                Icon(
                                    painter = painterResource(
                                        if (active_event != null) R.drawable.icon_unset
                                        else R.drawable.icon_set_percussion
                                    ),
                                    contentDescription = stringResource(R.string.set_percussion_event)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.icon_unset),
                                    contentDescription = stringResource(R.string.btn_unset)
                                )
                            }
                        }
                    )
                }
            }
        }

        if (ui_facade.line_data[cursor.ints[0]].assigned_offset == null) {
            // Octave Selector
            Row {
                for (i in 0 until 8) {
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1F),
                        colors = ButtonColors(
                            containerColor = if (octave != i) {
                                colorResource(R.color.ns_default)
                            } else {
                                colorResource(R.color.number_selector_highlight)
                            },
                            contentColor = if (octave != i) {
                                colorResource(R.color.ns_default_text)
                            } else {
                                colorResource(R.color.ns_selected_text)
                            },
                            disabledContentColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        onClick = { dispatcher.set_octave(i) },
                        content = { Text("$i", maxLines = 1) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContextMenuSingleSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val active_event = ui_facade.active_event.value
    val offset = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note % ui_facade.radix.value
        is RelativeNoteEvent -> active_event.offset % ui_facade.radix.value
        is PercussionEvent -> 0
        null -> 0
        else -> throw Exception("Invalid Event Type") // TODO: Specify
    }
    if (ui_facade.line_data[cursor.ints[0]].assigned_offset != null) return

    // Offset Selector
    Row {
        for (i in 0 until ui_facade.radix.value) {
            Button(
                onClick = { dispatcher.set_offset(i) },
                colors = ButtonColors(
                    containerColor = if (offset != i) {
                        colorResource(R.color.ns_alt)
                    } else {
                        colorResource(R.color.number_selector_highlight)
                    },
                    contentColor = if (offset != i) {
                        colorResource(R.color.ns_default_text)
                    } else {
                        colorResource(R.color.ns_selected_text)
                    },
                    disabledContentColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                content = { Text("$i", maxLines = 1) }
            )
        }
    }
}
