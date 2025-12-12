package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ContextMenuColumnPrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    val cursor = ui_facade.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = ui_facade.column_data[beat]

    Row {
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
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
        Spacer(Modifier.weight(1F))
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.adjust_selection() },
            icon = R.drawable.icon_adjust,
            description = R.string.cd_adjust_selection
        )
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            enabled = ui_facade.beat_count.value > 1,
            onClick = { dispatcher.remove_beat_at_cursor(1) },
            onLongClick = { dispatcher.remove_beat_at_cursor() },
            icon = R.drawable.icon_remove_beat,
            description = R.string.cd_remove_beat
        )
        IconCMenuButton(
            modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
            onClick = { dispatcher.insert_beat_after_cursor(1) },
            onLongClick = { dispatcher.insert_beat_after_cursor() },
            icon = R.drawable.icon_insert_beat,
            description = R.string.cd_insert_beat
        )
    }
}

@Composable
fun ContextMenuColumnSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {}

