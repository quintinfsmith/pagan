/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2026  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.RelativeInputMode
import com.qfs.pagan.TestTag
import com.qfs.pagan.Values
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.NumberSelector
import com.qfs.pagan.composable.RadioMenu
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.composable.effectwidget.DelayEventMenu
import com.qfs.pagan.composable.effectwidget.PanEventMenu
import com.qfs.pagan.composable.effectwidget.ReverbEventMenu
import com.qfs.pagan.composable.effectwidget.TempoEventMenu
import com.qfs.pagan.composable.effectwidget.VelocityEventMenu
import com.qfs.pagan.composable.effectwidget.VolumeEventMenu
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.Text
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
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun SplitButton(
    opus_manager: OpusLayerInterface,
    shape: Shape = Shapes.ContextMenuButtonPrimaryStart
) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LeafSplit),
        onClick = { opus_manager.split(2) },
        onLongClick = { opus_manager.split_tree_at_cursor(2) },
        icon = R.drawable.icon_split,
        shape = shape,
        description = R.string.btn_split
    )

    // TODO
    // this.dialog_number_input(R.string.dlg_split, 2, 32) {
    //     this.split(it)
    // }
}

@Composable
fun InsertButton(opus_manager: OpusLayerInterface) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LeafInsert),
        onClick = { opus_manager.insert_leaf(1) },
        onLongClick = { opus_manager.insert_at_cursor(1) },
        icon = R.drawable.icon_add,
        description = R.string.btn_insert
    )
    // TODO
    //this.dialog_number_input(R.string.dlg_insert, 1, 63) {
    //    this.insert_leaf(it)
    //}
}

@Composable
fun RemoveButton(opus_manager: OpusLayerInterface, cursor: ViewModelEditorState.CacheCursor) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.LeafRemove),
        enabled = (cursor.ints.size > 2),
        onClick = { opus_manager.remove_at_cursor() },
        onLongClick = { opus_manager.unset_root_at_cursor() },
        icon = R.drawable.icon_subtract,
        description = R.string.btn_remove
    )
}

@Composable
fun DurationButton(
    opus_manager: OpusLayerInterface,
    descriptor: ViewModelEditorState.EventDescriptor?,
    active_event: OpusEvent?,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    TextCMenuButton(
        modifier = Modifier
            .testTag(TestTag.EventDuration)
            .width(Dimensions.ButtonHeight.Normal),
        enabled = when (descriptor) {
            ViewModelEditorState.EventDescriptor.Selected,
            ViewModelEditorState.EventDescriptor.Tail -> true
            else -> false
        },
        shape = shape,
        contentPadding = Unpadded,
        onClick = { opus_manager.set_duration() },
        onLongClick = { opus_manager.set_duration_at_cursor(1) },
        text = when (descriptor) {
            ViewModelEditorState.EventDescriptor.Selected,
            ViewModelEditorState.EventDescriptor.Tail -> "x${active_event?.duration ?: 1}"
            else -> ""
        }
    )

    // TODO: Dialog
    //val event_duration = active_event?.duration ?: 1
    //this.dialog_number_input(R.string.dlg_duration, 1, default = event_duration) {
    //    this.set_duration(it)
    //}
}

