package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.RelativeInputMode
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.NumberSelector
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
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlin.math.abs

@Composable
fun SplitButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.split(2) },
        onLongClick = { dispatcher.split() },
        icon = R.drawable.icon_split,
        description = R.string.btn_split
    )
}
@Composable
fun InsertButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.insert_leaf(1) },
        onLongClick = { dispatcher.insert_leaf() },
        icon = R.drawable.icon_insert,
        description = R.string.btn_insert
    )
}

@Composable
fun RemoveButton(dispatcher: ActionTracker, cursor: ViewModelEditorState.CacheCursor) {
    IconCMenuButton(
        enabled = (cursor.ints.size > 2),
        onClick = { dispatcher.remove_at_cursor() },
        icon = R.drawable.icon_remove,
        description = R.string.btn_remove
    )
}

@Composable
fun DurationButton(dispatcher: ActionTracker, active_event: OpusEvent?) {
    TextCMenuButton(
        modifier = Modifier.width(dimensionResource(R.dimen.contextmenu_button_width)),
        enabled = active_event != null,
        onClick = { dispatcher.set_duration() },
        onLongClick = { dispatcher.set_duration(1) },
        text = if (active_event == null) "" else "x${active_event.duration}"
    )
}

@Composable
fun UnsetButton(dispatcher: ActionTracker, active_line: ViewModelEditorState.LineData, active_event: OpusEvent?) {
    IconCMenuButton(
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
fun ContextMenuStructureControls(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, landscape: Boolean) {
    val active_event = ui_facade.active_event.value
    val cursor = ui_facade.active_cursor.value ?: return
    val active_line = ui_facade.line_data[cursor.ints[0]]

    if (landscape) {
        Column(
            Modifier.width(dimensionResource(R.dimen.contextmenu_button_width)),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SplitButton(dispatcher)
            CMPadding()
            InsertButton(dispatcher)
            CMPadding()
            RemoveButton(dispatcher, cursor)
            CMPadding()
            Spacer(Modifier.weight(1F))
            key(active_event?.duration) {
                DurationButton(
                    dispatcher,
                    if (ui_facade.active_event_descriptor.value == ViewModelEditorState.EventDescriptor.Selected) {
                        active_event
                    } else {
                        null
                    }
                )
            }
            CMPadding()
            UnsetButton(dispatcher, active_line, active_event)
        }
    } else {
        ContextMenuPrimaryRow(modifier) {
            SplitButton(dispatcher)
            CMPadding()
            InsertButton(dispatcher)
            CMPadding()
            RemoveButton(dispatcher, cursor)
            CMPadding()

            key(active_event?.duration) {
                DurationButton(
                    dispatcher,
                    if (ui_facade.active_event_descriptor.value == ViewModelEditorState.EventDescriptor.Selected) {
                        active_event
                    } else {
                        null
                    }
                )
            }
            CMPadding()
            UnsetButton(dispatcher, active_line, active_event)
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
fun ContextMenuLeafPrimary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, show_relative_input: Boolean, layout: ViewModelPagan.LayoutSize) {
    val active_event = ui_facade.active_event.value
    val cursor = ui_facade.active_cursor.value ?: return
    val active_line = ui_facade.line_data[cursor.ints[0]]
    val is_percussion = active_line.assigned_offset.value != null

    val octave = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note / ui_facade.radix.value
        is RelativeNoteEvent -> abs(active_event.offset) / ui_facade.radix.value
        is PercussionEvent -> 0
        else -> null
    }

    when (layout) {
        ViewModelPagan.LayoutSize.SmallLandscape -> {
            if (is_percussion) {
                ContextMenuStructureControls(modifier, ui_facade, dispatcher, true)
            } else {
                Row {
                    if (show_relative_input) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            content = { UserCopyModeSelect(ui_facade, dispatcher) }
                        )
                    }
                    ContextMenuStructureControls(Modifier, ui_facade, dispatcher, true)
                    Column(Modifier.width(dimensionResource(R.dimen.numberselector_column_width))) {
                        NumberSelector(8, octave, ui_facade.highlighted_octave.value, false) { dispatcher.set_octave(it) }
                    }
                }
            }
        }

        ViewModelPagan.LayoutSize.MediumLandscape -> {
            ContextMenuStructureControls(modifier, ui_facade, dispatcher, true)
        }

        ViewModelPagan.LayoutSize.SmallPortrait,
        ViewModelPagan.LayoutSize.MediumPortrait,
        ViewModelPagan.LayoutSize.LargeLandscape,
        ViewModelPagan.LayoutSize.LargePortrait,
        ViewModelPagan.LayoutSize.XLargeLandscape,
        ViewModelPagan.LayoutSize.XLargePortrait -> {
            if (is_percussion) {
                ContextMenuStructureControls(modifier, ui_facade, dispatcher, false)
            } else {
                Column(modifier) {
                    if (show_relative_input) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            content = { UserCopyModeSelect(ui_facade, dispatcher) }
                        )
                    }
                    ContextMenuStructureControls(Modifier, ui_facade, dispatcher, false)
                }
            }
        }
    }
}

@Composable
fun ContextMenuLeafSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, layout: ViewModelPagan.LayoutSize) {
}
@Composable
fun ContextMenuLeafCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, layout: ViewModelPagan.LayoutSize) {
    val active_event = ui_facade.active_event.value ?: return
    ContextMenuSecondaryRow(modifier) {
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
fun ContextMenuLeafStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, layout: ViewModelPagan.LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    if (ui_facade.line_data[cursor.ints[0]].assigned_offset.value != null) return
    val active_event = ui_facade.active_event.value

    when (layout) {
        ViewModelPagan.LayoutSize.SmallPortrait,
        ViewModelPagan.LayoutSize.MediumLandscape,
        ViewModelPagan.LayoutSize.MediumPortrait,
        ViewModelPagan.LayoutSize.LargeLandscape,
        ViewModelPagan.LayoutSize.LargePortrait,
        ViewModelPagan.LayoutSize.XLargeLandscape,
        ViewModelPagan.LayoutSize.XLargePortrait -> {
            val octave = when (active_event) {
                is AbsoluteNoteEvent -> active_event.note / ui_facade.radix.value
                is RelativeNoteEvent -> abs(active_event.offset) / ui_facade.radix.value
                is PercussionEvent -> 0
                null -> null
                else -> throw Exception("Invalid Event Type $active_event") // TODO: Specify
            }

            Row(Modifier.padding(top = dimensionResource(R.dimen.contextmenu_padding))) {
                NumberSelector(8, octave, ui_facade.highlighted_octave.value, false) { dispatcher.set_octave(it) }
            }
        }
        ViewModelPagan.LayoutSize.SmallLandscape -> {}
    }

    val offset = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note % ui_facade.radix.value
        is RelativeNoteEvent -> abs(active_event.offset) % ui_facade.radix.value
        is PercussionEvent -> 0
        null -> null
        else -> throw Exception("Invalid Event Type") // TODO: Specify
    }

    Row(modifier) {
        NumberSelector(ui_facade.radix.value, offset, ui_facade.highlighted_offset.value, true) { dispatcher.set_offset(it) }
    }
}
