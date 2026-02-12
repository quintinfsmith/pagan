/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.table

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
                println("$path")
                LeafView(
                    line_info.channel.value?.let { ui_facade.channel_data[it] },
                    line_info,
                    leaf_data.value,
                    ui_facade.radix.value,
                    Modifier
                        .testTag("LeafView: ${line_info.channel.value}|${line_info.line_offset.value}|$x|$path")
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