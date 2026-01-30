package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.ui.theme.Typography
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
    active_event: OpusEvent?,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    TextCMenuButton(
        modifier = Modifier.width(Dimensions.ButtonHeight.Normal),
        enabled = when (descriptor) {
            ViewModelEditorState.EventDescriptor.Selected,
            ViewModelEditorState.EventDescriptor.Tail -> true
            else -> false
        },
        shape = shape,
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
                    ui_facade.active_event_descriptor.value,
                    active_event,
                    shape = if (active_line.assigned_offset.value != null) {
                        Shapes.ContextMenuButtonPrimaryBottom
                    } else {
                        Shapes.ContextMenuButtonPrimary
                    }
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

            key(active_event?.duration) {
                DurationButton(
                    dispatcher,
                    ui_facade.active_event_descriptor.value,
                    active_event,
                    shape = if (active_line.assigned_offset.value != null) {
                        Shapes.ContextMenuButtonPrimaryEnd
                    } else {
                        Shapes.ContextMenuButtonPrimary
                    }
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
fun ContextMenuLeafPrimary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize) {
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
        LayoutSize.SmallLandscape -> {
            if (is_percussion) {
                ContextMenuStructureControls(
                    modifier,
                    ui_facade,
                    dispatcher,
                    landscape = true
                )
            } else {
                Row {
                    ContextMenuStructureControls(Modifier, ui_facade, dispatcher, true)
                    Column(Modifier.width(dimensionResource(R.dimen.numberselector_column_width))) {
                        val octave_dropdown_visible: MutableState<Int?> = remember { mutableStateOf(null) }
                        NumberSelector(
                            progression = 7 downTo 0,
                            selected = when (ui_facade.active_event_descriptor.value) {
                                ViewModelEditorState.EventDescriptor.Tail,
                                ViewModelEditorState.EventDescriptor.Selected -> octave
                                else -> null
                            },
                            highlighted = if (ui_facade.latest_input_indicator.value && ui_facade.relative_input_mode.value == RelativeInputMode.Absolute) {
                                ui_facade.highlighted_octave.value
                            } else {
                                null
                            },
                            default = when (ui_facade.active_event_descriptor.value) {
                                ViewModelEditorState.EventDescriptor.Backup -> octave
                                else -> null
                            },
                            alternate = false,
                            on_click = { dispatcher.set_octave(it, ui_facade.relative_input_mode.value) },
                            on_long_click = { octave_dropdown_visible.value = it }
                        )
                        RelativeInputDropDown(ui_facade, dispatcher, octave_dropdown_visible) { i: Int, mode: RelativeInputMode ->
                            dispatcher.set_octave(i, mode)
                        }
                    }
                }
            }
        }

        LayoutSize.MediumLandscape -> {
            ContextMenuStructureControls(modifier, ui_facade, dispatcher, true)
        }

        LayoutSize.SmallPortrait,
        LayoutSize.MediumPortrait,
        LayoutSize.LargeLandscape,
        LayoutSize.LargePortrait,
        LayoutSize.XLargeLandscape,
        LayoutSize.XLargePortrait -> {
            if (is_percussion) {
                ContextMenuStructureControls(modifier, ui_facade, dispatcher, landscape = false)
            } else {
                Column(modifier) {
                    ContextMenuStructureControls(Modifier, ui_facade, dispatcher, false)
                }
            }
        }
    }
}

@Composable
fun ContextMenuLeafSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, layout: LayoutSize) {
}
@Composable
fun ContextMenuLeafCtlSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, layout: LayoutSize) {
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
fun RelativeInputDropDown(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, expanded: MutableState<Int?>, callback: (Int, RelativeInputMode) -> Unit) {
    DropdownMenu(
        expanded = expanded.value != null,
        onDismissRequest = { expanded.value = null}
    ) {
        RadioMenu(
            modifier = Modifier.padding(8.dp),
            options = listOf(
                Pair(RelativeInputMode.Negative) {
                    Icon(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .height(20.dp),
                        painter = painterResource(R.drawable.icon_subtract),
                        contentDescription = stringResource(R.string.relative_input_mode_positive)
                    )
                },
                Pair(RelativeInputMode.Absolute) {
                    SText(
                        R.string.absolute_label,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .height(20.dp),
                        style = Typography.RadioMenu
                    )
                },
                Pair(RelativeInputMode.Positive) {
                    Icon(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .height(20.dp),
                        painter = painterResource(R.drawable.icon_add),
                        contentDescription = stringResource(R.string.relative_input_mode_positive)
                    )
                }
            ),
            active = remember { mutableStateOf(ui_facade.relative_input_mode.value) },
            callback = {
                ui_facade.relative_input_mode.value = it
                callback(expanded.value!!, it)
                expanded.value = null
            }
        )
    }
}

@Composable
fun ContextMenuLeafStdSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier, layout: LayoutSize) {
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
        val octave_dropdown_visible: MutableState<Int?> = remember { mutableStateOf(null) }
        RelativeInputDropDown(ui_facade, dispatcher, octave_dropdown_visible) { i: Int, mode: RelativeInputMode ->
            dispatcher.set_octave(i, mode)
        }
        when (layout) {
            LayoutSize.SmallPortrait,
            LayoutSize.MediumLandscape,
            LayoutSize.MediumPortrait,
            LayoutSize.LargeLandscape,
            LayoutSize.LargePortrait,
            LayoutSize.XLargeLandscape,
            LayoutSize.XLargePortrait -> {
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
                        selected = when (ui_facade.active_event_descriptor.value) {
                            ViewModelEditorState.EventDescriptor.Selected,
                            ViewModelEditorState.EventDescriptor.Tail -> octave
                            else -> null
                        },
                        highlighted = if (ui_facade.latest_input_indicator.value && ui_facade.relative_input_mode.value == RelativeInputMode.Absolute) {
                            ui_facade.highlighted_octave.value
                        } else {
                            null
                        },
                        default = when (ui_facade.active_event_descriptor.value) {
                            ViewModelEditorState.EventDescriptor.Backup -> octave
                            else -> null
                        },
                        alternate = false,
                        on_click = { dispatcher.set_octave(it, ui_facade.relative_input_mode.value) },
                        on_long_click = { octave_dropdown_visible.value = it }
                    )
                }
                Spacer(Modifier.height(Dimensions.NumberSelectorSpacing))
            }

            LayoutSize.SmallLandscape -> {}
        }

        val offset = when (active_event) {
            is AbsoluteNoteEvent -> active_event.note % ui_facade.radix.value
            is RelativeNoteEvent -> abs(active_event.offset) % ui_facade.radix.value
            is PercussionEvent -> 0
            null -> null
            else -> throw Exception("Invalid Event Type") // TODO: Specify
        }

        val offset_dropdown_visible: MutableState<Int?> = remember { mutableStateOf(null) }
        RelativeInputDropDown(ui_facade, dispatcher, offset_dropdown_visible) { i: Int, mode: RelativeInputMode ->
            dispatcher.set_offset(i, mode)
        }
        Column {
            var count = ceil(ui_facade.radix.value.toFloat() / 12F).toInt()
            for (i in count - 1 downTo 0) {
                Row(modifier) {
                    NumberSelector(
                        progression = i until ui_facade.radix.value step count,
                        selected = when (ui_facade.active_event_descriptor.value) {
                            ViewModelEditorState.EventDescriptor.Selected,
                            ViewModelEditorState.EventDescriptor.Tail -> offset
                            else -> null
                        },
                        highlighted = if (ui_facade.latest_input_indicator.value && ui_facade.relative_input_mode.value == RelativeInputMode.Absolute) {
                            ui_facade.highlighted_offset.value
                        } else {
                            null
                        },
                        default = when (ui_facade.active_event_descriptor.value) {
                            ViewModelEditorState.EventDescriptor.Backup -> offset
                            else -> null
                        },
                        alternate = true,
                        shape_start = if (layout == LayoutSize.SmallLandscape) {
                            Shapes.NumberSelectorButtonStart
                        } else {
                            Shapes.NumberSelectorButton
                        },
                        shape_end = if (layout == LayoutSize.SmallLandscape) {
                            Shapes.NumberSelectorButtonEnd
                        } else {
                            Shapes.NumberSelectorButton
                        },
                        on_long_click = { offset_dropdown_visible.value = i },
                        on_click = { dispatcher.set_offset(it, ui_facade.relative_input_mode.value) }
                    )
                }
                if (i != 0) {
                    Spacer(Modifier.height(Dimensions.NumberSelectorSpacing))
                }
            }
        }
    }
}
