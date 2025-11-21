package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.uibill.UIFacade

@Composable
fun ContextMenuSinglePrimary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val active_event = ui_facade.active_event.value
    val octave = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note / ui_facade.radix.value
        is RelativeNoteEvent -> active_event.offset / ui_facade.radix.value
        is PercussionEvent -> 0
        null -> return
        else -> throw Exception("Invalid Event Type") // TODO: Specify
    }

    Column(Modifier.Companion.background(Color.Companion.Red)) {
        Row {
            Button(
                onClick = {},
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .weight(1F)
                    .combinedClickable(
                        onClick = { dispatcher.split(2) },
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
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .weight(1F)
                    .combinedClickable(
                        onClick = { dispatcher.insert_leaf(1) },
                        onLongClick = { dispatcher.insert_leaf() }
                    )
            )
            Button(
                onClick = { dispatcher.remove_at_cursor() },
                modifier = Modifier.Companion
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
                modifier = Modifier.Companion
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
                modifier = Modifier.Companion
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
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .background(color = if (octave == i) Color.Companion.Green else colorResource(R.color.ns_default))
                        .weight(1F),
                    onClick = { dispatcher.set_octave(i) },
                    content = { Text("$i", maxLines = 1) }
                )
            }
        }
    }
}

@Composable
fun ContextMenuSingleSecondary(ui_facade: UIFacade, dispatcher: ActionTracker) {
    val active_event = ui_facade.active_event.value
    val offset = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note % ui_facade.radix.value
        is RelativeNoteEvent -> active_event.offset % ui_facade.radix.value
        is PercussionEvent -> 0
        null -> return
        else -> throw Exception("Invalid Event Type") // TODO: Specify
    }

    // Offset Selector
    Row {
        for (i in 0 until ui_facade.radix.value) {
            TextButton(
                onClick = { dispatcher.set_offset(i) },
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .background(color = if (offset == i) Color.Companion.Green else Color.Companion.Transparent)
                    .weight(1F),
                content = { Text("$i", maxLines = 1) }
            )
        }
    }
}