@Composable
fun UnsetButton(
    opus_manager: OpusLayerInterface,
    active_line: ViewModelEditorState.LineData,
    active_event: OpusEvent?,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.EventUnset),
        enabled = active_event != null,
        onClick = { opus_manager.unset() },
        onLongClick = { opus_manager.unset_root_at_cursor() },
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
fun ContextMenuStructureControls(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, landscape: Boolean) {
    val active_event = vm_state.active_event.value
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]

    if (landscape) {
        Column(
            Modifier.width(Dimensions.ContextMenuButtonWidth),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SplitButton(opus_manager)
            MediumSpacer()
            InsertButton(opus_manager)
            MediumSpacer()
            RemoveButton(opus_manager, cursor)
            MediumSpacer()
            Spacer(Modifier.weight(1F))
            key(active_event?.duration) {
                DurationButton(
                    opus_manager,
                    vm_state.active_event_descriptor.value,
                    active_event,
                    shape = if (active_line.assigned_offset.value != null) {
                        Shapes.ContextMenuButtonPrimaryBottom
                    } else {
                        Shapes.ContextMenuButtonPrimary
                    }
                )
            }
            if (active_line.assigned_offset.value == null) {
                MediumSpacer()
                UnsetButton(opus_manager, active_line, active_event, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
    } else {
        ContextMenuPrimaryRow(modifier) {
            SplitButton(opus_manager)
            MediumSpacer()
            InsertButton(opus_manager)
            MediumSpacer()
            RemoveButton(opus_manager, cursor)
            MediumSpacer()

            key(active_event?.duration) {
                DurationButton(
                    opus_manager,
                    vm_state.active_event_descriptor.value,
                    active_event,
                    shape = if (active_line.assigned_offset.value != null) {
                        Shapes.ContextMenuButtonPrimaryEnd
                    } else {
                        Shapes.ContextMenuButtonPrimary
                    }
                )
            }
            if (active_line.assigned_offset.value == null) {
                MediumSpacer()
                UnsetButton(opus_manager, active_line, active_event, Shapes.ContextMenuButtonPrimaryEnd)
            }
        }
    }
}


@Composable
fun ContextMenuLeafPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val active_event = vm_state.active_event.value
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]
    val is_percussion = active_line.assigned_offset.value != null

    val octave = when (active_event) {
        is AbsoluteNoteEvent -> active_event.note / vm_state.radix.value
        is RelativeNoteEvent -> abs(active_event.offset) / vm_state.radix.value
        is PercussionEvent -> 0
        else -> null
    }

    when (layout) {
        LayoutSize.SmallLandscape -> {
            if (active_line.ctl_type.value != null || is_percussion) {
                ContextMenuStructureControls(
                    modifier,
                    vm_state,
                    opus_manager,
                    landscape = true
                )
            } else {
                Row {
                    ContextMenuStructureControls(Modifier, vm_state, opus_manager, true)
                    MediumSpacer()
                    Column(Modifier.width(Dimensions.NumberSelectorColumnWidth)) {
                        val octave_dropdown_visible: MutableState<Int?> = remember { mutableStateOf(null) }
                        NumberSelector(
                            progression = 7 downTo 0,
                            selected = when (vm_state.active_event_descriptor.value) {
                                ViewModelEditorState.EventDescriptor.Tail,
                                ViewModelEditorState.EventDescriptor.Selected -> octave
                                else -> null
                            },
                            highlighted = if (vm_state.latest_input_indicator.value && vm_state.relative_input_mode.value == RelativeInputMode.Absolute) {
                                vm_state.highlighted_octave.value
                            } else {
                                null
                            },
                            default = when (vm_state.active_event_descriptor.value) {
                                ViewModelEditorState.EventDescriptor.Backup -> octave
                                else -> null
                            },
                            alternate = false,
                            on_click = { opus_manager.set_note_octave_at_cursor(it, vm_state.relative_input_mode.value) },
                            on_long_click = { octave_dropdown_visible.value = it }
                        )
                        RelativeInputDropDown(vm_state, opus_manager, octave_dropdown_visible) { new_octave: Int, mode: RelativeInputMode ->
                            opus_manager.set_note_octave_at_cursor(new_octave, mode)
                        }
                    }
                }
            }
        }

        LayoutSize.MediumLandscape -> {
            ContextMenuStructureControls(modifier, vm_state, opus_manager, true)
        }

        LayoutSize.SmallPortrait,
        LayoutSize.MediumPortrait,
        LayoutSize.LargeLandscape,
        LayoutSize.LargePortrait,
        LayoutSize.XLargeLandscape,
        LayoutSize.XLargePortrait -> {
            if (is_percussion) {
                ContextMenuStructureControls(modifier, vm_state, opus_manager, landscape = false)
            } else {
                Column(modifier) {
                    ContextMenuStructureControls(Modifier, vm_state, opus_manager, false)
                }
            }
        }
    }
}

@Composable
fun ContextMenuLeafSecondary(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, layout: LayoutSize) {
}
@Composable
fun ContextMenuLeafCtlSecondary(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, layout: LayoutSize) {
    ContextMenuSecondaryRow(modifier) {
        when (val active_event = vm_state.active_event.value) {
            is OpusVolumeEvent -> VolumeEventMenu(vm_state, opus_manager, active_event)
            is OpusTempoEvent -> TempoEventMenu(vm_state, opus_manager, active_event)
            is OpusPanEvent -> PanEventMenu(vm_state, opus_manager, active_event)
            is OpusReverbEvent -> ReverbEventMenu(vm_state, opus_manager, active_event)
            is DelayEvent -> DelayEventMenu(vm_state, opus_manager, active_event)
            is OpusVelocityEvent -> VelocityEventMenu(vm_state, opus_manager, active_event)
            else -> {}
        }
    }
}

@Composable
fun RelativeInputDropDown(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, expanded: MutableState<Int?>, callback: (Int, RelativeInputMode) -> Unit) {
    DropdownMenu(
        expanded = expanded.value != null,
        onDismissRequest = { expanded.value = null}
    ) {
        RadioMenu(
            modifier = Modifier.padding(Dimensions.RelativeInputPopup.Padding),
            options = listOf(
                Pair(RelativeInputMode.Negative) {
                    Icon(
                        modifier = Modifier
                            .padding(vertical = Dimensions.RelativeInputPopup.ItemPadding)
                            .height(Dimensions.RelativeInputPopup.ItemHeight),
                        painter = painterResource(R.drawable.icon_subtract),
                        contentDescription = stringResource(R.string.relative_input_mode_positive)
                    )
                },
                Pair(RelativeInputMode.Absolute) {
                    Text(
                        R.string.absolute_label,
                        modifier = Modifier
                            .padding(vertical = Dimensions.RelativeInputPopup.ItemPadding)
                            .height(Dimensions.RelativeInputPopup.ItemHeight),
                        style = Typography.RadioMenu
                    )
                },
                Pair(RelativeInputMode.Positive) {
                    Icon(
                        modifier = Modifier
                            .padding(vertical = Dimensions.RelativeInputPopup.ItemPadding)
                            .height(Dimensions.RelativeInputPopup.ItemHeight),
                        painter = painterResource(R.drawable.icon_add),
                        contentDescription = stringResource(R.string.relative_input_mode_positive)
                    )
                }
            ),
            active = remember { mutableStateOf(vm_state.relative_input_mode.value) },
            callback = {
                vm_state.relative_input_mode.value = it
                callback(expanded.value!!, it)
                expanded.value = null
            }
        )
    }
}

