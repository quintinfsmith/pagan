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
import androidx.compose.runtime.Composable
import com.qfs.pagan.ActionDispatcher
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.ReverbEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionDispatcher, event: OpusReverbEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line

    // Row {
    //     TextCMenuButton(
    //         text = "${event.value}",
    //         onClick = {},
    //         onLongClick = {}
    //     )

    //     Slider(
    //         value = event.value,
    //         onValueChange = { dispatcher.set_volume_at_cursor(it) },
    //         modifier = Modifier.fillMaxWidth().weight(2F)
    //     )

    //     if (!is_initial) {
    //         IconCMenuButton(
    //             onClick = { dispatcher.set_ctl_transition() },
    //             icon = when (event.transition) {
    //                 EffectTransition.Instant -> R.drawable.icon_transition_immediate
    //                 EffectTransition.Linear -> R.drawable.icon_transition_linear
    //                 EffectTransition.RInstant -> R.drawable.icon_transition_rimmediate
    //                 EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
    //             },
    //             description = R.string.cd_show_effect_controls
    //         )
    //     }
    // }
}