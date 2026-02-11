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
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.TextInput
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState


@Composable
fun TagButton(
    dispatcher: ActionTracker,
    column_data: ViewModelEditorState.ColumnData,
    beat: Int,
    shape: Shape = Shapes.ContextMenuButtonPrimary
) {
    IconCMenuButton(
        onClick = {
            if (column_data.is_tagged.value) {
                dispatcher.untag_column(beat)
            } else {
                dispatcher.tag_column(beat, null, true)
            }
        },
        onLongClick = { dispatcher.tag_column(beat) },
        shape = shape,
        icon = if (column_data.is_tagged.value) R.drawable.icon_untag
        else R.drawable.icon_tag,
        description = if (column_data.is_tagged.value) R.string.cd_remove_section_mark
        else R.string.cd_mark_section
    )
}

@Composable
fun AdjustBeatButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        shape = Shapes.ContextMenuButtonPrimary,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun RemoveBeatButton(dispatcher: ActionTracker, enabled: Boolean) {
    IconCMenuButton(
        enabled = enabled,
        onClick = { dispatcher.remove_beat_at_cursor(1) },
        onLongClick = { dispatcher.remove_beat_at_cursor() },
        icon = R.drawable.icon_subtract,
        description = R.string.cd_remove_beat
    )
}

@Composable
fun InsertBeatButton(dispatcher: ActionTracker, shape: Shape = Shapes.ContextMenuButtonPrimary) {
    IconCMenuButton(
        onClick = { dispatcher.insert_beat_after_cursor(1) },
        onLongClick = { dispatcher.insert_beat_after_cursor() },
        shape = shape,
        icon = R.drawable.icon_add,
        description = R.string.cd_insert_beat
    )
}
@Composable
fun ContextMenuColumnPrimary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = ui_facade.column_data[beat]

    when (layout) {
        LayoutSize.MediumLandscape,
        LayoutSize.SmallLandscape -> {
            Column {
                InsertBeatButton(dispatcher, Shapes.ContextMenuButtonPrimaryStart)
                MediumSpacer()
                RemoveBeatButton(dispatcher, ui_facade.beat_count.value > 1)
                MediumSpacer()
                AdjustBeatButton(dispatcher)
                Spacer(Modifier.weight(1F))
                TagButton(dispatcher, column_data, beat, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
        else -> {
            TagDescription(modifier, ui_facade, dispatcher)
        }
    }
}

@Composable
fun ContextMenuColumnSecondary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = ui_facade.column_data[beat]

    when (layout) {
        LayoutSize.MediumLandscape,
        LayoutSize.SmallLandscape -> {
            TagDescription(modifier, ui_facade, dispatcher)
        }
        else -> {
            ContextMenuPrimaryRow(modifier) {
                TagButton(dispatcher, column_data, beat, Shapes.ContextMenuButtonPrimaryStart)
                MediumSpacer()
                AdjustBeatButton(dispatcher)
                MediumSpacer()
                RemoveBeatButton(dispatcher, ui_facade.beat_count.value > 1)
                MediumSpacer()
                InsertBeatButton(
                    dispatcher,
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
fun TagDescription(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = ui_facade.column_data[beat]
    key(column_data.tag_content.value) {
        if (!column_data.is_tagged.value) return
        val callback: (String) -> Unit = {
            val description = it.trim().ifEmpty { null }
            dispatcher.tag_column(beat, description = description, description == null)
        }
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
            callback = callback
        )
    }
}

