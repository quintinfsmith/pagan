package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun ContextMenuRangePrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker) {
    Column {
        Row {
            Spacer(modifier = Modifier.fillMaxWidth().weight(1F))
            IconCMenuButton(
                modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
                onClick = { dispatcher.adjust_selection() },
                icon = R.drawable.icon_adjust,
                description = R.string.cd_adjust_selection
            )
            IconCMenuButton(
                modifier = Modifier.height(dimensionResource(R.dimen.icon_button_height)),
                onClick = { dispatcher.unset() },
                icon = R.drawable.icon_unset,
                description = R.string.cd_unset
            )
        }
    }
}
@Composable
fun ContextMenuRangeSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, move_mode: PaganConfiguration.MoveMode) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .height(dimensionResource(R.dimen.contextmenu_secondary_button_height)),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val cursor_mode = ui_facade.active_cursor.value?.type
            SText(
                when (move_mode) {
                    PaganConfiguration.MoveMode.MOVE -> {
                        if (cursor_mode == CursorMode.Range) {
                            R.string.label_move_range
                        } else {
                            R.string.label_move_beat
                        }
                    }
                    PaganConfiguration.MoveMode.COPY -> {
                        if (cursor_mode == CursorMode.Range) {
                            R.string.label_copy_range
                        } else {
                            R.string.label_copy_beat
                        }
                    }
                    PaganConfiguration.MoveMode.MERGE -> {
                        if (cursor_mode == CursorMode.Range) {
                            R.string.label_merge_range
                        } else {
                            R.string.label_merge_beat
                        }
                    }
                }
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceAround) {
            SingleChoiceSegmentedButtonRow {
                PaganConfiguration.MoveMode.entries.forEachIndexed { i, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = PaganConfiguration.MoveMode.entries.size
                        ),
                        onClick = { dispatcher.set_copy_mode(mode) },
                        selected = mode == move_mode,
                        label = { SText(
                            when (mode) {
                                PaganConfiguration.MoveMode.MOVE -> R.string.move_mode_move
                                PaganConfiguration.MoveMode.COPY -> R.string.move_mode_copy
                                PaganConfiguration.MoveMode.MERGE -> R.string.move_mode_merge
                            }
                        ) }
                    )
                }
            }
        }
    }
}

