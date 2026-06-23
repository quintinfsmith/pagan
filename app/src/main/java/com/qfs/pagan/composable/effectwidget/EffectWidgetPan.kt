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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.wrappers.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RowScope.PanEventMenu(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, event: OpusPanEvent) {
    val cursor = vm_state.active_cursor.value ?: return
    val working_event = event.copy()
    val is_initial = cursor.type == CursorMode.Line
    val colors = Colors.get_pan_slider_colors()

    val submit = {
        opus_manager.lock_cursor {
            opus_manager.set_event_at_cursor(working_event)
        }
    }

    val working_value = remember { mutableFloatStateOf(working_event.value * -1) }

    Row(
        modifier = Modifier.weight(1F),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ProvideTextStyle(
            MaterialTheme.typography.bodyMedium
        ) {
            Text(
                text = if (working_value.floatValue < 0F) {
                    "<${(abs(working_value.floatValue) * 10).roundToInt()}"
                } else if (working_value.floatValue > 0F) {
                    "${(abs(working_value.floatValue) * 10).roundToInt()}>"
                } else {
                    "-0-"
                },
                maxLines = 1,
                textAlign = TextAlign.Center,
                lineHeight = 2.em,
                modifier = Modifier
                    .width(Dimensions.EffectWidget.Pan.SliderPadding)
                    .clip(CircleShape)
                    .background(Colors.active_color_scheme.button),
                color = Colors.active_color_scheme.button_foreground
            )
        }

        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ){
            Spacer(
                Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .clip(CircleShape)
                    .background(Colors.active_color_scheme.SLIDER_TRACK_INACTIVE)
            )
            Slider(
                value = working_value.floatValue,
                onValueChange = {
                    working_value.floatValue = it
                },
                onValueChangeFinished = {
                    working_value.floatValue = (working_value.floatValue * 10F).roundToInt().toFloat() / 10F
                    working_event.value = working_value.floatValue * -1
                    submit()
                },
                valueRange = -1F..1F,
                colors = colors,
                modifier = Modifier
                    .padding(
                        start = Dimensions.EffectWidget.Pan.SliderPadding / 2,
                        end = Dimensions.EffectWidget.Pan.SliderPadding / 2
                    )
                    .testTag(TestTag.PanSlider)
                    .fillMaxWidth()
            )
        }
    }
    LaunchedEffect(working_event.value) {
        working_value.floatValue = working_event.value * -1
    }

    EffectTransitionButton(working_event, opus_manager, is_initial)
}
