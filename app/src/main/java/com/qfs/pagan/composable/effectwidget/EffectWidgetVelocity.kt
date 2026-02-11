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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

@Composable
fun RowScope.VelocityEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusVelocityEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val working_value = remember { mutableFloatStateOf(event.value) }
    TextCMenuButton(
        modifier = Modifier.width(Dimensions.ContextMenuButtonWidth),
        text = "%02d".format((event.value * 100).roundToInt()),
        shape = Shapes.ContextMenuSecondaryButtonStart,
        onClick = {
            dispatcher.dialog_number_input(R.string.dlg_set_velocity, 0, 127, default = (event.value * 100).toInt()) {
                event.value = it.toFloat() / 100F
                dispatcher.set_effect_at_cursor(event)
            }
        },
        onLongClick = {
            working_value.floatValue = 1F
            event.value = 1F
            dispatcher.set_effect_at_cursor(event)
        }
    )
    MediumSpacer()
    Slider(
        modifier = Modifier
            .height(Dimensions.ContextMenuButtonHeight)
            .weight(1F),
        value = working_value.floatValue,
        valueRange = 0F .. 1.27F,
        onValueChange = {
            working_value.value = it
            event.value = it
        },
        onValueChangeFinished = {
            dispatcher.set_effect_at_cursor(event)
        },
    )

    EffectTransitionButton(event, dispatcher, is_initial)
}
