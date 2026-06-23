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

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PlaybackState
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.PaganDialog
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ShortcutView(modifier: Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, scope: CoroutineScope) {
    val (text_color, background_color) = if (vm_state.selecting_beat.value) {
        Pair(
            Colors.active_color_scheme.SHORTCUT_SELECTED_FOREGROUND,
            Colors.active_color_scheme.SHORTCUT_SELECTED
        )
    } else {
        Pair(
            Colors.active_color_scheme.SHORTCUT_FOREGROUND,
            Colors.active_color_scheme.SHORTCUT
        )
    }

    HalfBorderBox(
        modifier
            .testTag(TestTag.ShortCut)
            .background(background_color, shape = RectangleShape)
            .combinedClickable(
                onClick = { vm_state.selecting_beat.value = !vm_state.selecting_beat.value },
                onLongClick = {
                    opus_manager.cursor_select_column(0)
                    scope.launch {
                        vm_state.scroll_state_x.value.scrollToItem(0)
                    }
                }
            ),
        border_color = MaterialTheme.colorScheme.onSurfaceVariant,
        content = {
            ProvideContentColorTextStyle(text_color) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_shortcut),
                        contentDescription = stringResource(R.string.jump_to_section)
                    )
                }
            }
        }
    )
}