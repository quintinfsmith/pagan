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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.TextInput
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState


@Composable
fun TagButton(
    opus_manager: OpusLayerInterface,
    column_data: ViewModelEditorState.ColumnData,
    beat: Int,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.BeatToggleTag),
        onClick = {
            if (column_data.is_tagged.value) {
                opus_manager.remove_tagged_section(beat)
            } else {
                opus_manager.tag_section(beat, null)
            }
        },
        shape = shape,
        icon = if (column_data.is_tagged.value) R.drawable.icon_untag
        else R.drawable.icon_tag,
        description = if (column_data.is_tagged.value) R.string.cd_remove_section_mark
        else R.string.cd_mark_section
    )
}

@Composable
fun AdjustBeatButton(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface) {
    val dialog_visibility = remember { mutableStateOf(false) }
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.AdjustSelection),
        onClick = { dialog_visibility.value = ! dialog_visibility.value },
        icon = R.drawable.icon_adjust,
        shape = Shapes.ContextMenuButtonPrimary,
        description = R.string.cd_adjust_selection
    )

    AdjustSelectionDialog(dialog_visibility, vm_state.radix.value) {
        opus_manager.offset_selection(it)
    }
}

@Composable
fun RemoveBeatButton(opus_manager: OpusLayerInterface, enabled: Boolean) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.BeatRemove),
        enabled = enabled,
        onClick = { opus_manager.remove_beat_at_cursor(1) },
        onLongClick = { opus_manager.remove_beat_at_cursor() },
        icon = R.drawable.icon_subtract,
        description = R.string.cd_remove_beat
    )
     // TODO: Dialog
}

@Composable
fun InsertBeatButton(opus_manager: OpusLayerInterface, shape: Shape = Shapes.ContextMenuButtonPrimary) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.BeatInsert),
        onClick = { opus_manager.insert_beat_after_cursor(1) },
        onLongClick = { opus_manager.insert_beat_after_cursor() },
        shape = shape,
        icon = R.drawable.icon_add,
        description = R.string.cd_insert_beat
    )
    // TODO: Dialog
}
@Composable
fun ContextMenuColumnPrimary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = vm_state.column_data[beat]

    when (layout) {
        LayoutSize.MediumLandscape,
        LayoutSize.SmallLandscape -> {
            Column {
                InsertBeatButton(opus_manager, Shapes.ContextMenuButtonPrimaryStart)
                MediumSpacer()
                RemoveBeatButton(opus_manager, vm_state.beat_count.value > 1)
                MediumSpacer()
                AdjustBeatButton(vm_state, opus_manager)
                Spacer(Modifier.weight(1F))
                TagButton(opus_manager, column_data, beat, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
        else -> {
            TagDescription(modifier, vm_state, opus_manager)
        }
    }
}

@Composable
fun ContextMenuColumnSecondary(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, layout: LayoutSize) {
    val cursor = vm_state.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = vm_state.column_data[beat]

    when (layout) {
        LayoutSize.MediumLandscape,
        LayoutSize.SmallLandscape -> {
            TagDescription(modifier, vm_state, opus_manager)
        }
        else -> {
            ContextMenuPrimaryRow(modifier) {
                TagButton(opus_manager, column_data, beat, Shapes.ContextMenuButtonPrimaryStart)
                MediumSpacer()
                AdjustBeatButton(vm_state, opus_manager)
                MediumSpacer()
                RemoveBeatButton(opus_manager, vm_state.beat_count.value > 1)
                MediumSpacer()
                InsertBeatButton(
                    opus_manager,
                    if (!column_data.is_tagged.value) {
                        Shapes.ContextMenuButtonPrimaryEnd
                    } else {
                        Shapes.ContextMenuButtonPrimary
                    }
                )
            }

        }
    }
}

@Composable
fun TagDescription(modifier: Modifier = Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface) {
    val cursor = vm_state.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = vm_state.column_data[beat]
    key(column_data.tag_content.value) {
        if (!column_data.is_tagged.value) return
        TextInput(
            input = remember { mutableStateOf(column_data.tag_content.value ?: "") },
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
            lineLimits = TextFieldLineLimits.MultiLine(
                minHeightInLines = 1,
                maxHeightInLines = 4
            ),
            shape = Shapes.ContextMenuButtonFull,
            callback_on_return = true,
            callback = {
                val description = it.trim().ifEmpty { null }
                opus_manager.tag_section(beat, description)
            }
        )
    }
}

