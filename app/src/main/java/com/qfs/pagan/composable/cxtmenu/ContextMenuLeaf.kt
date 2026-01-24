package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.RelativeInputMode
import com.qfs.pagan.composable.DropdownMenu
import com.qfs.pagan.composable.RadioMenu
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
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState
import com.qfs.pagan.viewmodel.ViewModelPagan
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun SplitButton(
    dispatcher: ActionTracker,
    shape: Shape = Shapes.ContextMenuButtonPrimaryStart
) {
    IconCMenuButton(
        onClick = { dispatcher.split(2) },
        onLongClick = { dispatcher.split() },
        icon = R.drawable.icon_split,
        shape = shape,
        description = R.string.btn_split
    )
}
@Composable
fun InsertButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.insert_leaf(1) },
        onLongClick = { dispatcher.insert_leaf() },
        icon = R.drawable.icon_add,
        description = R.string.btn_insert
    )
}

@Composable
fun RemoveButton(dispatcher: ActionTracker, cursor: ViewModelEditorState.CacheCursor) {
    IconCMenuButton(
        enabled = (cursor.ints.size > 2),
        onClick = { dispatcher.remove_at_cursor() },
        icon = R.drawable.icon_subtract,
        description = R.string.btn_remove
    )
}

@Composable
fun DurationButton(
    dispatcher: ActionTracker,
    descriptor: ViewModelEditorState.EventDescriptor?,
    active_event: OpusEvent?
) {
    TextCMenuButton(
        modifier = Modifier.width(Dimensions.ButtonHeight.Normal),
        enabled = when (descriptor) {
            ViewModelEditorState.EventDescriptor.Selected,
            ViewModelEditorState.EventDescriptor.Tail -> true
            else -> false
        },
        contentPadding = PaddingValues(0.dp),
        onClick = { dispatcher.set_duration() },
        onLongClick = { dispatcher.set_duration(1) },
        text = when (descriptor) {
            ViewModelEditorState.EventDescriptor.Selected,
            ViewModelEditorState.EventDescriptor.Tail -> "x${active_event?.duration ?: 1}"
            else -> ""
        }
    )
}

@Composable
fun UnsetButton(
    dispatcher: ActionTracker,
    active_line: ViewModelEditorState.LineData,
    active_event: OpusEvent?,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        enabled = active_event != null,
        onClick = { dispatcher.unset() },
        onLongClick = { dispatcher.unset_root() },
        icon = R.drawable.icon_erase,
        shape = shape,
        description = if (active_line.assigned_offset.value != null) {
            R.string.set_percussion_event
        } else {
            R.string.btn_unset
        }
    )
}

@Composable
fun RelativeModeButton(dispatcher: ActionTracker, ui_facade: ViewModelEditorState) {
    val expanded = remember { mutableStateOf(false) }
    Box {
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false}
        ) {
            RadioMenu(
                options = listOf(
                    Pair(RelativeInputMode.Negative) { Text("-") },
                    Pair(RelativeInputMode.Absolute) { SText(R.string.absolute_label) },
                    Pair(RelativeInputMode.Positive) { Text("+") }
                ),
                active = remember { mutableStateOf(ui_facade.relative_input_mode.value) },
                callback = {
                    dispatcher.set_relative_mode(it)
                    expanded.value = false
                }
            )
        }
        TextCMenuButton(
            modifier = Modifier.width(Dimensions.ButtonHeight.Normal),
            text = when (ui_facade.relative_input_mode.value) {
                RelativeInputMode.Absolute -> "|n|"
                RelativeInputMode.Positive -> "+"
                RelativeInputMode.Negative -> "-"
            },
            shape = Shapes.ContextMenuButtonPrimary,
            onClick = { expanded.value = !expanded.value }
        )
    }
}

