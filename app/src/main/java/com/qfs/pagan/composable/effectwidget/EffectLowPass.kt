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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.Values
import com.qfs.pagan.composable.FloatInputDropDown
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

@Composable
fun RowScope.LowPassEventMenu(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, event: LowPassEvent) {
    val cursor = vm_state.active_cursor.value ?: return
    val working_event = event.copy()
    val is_initial = cursor.type == CursorMode.Line
    val working_cutoff = remember { mutableStateOf(working_event.filter_cutoff) }
    val dialog_visibility = remember { mutableStateOf(false) }

    TextCMenuButton(
        modifier = Modifier
            .testTag(TestTag.LowPassButton)
            .width(Dimensions.ContextMenuButtonWidth),
        text = if (working_cutoff.value == null) {
            "Disabled"
        } else {
            "%02d Hz".format((working_cutoff.value!!).roundToInt())
        },
        shape = Shapes.ContextMenuSecondaryButtonStart,
        onClick = { dialog_visibility.value = true },
        onLongClick = {
            working_cutoff.value = 1F
            working_event.filter_cutoff = 1F
            opus_manager.set_event_at_cursor(working_event)
        },
    )

    MediumSpacer()
    Slider(
        modifier = Modifier
            .testTag(TestTag.LowPassSlider)
            .height(Dimensions.ContextMenuButtonHeight)
            .weight(1F),
        value = working_cutoff.value ?: 0F,
        valueRange = Values.LowPassMinimum .. Values.LowPassMaximum,
        onValueChange = {
            working_event.filter_cutoff = it
            working_cutoff.value = it
        },
        onValueChangeFinished = {
            working_event.filter_cutoff = working_cutoff.value
            opus_manager.set_event_at_cursor(working_event)
        },
    )

    FloatInputDropDown(
        R.string.dlg_set_low_pass_frequency,
        dialog_visibility,
        remember { mutableFloatStateOf(working_event.filter_cutoff ?: 0F) },
        Values.LowPassMinimum,
        Values.LowPassMaximum,
    ) {
        working_event.filter_cutoff = it
        working_cutoff.value = it
        opus_manager.set_event_at_cursor(working_event)
    }

    EffectTransitionButton(working_event, opus_manager, is_initial)
}