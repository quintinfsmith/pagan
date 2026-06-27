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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.PaganConfiguration
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.RadioMenu
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun AdjustRangeButton(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, shape: Shape) {
    val visibility = remember { mutableStateOf(false) }
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.AdjustSelection),
        onClick = { visibility.value = true },
        icon = R.drawable.icon_adjust,
        shape = shape,
        description = R.string.cd_adjust_selection
    )
    if (visibility.value) {
        AdjustSelectionDialog(visibility, vm_state.radix.value) { i ->
            opus_manager.offset_selection(i)
            opus_manager.cursor_clear()
        }
    }
}

@Composable
fun UnsetRangeButton(opus_manager: OpusLayerInterface, shape: Shape) {
    IconCMenuButton(
        modifier = Modifier.testTag(TestTag.RangeUnset),
        onClick = { opus_manager.unset() },
        icon = R.drawable.icon_erase,
        shape = shape,
        description = R.string.cd_unset
    )
}

@Composable
fun ExitRangeButton(
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface
) {
    Box(
        Modifier
            .width(41.dp)
            .height(41.dp)
            .clip(CircleShape)
            .clickable {
                opus_manager.cursor.range?.first?.let {
                    opus_manager.cursor_select(
                        it, opus_manager.get_first_position(it)
                    )
                } ?: opus_manager.cursor_clear()
            },
        contentAlignment = Alignment.Center
    ) {
        ProvideContentColorTextStyle(Colors.active_color_scheme.button) {
            Icon(
                painter = painterResource(R.drawable.icon_cross_circle),
                contentDescription = stringResource(R.string.close_beat_selector),
            )
        }
    }
}
@Composable
fun Helper(vm_state: ViewModelEditorState) {
    Text(
        when (vm_state.move_mode.value) {
            PaganConfiguration.MoveMode.MOVE -> R.string.label_move_beat
            PaganConfiguration.MoveMode.COPY -> R.string.label_copy_beat
            PaganConfiguration.MoveMode.MERGE -> R.string.label_merge_beat
        },
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun IRow(
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface
) {
}

@Composable
fun ModeRadio(vm_state: ViewModelEditorState) {
    RadioMenu(
        options = listOf(
            Pair(PaganConfiguration.MoveMode.MOVE) { Text(R.string.move_mode_move) },
            Pair(PaganConfiguration.MoveMode.COPY) { Text(R.string.move_mode_copy) }
        ),
        active = vm_state.move_mode,
        callback = { }
    )
}

@Composable
fun CRow(
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface
) {
}
@Composable
fun ContextMenuRangePrimary(
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface,
    layout: LayoutSize
) {
    when (layout) {
        LayoutSize.SmallLandscape,
        LayoutSize.MediumLandscape -> {
            Column(
                Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ExitRangeButton(vm_state, opus_manager)

                Spacer(Modifier.weight(1F))
                if (vm_state.std_notes_in_range.value) {
                    AdjustRangeButton(vm_state, opus_manager, Shapes.ContextMenuButtonPrimary)
                    MediumSpacer()
                }

                UnsetRangeButton(opus_manager, Shapes.ContextMenuButtonPrimaryBottom)
            }
        }
        else -> {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Spacer(
                    Modifier
                        .width(41.dp)
                        .height(41.dp)
                )
                Helper(vm_state)
                ExitRangeButton(vm_state, opus_manager)
            }
        }
    }

}

@Composable
fun ContextMenuRangeSecondary(
    vm_state: ViewModelEditorState,
    opus_manager: OpusLayerInterface,
    layout: LayoutSize
) {

    when (layout) {
        LayoutSize.SmallLandscape,
        LayoutSize.MediumLandscape -> {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Helper(vm_state)
                ModeRadio(vm_state)
            }
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (vm_state.std_notes_in_range.value) {
                    AdjustRangeButton(vm_state, opus_manager, Shapes.ContextMenuButtonPrimary)
                } else {
                    Spacer(
                        Modifier
                            .height(Dimensions.ContextMenuButtonHeight)
                            .width(Dimensions.ContextMenuButtonWidth)
                    )
                }

                ModeRadio(vm_state)
                UnsetRangeButton(opus_manager, Shapes.ContextMenuButtonPrimary)
            }
        }
    }
}

