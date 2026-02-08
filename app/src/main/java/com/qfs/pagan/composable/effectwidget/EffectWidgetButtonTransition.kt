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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes

@Composable
fun EffectTransitionButton(event: EffectEvent, dispatcher: ActionTracker, is_initial: Boolean, modifier: Modifier = Modifier) {
    val transition = event.transition
    if (!is_initial) {
        Spacer(Modifier.width(Dimensions.ContextMenuPadding))
        IconCMenuButton(
            modifier = modifier
                .height(Dimensions.ContextMenuButtonHeight)
                .width(Dimensions.ContextMenuButtonWidth),
            onClick = {
                dispatcher.set_effect_transition(event)
            },
            icon = when (transition) {
                EffectTransition.Instant -> R.drawable.icon_transition_immediate
                EffectTransition.Linear -> R.drawable.icon_transition_linear
                EffectTransition.RInstant -> R.drawable.icon_transition_rimmediate
                EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
            },
            shape = Shapes.ContextMenuSecondaryButtonEnd,
            description = R.string.cd_show_effect_controls
        )
    }
}