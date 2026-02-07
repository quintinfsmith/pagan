/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.composable.RadioMenu
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.IconCMenuButton
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
            RadioMenu(
                options = listOf(
                    Pair(PaganConfiguration.MoveMode.MOVE) {
                        SText(R.string.move_mode_move)
                    },
                    Pair(PaganConfiguration.MoveMode.COPY) {
                        SText(R.string.move_mode_copy)
                    }
                ),
                active = remember { mutableStateOf(move_mode) },
                callback = { dispatcher.set_copy_mode(it) }
            )
            UnsetRangeButton(dispatcher)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SText(
                when (move_mode) {
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

