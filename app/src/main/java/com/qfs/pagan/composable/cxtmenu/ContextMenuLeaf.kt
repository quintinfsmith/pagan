package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.NumberSelectorButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ContextMenuStructureControls(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    val active_event = ui_facade.active_event.value
    val cursor = ui_facade.active_cursor.value ?: return
    val active_line = ui_facade.line_data[cursor.ints[0]]

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconCMenuButton(
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .weight(1F),
            onClick = { dispatcher.split(2) },
            onLongClick = { dispatcher.split() },
            icon = R.drawable.icon_split,
            description = R.string.btn_split
        )
        IconCMenuButton(
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .weight(1F),
            onClick = { dispatcher.insert_leaf(1) },
            onLongClick = { dispatcher.insert_leaf() },
            icon = R.drawable.icon_insert,
            description = R.string.btn_insert
        )
        IconCMenuButton(
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .weight(1F),
            enabled = (cursor.ints.size > 2),
            onClick = { dispatcher.remove_at_cursor() },
            icon = R.drawable.icon_remove,
            description = R.string.btn_remove
        )
        TextCMenuButton(
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .weight(1F),
            enabled = active_event != null,
            onClick = { dispatcher.set_duration() },
            onLongClick = { dispatcher.set_duration(1) },
            text = if (active_event == null) "" else "x${active_event.duration}"
        )
        IconCMenuButton(
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .weight(1F),
            enabled = active_line.assigned_offset.value != null || active_event != null,
            onClick = {
                if (active_line.assigned_offset.value != null) {
                    dispatcher.toggle_percussion()
                } else if (active_event != null) {
                    dispatcher.unset()
                }
            },
            onLongClick = { dispatcher.unset_root() },
            icon = if (active_line.assigned_offset.value != null) {
                if (active_event != null) R.drawable.icon_unset
                else R.drawable.icon_set_percussion
            } else {
                R.drawable.icon_unset
            },
            description = if (active_line.assigned_offset.value != null) {
                R.string.set_percussion_event
            } else {
                R.string.btn_unset
            }
        )
    }
}

@Composable
fun ContextMenuSinglePrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, show_relative_input: Boolean) {
    val active_event = ui_facade.active_event.value
    val cursor = ui_facade.active_cursor.value ?: return

    val octave = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note / ui_facade.radix.value
        is RelativeNoteEvent -> active_event.offset / ui_facade.radix.value
        is PercussionEvent -> 0
        null -> null
        else -> {
            ContextMenuStructureControls(ui_facade, dispatcher)
            return
        }
    }

    Column {
        ContextMenuStructureControls(ui_facade, dispatcher)
        Row {
            if (show_relative_input) {
                Column {
                    SText(R.string.absolute_label)
                    Text("+")
                    Text("-")
                }
            }
            Column {
            }
        }

        if (ui_facade.line_data[cursor.ints[0]].assigned_offset.value == null) {
            // Octave Selector
            Row {
                for (i in 0 until 8) {
                    NumberSelectorButton(
                        modifier = Modifier.weight(1F),
                        index = i,
                        selected = octave == i,
                        highlighted = ui_facade.highlighted_octave.value == i,
                        alternate = false,
                        callback = { dispatcher.set_octave(i) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContextMenuSingleSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    val line_data = ui_facade.line_data[cursor.ints[0]]
    if (line_data.assigned_offset.value != null) return
    if (line_data.ctl_type.value == null) {
        ContextMenuSingleStdSecondary(ui_facade, dispatcher, modifier)
    } else {
        ContextMenuSingleCtlSecondary(ui_facade, dispatcher, modifier)
    }
}
@Composable
fun ContextMenuSingleCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    val active_event = ui_facade.active_event.value ?: return
    Row(modifier = modifier) {
        when (active_event) {
            is OpusVolumeEvent -> VolumeEventMenu(ui_facade, dispatcher, active_event)
            is OpusTempoEvent -> TempoEventMenu(ui_facade, dispatcher, active_event)
            is OpusPanEvent -> PanEventMenu(ui_facade, dispatcher, active_event)
            is OpusReverbEvent -> ReverbEventMenu(ui_facade, dispatcher, active_event)
            is DelayEvent -> DelayEventMenu(ui_facade, dispatcher, active_event)
            is OpusVelocityEvent -> VelocityEventMenu(ui_facade, dispatcher, active_event)
            else -> {}
        }
    }
}

@Composable
fun ContextMenuSingleStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    val cursor = ui_facade.active_cursor.value ?: return
    if (ui_facade.line_data[cursor.ints[0]].assigned_offset.value != null) return

    val active_event = ui_facade.active_event.value
    val offset = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note % ui_facade.radix.value
        is RelativeNoteEvent -> active_event.offset % ui_facade.radix.value
        is PercussionEvent -> 0
        null -> null
        else -> throw Exception("Invalid Event Type") // TODO: Specify
    }

    // Offset Selector
    Row(modifier = modifier) {
        for (i in 0 until ui_facade.radix.value) {
            NumberSelectorButton(
                modifier = Modifier.weight(1F),
                index = i,
                selected = offset == i,
                highlighted = ui_facade.highlighted_offset.value == i,
                alternate = true,
                callback = { dispatcher.set_offset(i) }
            )
        }
    }
}