@Composable
fun ContextMenuLeafStdSecondary(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, modifier: Modifier = Modifier, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val active_line = vm_state.line_data[cursor.ints[0]]
    val active_event = vm_state.active_event.value

    if (active_line.assigned_offset.value != null) {
        val checked = remember { mutableStateOf(vm_state.active_event_descriptor.value == ViewModelEditorState.EventDescriptor.Selected && vm_state.active_event.value != null) }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Switch(
                checked.value,
                modifier = Modifier.testTag(TestTag.PercussionToggle),
                thumbContent = {
                    Icon(
                        modifier = Modifier.padding(Dimensions.PercussionSwitchIconPadding),
                        painter = painterResource(R.drawable.percussion_indicator),
                        contentDescription = null
                    )
                },
                onCheckedChange = {
                    checked.value = it
                    if (!it) {
                        opus_manager.unset()
                    } else {
                        opus_manager.set_percussion_event_at_cursor()
                    }
                }
            )
        }
    } else {
        val octave_dropdown_visible: MutableState<Int?> = remember { mutableStateOf(null) }
        RelativeInputDropDown(vm_state, opus_manager, octave_dropdown_visible) { i: Int, mode: RelativeInputMode ->
            opus_manager.set_note_octave_at_cursor(i, mode)
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
                    is AbsoluteNoteEvent -> active_event.note / vm_state.radix.value
                    is RelativeNoteEvent -> abs(active_event.offset) / vm_state.radix.value
                    is PercussionEvent -> 0
                    null -> null
                    else -> throw Exception("Invalid Event Type $active_event") // TODO: Specify
                }

                Row {
                    NumberSelector(
                        progression = 0 until Values.OctaveCount,
                        selected = when (vm_state.active_event_descriptor.value) {
                            ViewModelEditorState.EventDescriptor.Selected,
                            ViewModelEditorState.EventDescriptor.Tail -> octave
                            else -> null
                        },
                        highlighted = if (vm_state.latest_input_indicator.value && vm_state.relative_input_mode.value == RelativeInputMode.Absolute) {
                            vm_state.highlighted_octave.value
                        } else {
                            null
                        },
                        default = when (vm_state.active_event_descriptor.value) {
                            ViewModelEditorState.EventDescriptor.Backup -> octave
                            else -> null
                        },
                        alternate = false,
                        on_click = { opus_manager.set_note_octave_at_cursor(it, vm_state.relative_input_mode.value) },
                        on_long_click = { octave_dropdown_visible.value = it }
                    )
                }
                Spacer(Modifier.height(Dimensions.NumberSelectorSpacing))
            }

            LayoutSize.SmallLandscape -> {}
        }

        val offset = when (active_event) {
            is AbsoluteNoteEvent -> active_event.note % vm_state.radix.value
            is RelativeNoteEvent -> abs(active_event.offset) % vm_state.radix.value
            is PercussionEvent -> 0
            null -> null
            else -> throw Exception("Invalid Event Type") // TODO: Specify
        }

        val offset_dropdown_visible: MutableState<Int?> = remember { mutableStateOf(null) }
        RelativeInputDropDown(vm_state, opus_manager, offset_dropdown_visible) { i: Int, mode: RelativeInputMode ->
            opus_manager.set_note_offset_at_cursor(i, mode)
        }
        Column {
            var count = ceil(vm_state.radix.value.toFloat() / Values.OffsetModulo).toInt()
            for (i in count - 1 downTo 0) {
                Row(modifier) {
                    NumberSelector(
                        progression = i until vm_state.radix.value step count,
                        selected = when (vm_state.active_event_descriptor.value) {
                            ViewModelEditorState.EventDescriptor.Selected,
                            ViewModelEditorState.EventDescriptor.Tail -> offset
                            else -> null
                        },
                        highlighted = if (vm_state.latest_input_indicator.value && vm_state.relative_input_mode.value == RelativeInputMode.Absolute) {
                            vm_state.highlighted_offset.value
                        } else {
                            null
                        },
                        default = when (vm_state.active_event_descriptor.value) {
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
                        on_long_click = { offset_dropdown_visible.value = it },
                        on_click = { opus_manager.set_note_offset_at_cursor(it, vm_state.relative_input_mode.value) }
                    )
                }
                if (i != 0) {
                    Spacer(Modifier.height(Dimensions.NumberSelectorSpacing))
                }
            }
        }
    }
}
