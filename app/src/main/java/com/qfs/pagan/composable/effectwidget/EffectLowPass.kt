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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.Values
import com.qfs.pagan.composable.FloatInput
import com.qfs.pagan.composable.Knob
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.FilterEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.HighPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun <T: FilterEvent> RowScope.FilterEventMenu(vm_state: ViewModelEditorState, opus_manager: OpusLayerInterface, event: T) {
    val cursor = vm_state.active_cursor.value ?: return
    val working_event = event.copy()
    val is_initial = cursor.type == CursorMode.Line
    val working_cutoff = remember { mutableFloatStateOf(working_event.filter_cutoff) }


    IconCMenuButton(
        modifier = Modifier
            .testTag(TestTag.EventUnset)
            .height(Dimensions.ContextMenuButtonHeight)
            .width(Dimensions.ContextMenuButtonWidth),
        enabled = (working_event is HighPassEvent && working_cutoff.floatValue > Values.LowPassMinimum) || (working_event is LowPassEvent && working_cutoff.floatValue < Values.LowPassMaximum),
        onClick = {
            working_cutoff.floatValue = when (working_event) {
                is HighPassEvent -> Values.LowPassMinimum
                is LowPassEvent -> Values.LowPassMaximum
                else -> return@IconCMenuButton
            }
            working_event.filter_cutoff = working_cutoff.floatValue
            opus_manager.set_event_at_cursor(working_event)
        },
        icon = R.drawable.no_soundfont,
        description = R.string.disable_filter,
    )

    MediumSpacer()

    Knob(
        Modifier
            .size(Dimensions.ContextMenuButtonWidth, Dimensions.ContextMenuButtonHeight)
            .testTag(TestTag.FilterKnob),
        working_cutoff,
        Values.LowPassMinimum,
        Values.LowPassMaximum,
        precision = 0,
        rotations = 4
    ) { knob_value ->
        working_cutoff.floatValue = knob_value
        working_event.filter_cutoff = working_cutoff.floatValue
        opus_manager.set_event_at_cursor(working_event)
    }

    Spacer(Modifier.weight(.25F))

    FloatInput(
        working_cutoff,
        precision = 3,
        revert_on_exit = true,
        prefix = {
            Icon(
                modifier = Modifier
                    .width(Dimensions.ContextMenuButtonIconWidth),
                painter = painterResource(R.drawable.icon_lowpass),
                contentDescription = null
            )
        },
        minimum = Values.LowPassMinimum,
        maximum = Values.LowPassMaximum,
        contentPadding = Unpadded,
        text_align = TextAlign.Center,
        modifier = Modifier
            .testTag(TestTag.FilterInput)
            .height(Dimensions.EffectWidget.InputHeight)
            .weight(1F, fill = false)
    ) {
        working_event.filter_cutoff = it
        working_cutoff.floatValue = it
        opus_manager.set_event_at_cursor(working_event)
    }

    MediumSpacer()

    Text(
        R.string.hz,
        style = Typography.ContextMenuUnits
    )

    Spacer(Modifier.weight(.25F))

    EffectTransitionButton(working_event, opus_manager, is_initial)
}