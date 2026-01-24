package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.composable.RadioMenu
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun AdjustRangeButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        shape = Shapes.ContextMenuButtonPrimaryStart,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun UnsetRangeButton(dispatcher: ActionTracker) {
    IconCMenuButton(
        onClick = { dispatcher.unset() },
        icon = R.drawable.icon_erase,
        shape = Shapes.ContextMenuButtonPrimaryEnd,
        description = R.string.cd_unset
    )
}

@Composable
fun ContextMenuRangeSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, move_mode: PaganConfiguration.MoveMode) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdjustRangeButton(dispatcher)
            SingleChoiceSegmentedButtonRow {
                // TODO: MERGE
                val options = PaganConfiguration.MoveMode.entries.filter { it != PaganConfiguration.MoveMode.MERGE }
                for ((i, mode) in options.enumerate()) {
                    if (i != 0) {
                        Spacer(Modifier.width(4.dp))
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = i,
                            count = options.size
                        ),
                        onClick = { dispatcher.set_copy_mode(mode) },
                        selected = mode == move_mode,
                        label = {
                            SText(
                                when (mode) {
                                    PaganConfiguration.MoveMode.MOVE -> R.string.move_mode_move
                                    PaganConfiguration.MoveMode.COPY -> R.string.move_mode_copy
                                    PaganConfiguration.MoveMode.MERGE -> R.string.move_mode_merge
                                }
                            )
                        }
                    )
                }
            }
            UnsetRangeButton(dispatcher)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
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
    }
}

