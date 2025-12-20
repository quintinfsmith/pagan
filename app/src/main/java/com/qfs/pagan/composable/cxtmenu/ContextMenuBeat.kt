package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.viewmodel.ViewModelEditorState
import com.qfs.pagan.viewmodel.ViewModelPagan


@Composable
fun TagButton(dispatcher: ActionTracker, column_data: ViewModelEditorState.ColumnData, beat: Int) {
    IconCMenuButton(
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
fun AdjustBeatButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun RemoveBeatButton(dispatcher: ActionTracker, enabled: Boolean) {
    IconCMenuButton(
        enabled = enabled,
        onClick = { dispatcher.remove_beat_at_cursor(1) },
        onLongClick = { dispatcher.remove_beat_at_cursor() },
        icon = R.drawable.icon_remove_beat,
        description = R.string.cd_remove_beat
    )
}

@Composable
fun InsertBeatButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.insert_beat_after_cursor(1) },
        onLongClick = { dispatcher.insert_beat_after_cursor() },
        icon = R.drawable.icon_insert_beat,
        description = R.string.cd_insert_beat
    )
}
@Composable
fun ContextMenuColumnPrimary(modifier: Modifier = Modifier, ui_facade: ViewModelEditorState, dispatcher: ActionTracker, layout: ViewModelPagan.LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    val beat = cursor.ints[0]
    val column_data = ui_facade.column_data[beat]

    when (layout) {
        ViewModelPagan.LayoutSize.SmallPortrait,
        ViewModelPagan.LayoutSize.MediumPortrait,
        ViewModelPagan.LayoutSize.LargePortrait,
        ViewModelPagan.LayoutSize.XLargePortrait -> {
            ContextMenuPrimaryRow(modifier) {
                TagButton(dispatcher, column_data, beat)
                CMPadding()
                AdjustBeatButton(dispatcher)
                CMPadding()
                RemoveBeatButton(dispatcher, ui_facade.beat_count.value > 1)
                CMPadding()
                InsertBeatButton(dispatcher)
            }
        }
        ViewModelPagan.LayoutSize.SmallLandscape,
        ViewModelPagan.LayoutSize.MediumLandscape,
        ViewModelPagan.LayoutSize.LargeLandscape,
        ViewModelPagan.LayoutSize.XLargeLandscape -> {
            Column {
                TagButton(dispatcher, column_data, beat)
                CMPadding()
                RemoveBeatButton(dispatcher, ui_facade.beat_count.value > 1)
                CMPadding()
                InsertBeatButton(dispatcher)
                CMPadding()
                AdjustBeatButton(dispatcher)
            }
        }
    }
}

@Composable
fun ContextMenuColumnSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, modifier: Modifier = Modifier) {}

