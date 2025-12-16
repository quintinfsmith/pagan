package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.viewmodel.ViewModelEditorState


@Composable
fun TagButton(modifier: Modifier = Modifier, dispatcher: ActionTracker, column_data: ViewModelEditorState.ColumnData, beat: Int) {
    IconCMenuButton(
        modifier = modifier,
        onClick = {
            if (column_data.is_tagged.value) {
                dispatcher.untag_column(beat)
            } else {
                dispatcher.tag_column(beat, null, true)
            }
        },
        onLongClick = { dispatcher.tag_column(beat) },
        icon = if (column_data.is_tagged.value) R.drawable.icon_untag
        else R.drawable.icon_tag,
        description = if (column_data.is_tagged.value) R.string.cd_remove_section_mark
        else R.string.cd_mark_section
    )
}

@Composable
fun AdjustBeatButton(modifier: Modifier = Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun RemoveBeatButton(modifier: Modifier, dispatcher: ActionTracker, enabled: Boolean) {
    IconCMenuButton(
        modifier = modifier,
        enabled = enabled,
        onClick = { dispatcher.remove_beat_at_cursor(1) },
        onLongClick = { dispatcher.remove_beat_at_cursor() },
        icon = R.drawable.icon_remove_beat,
        description = R.string.cd_remove_beat
    )
}

@Composable
fun InsertBeatButton(modifier: Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.insert_beat_after_cursor(1) },
        onLongClick = { dispatcher.insert_beat_after_cursor() },
        icon = R.drawable.icon_insert_beat,
        description = R.string.cd_insert_beat
    )
}
@Composable
fun ContextMenuColumnPrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, landscape: Boolean) {
    val cursor = ui_facade.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = ui_facade.column_data[beat]

    if (landscape) {
        Column(Modifier.width(dimensionResource(R.dimen.contextmenu_primary_width))) {
            TagButton(Modifier.fillMaxWidth(), dispatcher, column_data, beat)
            RemoveBeatButton(Modifier.fillMaxWidth(), dispatcher, ui_facade.beat_count.value > 1)
            InsertBeatButton(Modifier.fillMaxWidth(), dispatcher)
            AdjustBeatButton(Modifier.fillMaxWidth(), dispatcher)
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.contextmenu_primary_height)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TagButton(
                Modifier
                    .width(dimensionResource(R.dimen.contextmenu_button_width))
                    .fillMaxHeight(),
                dispatcher,
                column_data,
                beat
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))
            AdjustBeatButton(
                Modifier
                    .width(dimensionResource(R.dimen.contextmenu_button_width))
                    .fillMaxHeight(),
                dispatcher
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))
            RemoveBeatButton(
                Modifier
                    .width(dimensionResource(R.dimen.contextmenu_button_width))
                    .fillMaxHeight(),
                dispatcher,
                ui_facade.beat_count.value > 1
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))
            InsertBeatButton(
                Modifier
                    .width(dimensionResource(R.dimen.contextmenu_button_width))
                    .fillMaxHeight(),
                dispatcher
            )
        }
    }
}

@Composable
fun ContextMenuColumnSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {}

