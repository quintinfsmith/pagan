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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.PanEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusPanEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val default_colors = SliderDefaults.colors()
    val colors = SliderColors(
        thumbColor = default_colors.thumbColor,
        activeTrackColor = default_colors.activeTrackColor,
        activeTickColor = MaterialTheme.colorScheme.surface,
        inactiveTrackColor = default_colors.activeTrackColor,
        inactiveTickColor = MaterialTheme.colorScheme.surface,
        disabledThumbColor = default_colors.disabledThumbColor,
        disabledActiveTrackColor = default_colors.disabledActiveTrackColor,
        disabledActiveTickColor = default_colors.disabledActiveTickColor,
        disabledInactiveTrackColor = default_colors.disabledInactiveTrackColor,
        disabledInactiveTickColor = default_colors.disabledInactiveTickColor
    )

    key(ui_facade.active_event.value.hashCode()) {
        val working_value = remember { mutableFloatStateOf(event.value) }
        Box(modifier = Modifier.weight(1F)) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .background(colors.thumbColor, CircleShape)
                    .height(12.dp)
                    .width(12.dp)
            )
            Slider(
                value = working_value.floatValue,
                onValueChange = {
                    working_value.floatValue = it
                },
                onValueChangeFinished = {
                    event.value = working_value.floatValue
                    dispatcher.set_effect_at_cursor(event)
                },
                valueRange = -1F..1F,
                steps = 21,
                colors = colors,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
            )
        }
    }

    EffectTransitionButton(event, dispatcher, is_initial)
}
