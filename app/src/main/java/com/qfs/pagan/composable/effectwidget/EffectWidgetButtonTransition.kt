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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.DialogMenu
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes

@Composable
fun EffectTransitionButton(event: EffectEvent, opus_manager: OpusLayerInterface, is_initial: Boolean, modifier: Modifier = Modifier) {
    if (!is_initial) {
        val transition = event.transition
        val dialog_visibility = remember { mutableStateOf(false) }
        Spacer(Modifier.width(Dimensions.ContextMenuPadding))
        IconCMenuButton(
            modifier = modifier
                .testTag(TestTag.EffectTransition)
                .height(Dimensions.ContextMenuButtonHeight)
                .width(Dimensions.ContextMenuButtonWidth),
            onClick = { dialog_visibility.value = true },
            icon = when (transition) {
                EffectTransition.Instant -> R.drawable.icon_transition_immediate
                EffectTransition.Linear -> R.drawable.icon_transition_linear
                EffectTransition.InstantB -> R.drawable.icon_transition_rimmediate
                EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
                EffectTransition.LinearB -> R.drawable.icon_transition_linearb
            },
            shape = Shapes.ContextMenuSecondaryButtonEnd,
            description = R.string.cd_show_effect_controls
        )

        EffectTransitionDialog(dialog_visibility, event) {
            opus_manager.lock_cursor {
                event.transition = it
                opus_manager.set_event_at_cursor(event.copy())
            }
        }
    }

}

@Composable
fun EffectTransitionDialog(
    visibility: MutableState<Boolean>,
    active_event: EffectEvent,
    callback: (EffectTransition) -> Unit
) {
    val options_gen = {
        val options = mutableListOf<Pair<EffectTransition, @Composable RowScope.() -> Unit>>()
        for (transition_option in OpusLayerInterface.get_available_transitions(active_event.event_type)) {
            options.add(
                Pair(transition_option) {
                    Icon(
                        modifier = Modifier.height(Dimensions.EffectTransitionDialogIconHeight),
                        painter = painterResource(
                            when (transition_option) {
                                EffectTransition.Instant -> R.drawable.icon_transition_immediate
                                EffectTransition.Linear -> R.drawable.icon_transition_linear
                                EffectTransition.InstantB -> R.drawable.icon_transition_rimmediate
                                EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
                                EffectTransition.LinearB -> R.drawable.icon_transition_linearb
                            }
                        ),
                        contentDescription = null
                    )
                    Box(
                        modifier = Modifier.weight(1F),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (transition_option) {
                                EffectTransition.Instant -> R.string.effect_transition_instant
                                EffectTransition.Linear -> R.string.effect_transition_linear
                                EffectTransition.InstantB -> R.string.effect_transition_rinstant
                                EffectTransition.RLinear -> R.string.effect_transition_rlinear_out
                                EffectTransition.LinearB -> R.string.effect_transition_linearB
                            }
                        )
                    }
                }
            )
        }
        options
    }

    DialogMenu(
        visibility = visibility,
        title = R.string.dialog_transition,
        options = options_gen,
        default = active_event.transition,
        callback = { callback(it) }
    )
}
