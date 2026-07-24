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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.Values
import com.qfs.pagan.composable.FloatInput
import com.qfs.pagan.composable.Knob
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.MasterTheme
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.TempoEventMenu(ui_facade: ViewModelEditorState, opus_manager: OpusLayerInterface, event: OpusTempoEvent, layout: LayoutSize) {
    val cursor = ui_facade.active_cursor.value ?: return
    val working_event = event.copy()
    val is_initial = cursor.type == CursorMode.Line

    Spacer(Modifier.weight(.25F))

    val tempo_label = remember { mutableFloatStateOf(working_event.value) }

    Knob(
        Modifier
            .testTag(TestTag.TempoKnob)
            .size(
                MasterTheme.dimensions.ContextMenuButtonWidth,
                MasterTheme.dimensions.ContextMenuButtonHeight,
            ),
        tempo_label,
        minimum = Values.TempoMinimum,
        maximum = Values.TempoMaximum,
        precision = 0,
        rotations = 8,
    ) {
        working_event.value = it
        opus_manager.lock_cursor {
            opus_manager.set_event_at_cursor(working_event)
        }
    }

    Spacer(Modifier.weight(.25F))

    FloatInput(
        tempo_label,
        precision = 3,
        revert_on_exit = true,
        prefix = {
            Icon(
                modifier = Modifier.width(MasterTheme.dimensions.ContextMenuButtonIconWidth),
                painter = painterResource(R.drawable.icon_tempo),
                contentDescription = null
            )
        },
        minimum = Values.TempoMinimum,
        contentPadding = MasterTheme.dimensions.Unpadded,
        text_align = TextAlign.Center,
        modifier = Modifier
            .testTag(TestTag.Tempo)
            .height(MasterTheme.dimensions.EffectWidgetInputHeight)
            .weight(1F, fill = true)
    ) {
        working_event.value = it
        opus_manager.lock_cursor {
            opus_manager.set_event_at_cursor(working_event)
        }
    }

    ProvideContentColorTextStyle(
        contentColor = Colors.active_color_scheme.context_menu_foreground,
        content = {
            Text(
                R.string.bpm,
                Modifier.padding(start = MasterTheme.dimensions.SpaceMedium),
                style = MasterTheme.typography.ContextMenuUnits
            )
        }
    )

    Spacer(Modifier.weight(.5F))

    EffectTransitionButton(working_event, opus_manager, is_initial, layout)
}