package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.qfs.pagan.enumerate
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState
@Composable
fun AdjustRangeButton(modifier: Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.adjust_selection() },
        icon = R.drawable.icon_adjust,
        description = R.string.cd_adjust_selection
    )
}

@Composable
fun UnsetRangeButton(modifier: Modifier, dispatcher: ActionTracker) {
    IconCMenuButton(
        modifier = modifier,
        onClick = { dispatcher.unset() },
        icon = R.drawable.icon_unset,
        description = R.string.cd_unset
    )
}

@Composable
fun ContextMenuRangePrimary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, landscape: Boolean) {
    if (landscape) {
        Column(modifier = Modifier.width(dimensionResource(R.dimen.contextmenu_primary_width))) {
            UnsetRangeButton(Modifier.fillMaxWidth(), dispatcher)
            AdjustRangeButton(Modifier.fillMaxWidth(), dispatcher)
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.icon_button_height))
        ) {
            AdjustRangeButton(
                Modifier
                    .width(dimensionResource(R.dimen.contextmenu_button_width))
                    .fillMaxHeight(),
                dispatcher
            )
            Spacer(modifier = Modifier.fillMaxWidth().weight(1F))
            UnsetRangeButton(
                Modifier
                    .width(dimensionResource(R.dimen.contextmenu_button_width))
                    .fillMaxHeight(),
                dispatcher
            )
        }
    }
}

@Composable
fun ContextMenuRangeSecondary(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, move_mode: PaganConfiguration.MoveMode) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {

            SingleChoiceSegmentedButtonRow {
                // TODO: MERGE
                val options = PaganConfiguration.MoveMode.entries.filter { it != PaganConfiguration.MoveMode.MERGE }
                for ((i, mode) in options.enumerate()) {

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
        }
    }
}

