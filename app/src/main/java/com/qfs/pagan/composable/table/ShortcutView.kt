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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.DialogTitle
import com.qfs.pagan.composable.PaganDialog
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ShortcutView(modifier: Modifier, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, scope: CoroutineScope) {
    val dialog_visibility = remember { mutableStateOf(false) }
    HalfBorderBox(
        modifier
            .testTag(TestTag.ShortCut)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RectangleShape)
            .combinedClickable(
                onClick = { dialog_visibility.value = true },
                onLongClick = {
                    opus_manager.cursor_select_column(0)
                    scope.launch {
                        vm_state.scroll_state_x.value.scrollToItem(0)
                    }
                }
            ),
        border_color = MaterialTheme.colorScheme.onSurfaceVariant,
        content = {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ProvideContentColorTextStyle(MaterialTheme.colorScheme.onSurfaceVariant) {
                    Icon(
                        //modifier = Modifier.padding(Dimensions.ShortcutIconPadding),
                        painter = painterResource(R.drawable.icon_shortcut),
                        contentDescription = stringResource(R.string.jump_to_section)
                    )
                }
            }
        }
    )
    BeatSelectDialog(dialog_visibility, vm_state, opus_manager)
}

@Composable
fun BeatSelectDialog(visibility: MutableState<Boolean>, vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface) {
    PaganDialog(visibility) {
        val slider_position = remember {
            mutableFloatStateOf(
                when (vm_state.active_cursor.value?.type) {
                    CursorMode.Beat -> vm_state.active_cursor.value!!.ints[0]
                    CursorMode.Single -> vm_state.active_cursor.value!!.ints[1]
                    CursorMode.Range -> vm_state.active_cursor.value!!.ints[1]

                    null,
                    CursorMode.Channel,
                    CursorMode.Unset,
                    CursorMode.Line -> vm_state.scroll_state_x.value.firstVisibleItemIndex
                }.toFloat()
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DialogTitle(
                stringResource(
                    R.string.label_shortcut_scrollbar,
                    slider_position.floatValue.toInt(),
                    opus_manager.length - 1
                )
            )
        }

        val default_colors = SliderDefaults.colors()
        val colors = SliderColors(
            thumbColor = default_colors.thumbColor,
            activeTrackColor = default_colors.activeTrackColor,
            activeTickColor = default_colors.inactiveTickColor,
            inactiveTrackColor = default_colors.inactiveTrackColor,
            inactiveTickColor = default_colors.activeTickColor,
            disabledThumbColor = default_colors.disabledThumbColor,
            disabledActiveTrackColor = default_colors.disabledActiveTrackColor,
            disabledActiveTickColor = default_colors.disabledActiveTickColor,
            disabledInactiveTrackColor = default_colors.disabledInactiveTrackColor,
            disabledInactiveTickColor = default_colors.disabledInactiveTickColor
        )

        Slider(
            value = slider_position.floatValue,
            steps = opus_manager.length,
            colors = colors,
            valueRange = 0F..(opus_manager.length - 1).toFloat(),
            onValueChange = { value ->
                slider_position.floatValue = value
                opus_manager.cursor_select_column(value.toInt())
            }
        )

        // TODO:  Use a UI variable here instead of accessing opus_manager.marked_sections
        if (opus_manager.marked_sections.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val section_dropdown_visible = remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = {
                            section_dropdown_visible.value = !section_dropdown_visible.value
                        },
                        content = { Text(R.string.jump_to_section) }
                    )

                    DropdownMenu(
                        onDismissRequest = { section_dropdown_visible.value = false },
                        expanded = section_dropdown_visible.value
                    ) {
                        var section_index = 0
                        for ((i, tag) in opus_manager.marked_sections.toList().sortedBy { it.first }) {
                            DropdownMenuItem(
                                onClick = {
                                    visibility.value = false
                                    section_dropdown_visible.value = false
                                    opus_manager.cursor_select_column(i)
                                },
                                text = {
                                    if (tag == null) {
                                        Text(
                                            stringResource(
                                                R.string.section_spinner_item,
                                                i,
                                                section_index
                                            )
                                        )
                                    } else {
                                        Text("${"%02d".format(i)}: $tag")
                                    }
                                }
                            )
                            section_index++
                        }
                    }
                }
            }
        }
    }
}