package com.qfs.pagan.composable.cxtmenu

import android.database.Cursor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.RelativeInputMode
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.NumberSelectorButton
import com.qfs.pagan.composable.button.NumberSelectorColumn
import com.qfs.pagan.composable.button.NumberSelectorRow
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.abs
@Composable
fun SplitButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.split(2) },
        onLongClick = { dispatcher.split() },
        icon = R.drawable.icon_split,
        description = R.string.btn_split
    )
}
@Composable
fun InsertButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.insert_leaf(1) },
        onLongClick = { dispatcher.insert_leaf() },
        icon = R.drawable.icon_insert,
        description = R.string.btn_insert
    )
}

@Composable
fun RemoveButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, cursor: ViewModelEditorState.CacheCursor) {
    IconCMenuButton(
        modifier = modifier,
        enabled = (cursor.ints.size > 2),
        onClick = { dispatcher.remove_at_cursor() },
        icon = R.drawable.icon_remove,
        description = R.string.btn_remove
    )
}

@Composable
fun DurationButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, active_event: OpusEvent?) {
    TextCMenuButton(
        modifier = modifier,
        enabled = active_event != null,
        onClick = { dispatcher.set_duration() },
        onLongClick = { dispatcher.set_duration(1) },
        text = if (active_event == null) "" else "x${active_event.duration}"
    )
}

@Composable
fun UnsetButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, active_line: ViewModelEditorState.LineData, active_event: OpusEvent?) {
    IconCMenuButton(
        modifier = modifier,
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

@Composable
fun ContextMenuStructureControls(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, landscape: Boolean) {
    val active_event = ui_facade.active_event.value
    val cursor = ui_facade.active_cursor.value ?: return
    val active_line = ui_facade.line_data[cursor.ints[0]]

    if (landscape) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            SplitButton(Modifier, dispatcher)
            InsertButton(Modifier, dispatcher)
            RemoveButton(Modifier, dispatcher, cursor)
            DurationButton(Modifier, dispatcher, active_event)
            UnsetButton(Modifier, dispatcher, active_line, active_event)
        }
    } else {
        Row(
            modifier = Modifier
                .height(dimensionResource(R.dimen.icon_button_height))
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SplitButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            InsertButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher
            )
            RemoveButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher,
                cursor
            )
            DurationButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher,
                active_event
            )
            UnsetButton(
                Modifier
                    .fillMaxHeight()
                    .width(dimensionResource(R.dimen.contextmenu_button_width)),
                dispatcher,
                active_line,
                active_event
            )
        }
    }
}

@Composable
fun UserCopyModeSelect(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            modifier = Modifier.weight(1F),
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            onClick = { dispatcher.set_relative_mode(RelativeInputMode.Negative) },
            selected = ui_facade.relative_input_mode.value == RelativeInputMode.Negative,
            label = { Text("-") }
        )
        SegmentedButton(
            modifier = Modifier.weight(1F),
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            onClick = { dispatcher.set_relative_mode(RelativeInputMode.Absolute) },
            selected = ui_facade.relative_input_mode.value == RelativeInputMode.Absolute,
            label = { SText(R.string.absolute_label) }
        )
        SegmentedButton(
            modifier = Modifier.weight(1F),
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            onClick = { dispatcher.set_relative_mode(RelativeInputMode.Positive) },
            selected = ui_facade.relative_input_mode.value == RelativeInputMode.Positive,
            label = { Text("+") }
        )
    }
}

@Composable
fun ContextMenuSinglePrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, show_relative_input: Boolean, landscape: Boolean) {
    val active_event = ui_facade.active_event.value
    val cursor = ui_facade.active_cursor.value ?: return

    val octave = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note / ui_facade.radix.value
        is RelativeNoteEvent -> abs(active_event.offset) / ui_facade.radix.value
        is PercussionEvent -> 0
        null -> null
        else -> {
            ContextMenuStructureControls(ui_facade, dispatcher, landscape)
            return
        }
    }

    val active_line = ui_facade.line_data[cursor.ints[0]]
    if (active_line.assigned_offset.value != null) {
        ContextMenuStructureControls(ui_facade, dispatcher, landscape)
    } else if (landscape) {
        ContextMenuSinglePrimaryLandscape(ui_facade, dispatcher, show_relative_input, octave)
    } else {
        ContextMenuSinglePrimaryPortrait(ui_facade, dispatcher, show_relative_input, octave)
    }
}

@Composable
fun ContextMenuSinglePrimaryLandscape(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, show_relative_input: Boolean, octave: Int?) {
    Row {
        if (show_relative_input) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = { UserCopyModeSelect(ui_facade, dispatcher) }
            )
        }
        ContextMenuStructureControls(ui_facade, dispatcher, true)
        NumberSelectorColumn(8, octave, ui_facade.highlighted_octave.value, false) { dispatcher.set_octave(it) }
    }
}

@Composable
fun ContextMenuSinglePrimaryPortrait(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, show_relative_input: Boolean, octave: Int?) {
    Column {
        if (show_relative_input) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = { UserCopyModeSelect(ui_facade, dispatcher) }
            )
        }
        ContextMenuStructureControls(ui_facade, dispatcher, false)
        NumberSelectorRow(8, octave, ui_facade.highlighted_octave.value, false) { dispatcher.set_octave(it) }
    }
}

@Composable
fun ContextMenuSingleSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, landscape: Boolean = false) {
    val cursor = ui_facade.active_cursor.value ?: return
    val line_data = ui_facade.line_data[cursor.ints[0]]
    if (line_data.assigned_offset.value != null) return
    if (line_data.ctl_type.value == null) {
        ContextMenuSingleStdSecondary(ui_facade, dispatcher, modifier, landscape)
    } else {
        ContextMenuSingleCtlSecondary(ui_facade, dispatcher, modifier, landscape)
    }
}
@Composable
fun ContextMenuSingleCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, landscape: Boolean = false) {
    val active_event = ui_facade.active_event.value ?: return
    Row(modifier
        .fillMaxWidth()
        .padding(1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
fun ContextMenuSingleStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, landscape: Boolean = false) {
    val cursor = ui_facade.active_cursor.value ?: return
    if (ui_facade.line_data[cursor.ints[0]].assigned_offset.value != null) return

    val active_event = ui_facade.active_event.value
    val offset = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note % ui_facade.radix.value
        is RelativeNoteEvent -> abs(active_event.offset) % ui_facade.radix.value
        is PercussionEvent -> 0
        null -> null
        else -> throw Exception("Invalid Event Type") // TODO: Specify
    }

    // Offset Selector
    NumberSelectorRow(ui_facade.radix.value, offset, ui_facade.highlighted_offset.value, true) { dispatcher.set_offset(it) }
}
