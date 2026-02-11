package com.qfs.pagan.composable.table

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun CellView(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, cell: MutableState<ViewModelEditorState.TreeData>, y: Int, x: Int, modifier: Modifier = Modifier) {
    val line_info = ui_facade.line_data[y]
    key(cell.value.key.value, y) {
        Row(
            modifier
                .fillMaxSize()
        ) {
            for ((path, leaf_data) in cell.value.leafs) {
                LeafView(
                    line_info.channel.value?.let { ui_facade.channel_data[it] },
                    line_info,
                    leaf_data.value,
                    ui_facade.radix.value,
                    Modifier
                        .weight(leaf_data.value.weight.floatValue)
                        .combinedClickable(
                            onClick = {
                                dispatcher.tap_leaf(
                                    x,
                                    path,
                                    line_info.channel.value,
                                    line_info.line_offset.value,
                                    line_info.ctl_type.value
                                )
                            },
                            onLongClick = {
                                dispatcher.long_tap_leaf(
                                    x,
                                    path,
                                    line_info.channel.value,
                                    line_info.line_offset.value,
                                    line_info.ctl_type.value
                                )
                            }
                        )
                )
            }
        }
    }
}