@Composable
fun ContextMenuStructureControls(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, show_relative_input: Boolean, landscape: Boolean) {
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
            if (show_relative_input) {
                RelativeModeButton(dispatcher, ui_facade)
                CMPadding()
            } else {
                Spacer(Modifier.weight(1F))
            }
            key(active_event?.duration) {
                DurationButton(
                    dispatcher,
                    ui_facade.active_event_descriptor.value,
                    active_event,
                )
            }
            if (active_line.assigned_offset.value == null) {
                CMPadding()
                UnsetButton(dispatcher, active_line, active_event, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
    } else {
        ContextMenuPrimaryRow(modifier) {
            SplitButton(dispatcher)
            CMPadding()
            InsertButton(dispatcher)
            CMPadding()
            RemoveButton(dispatcher, cursor)
            CMPadding()

            if (show_relative_input) {
                RelativeModeButton(dispatcher, ui_facade)
                CMPadding()
            }

            key(active_event?.duration) {
                DurationButton(
                    dispatcher,
                    ui_facade.active_event_descriptor.value,
                    active_event,
                )
            }
            if (active_line.assigned_offset.value == null) {
                CMPadding()
                UnsetButton(dispatcher, active_line, active_event, Shapes.ContextMenuButtonPrimaryEnd)
            }
        }
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
                ContextMenuStructureControls(
                    modifier,
                    ui_facade,
                    dispatcher,
                    show_relative_input = false,
                    landscape = true
                )
            } else {
                Row {
                    ContextMenuStructureControls(Modifier, ui_facade, dispatcher, show_relative_input, true)
                    Column(Modifier.width(dimensionResource(R.dimen.numberselector_column_width))) {
                        NumberSelector(
                            progression = 7 downTo 0,
                            selected = octave,
                            highlighted = if (ui_facade.relative_input_mode.value == RelativeInputMode.Absolute) {
                                ui_facade.highlighted_octave.value
                            } else {
                                null
                            },
                            alternate = false,
                            callback = { dispatcher.set_octave(it) }
                        )
                    }
                }
            }
        }

        ViewModelPagan.LayoutSize.MediumLandscape -> {
            ContextMenuStructureControls(modifier, ui_facade, dispatcher, show_relative_input && ! is_percussion, true)
        }

        ViewModelPagan.LayoutSize.SmallPortrait,
        ViewModelPagan.LayoutSize.MediumPortrait,
        ViewModelPagan.LayoutSize.LargeLandscape,
        ViewModelPagan.LayoutSize.LargePortrait,
        ViewModelPagan.LayoutSize.XLargeLandscape,
        ViewModelPagan.LayoutSize.XLargePortrait -> {
            if (is_percussion) {
                ContextMenuStructureControls(modifier, ui_facade, dispatcher,
                    show_relative_input = false,
                    landscape = false
                )
            } else {
                Column(modifier) {
                    ContextMenuStructureControls(Modifier, ui_facade, dispatcher, show_relative_input, false)
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
    val active_event = ui_facade.active_event.value?.copy() ?: return
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
    val active_line = ui_facade.line_data[cursor.ints[0]]
    val active_event = ui_facade.active_event.value

    if (active_line.assigned_offset.value != null) {
        val checked = remember { mutableStateOf(active_event != null) }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Switch(
                checked.value,
                thumbContent = {
                    Icon(
                        modifier = Modifier.padding(4.dp),
                        painter = painterResource(R.drawable.percussion_indicator),
                        contentDescription = null
                    )
                },
                onCheckedChange = {
                    checked.value = it
                    if (!it) {
                        dispatcher.unset()
                    } else {
                        dispatcher.toggle_percussion()
                    }
                }
            )
        }
    } else {
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

                Row {
                    NumberSelector(
                        progression = 0 until 8,
                        selected = octave,
                        highlighted = if (ui_facade.relative_input_mode.value == RelativeInputMode.Absolute) {
                            ui_facade.highlighted_octave.value
                        } else {
                            null
                        },
                        alternate = false,
                        callback = { dispatcher.set_octave(it) }
                    )
                }
                Spacer(Modifier.height(Dimensions.NumberSelectorSpacing))
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

        Column {
            var count = ceil(ui_facade.radix.value.toFloat() / 12F).toInt()
            for (i in count - 1 downTo 0) {
                Row(modifier) {
                    NumberSelector(
                        progression = i until ui_facade.radix.value step count,
                        selected = offset,
                        highlighted = if (ui_facade.relative_input_mode.value == RelativeInputMode.Absolute) {
                            ui_facade.highlighted_offset.value
                        } else {
                            null
                        },
                        alternate = true,
                        shape_start = if (layout == ViewModelPagan.LayoutSize.SmallLandscape) {
                            Shapes.NumberSelectorButtonStart
                        } else {
                            Shapes.NumberSelectorButton
                        },
                        shape_end = if (layout == ViewModelPagan.LayoutSize.SmallLandscape) {
                            Shapes.NumberSelectorButtonEnd
                        } else {
                            Shapes.NumberSelectorButton
                        },
                        callback = { dispatcher.set_offset(it) }
                    )
                }
            }
        }
    }
}
