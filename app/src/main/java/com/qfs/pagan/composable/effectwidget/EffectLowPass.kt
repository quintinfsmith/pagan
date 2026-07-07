/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.effectwidget

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.Values
import com.qfs.pagan.composable.FloatInput
import com.qfs.pagan.composable.PinchKnob
import com.qfs.pagan.composable.wrappers.Switch
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.LowPassEventMenu(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, event: LowPassEvent) {
    val cursor = vm_state.active_cursor.value ?: return
    val working_event = event.copy()
    val is_initial = cursor.type == CursorMode.Line
    val working_cutoff = remember { mutableStateOf(working_event.filter_cutoff) }
    val enabled = remember { mutableStateOf(working_cutoff.value != null) }

    Switch(
        enabled.value,
        onCheckedChange = { checked ->
            if (checked) {
                working_cutoff.value = Values.LowPassDefault
            } else {
                working_cutoff.value = null
            }
            enabled.value = checked
            working_event.filter_cutoff = working_cutoff.value
            opus_manager.set_event_at_cursor(working_event)
        }
    )

    working_cutoff.value?.let {
        PinchKnob(
            Modifier
                .testTag(TestTag.LowPassNob),
            remember { mutableFloatStateOf(0F) }
        )
        // Slider(
        //     modifier = Modifier
        //         .height(Dimensions.ContextMenuButtonHeight)
        //         .weight(1F),
        //     value = working_cutoff.value ?: 0F,
        //     valueRange = Values.LowPassMinimum .. Values.LowPassMaximum,
        //     onValueChange = {
        //         working_event.filter_cutoff = it
        //         working_cutoff.value = it
        //     },
        //     onValueChangeFinished = {
        //         working_event.filter_cutoff = working_cutoff.value
        //         opus_manager.set_event_at_cursor(working_event)
        //     },
        // )
        val tempo_label = remember { mutableFloatStateOf(working_cutoff.value ?: 0F) }
        FloatInput(
            tempo_label,
            precision = 3,
            revert_on_exit = true,
            prefix = {
                Icon(
                    modifier = Modifier
                        .width(Dimensions.ContextMenuButtonIconWidth),
                    painter = painterResource(R.drawable.icon_tempo),
                    contentDescription = null
                )
            },
            minimum = 1F,
            contentPadding = Unpadded,
            text_align = TextAlign.Center,
            modifier = Modifier
                .testTag(TestTag.Tempo)
                .height(Dimensions.EffectWidget.InputHeight)
                .weight(1F, fill = true)
        ) {
            if (it == 0F) {
                working_event.filter_cutoff = null
                working_cutoff.value = null
            } else {
                working_event.filter_cutoff = it
                working_cutoff.value = it
            }
            opus_manager.set_event_at_cursor(working_event)
        }
    }

    EffectTransitionButton(working_event, opus_manager, is_initial)
}