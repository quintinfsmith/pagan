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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.RadioMenu
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun AdjustRangeButton(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface) {
    val visibility = remember { mutableStateOf(false) }
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.AdjustSelection),
        onClick = { visibility.value = true },
        icon = R.drawable.icon_adjust,
        shape = Shapes.ContextMenuButtonPrimaryStart,
        description = R.string.cd_adjust_selection
    )
    if (visibility.value) {
        AdjustSelectionDialog(visibility, vm_state.radix.value) { i ->
            opus_manager.offset_selection(i)
        }
    }
}

@Composable
fun UnsetRangeButton(opus_manager: OpusLayerInterface) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.RangeUnset),
        onClick = { opus_manager.unset() },
        icon = R.drawable.icon_erase,
        shape = Shapes.ContextMenuButtonPrimaryEnd,
        description = R.string.cd_unset
    )
}

@Composable
fun ContextMenuRangeSecondary(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, move_mode: MutableState<PaganConfiguration.MoveMode>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AdjustRangeButton(vm_state, opus_manager)
            RadioMenu(
                options = listOf(
                    Pair(PaganConfiguration.MoveMode.MOVE) {
                        Text(R.string.move_mode_move)
                    },
                    Pair(PaganConfiguration.MoveMode.COPY) {
                        Text(R.string.move_mode_copy)
                    }
                ),
                active = move_mode,
                callback = { }
            )
            UnsetRangeButton(opus_manager)
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                when (move_mode.value) {
                    PaganConfiguration.MoveMode.MOVE -> R.string.label_move_beat
                    PaganConfiguration.MoveMode.COPY -> R.string.label_copy_beat
                    PaganConfiguration.MoveMode.MERGE -> R.string.label_merge_beat
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

