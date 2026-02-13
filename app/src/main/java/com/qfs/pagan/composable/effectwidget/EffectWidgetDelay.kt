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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.DivisorSeparator
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

@Composable
fun RowScope.DelayEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: DelayEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val fade = remember { mutableFloatStateOf(event.fade) }
    val (channel, line_offset, beat, position) = ui_facade.get_location_ints()

    val default_colors = SliderDefaults.colors()
    val colors = default_colors.copy(
        activeTickColor = default_colors.inactiveTickColor,
        inactiveTickColor = default_colors.activeTickColor
    )

    val echo_label = remember { mutableIntStateOf(event.echo + 1) }
    val numerator_label = remember { mutableIntStateOf(event.numerator) }
    val denominator_label = remember { mutableIntStateOf(event.denominator) }

    val submit = {
        if (beat != null) {
            dispatcher.set_effect(EffectType.Delay, event, channel, line_offset, beat, position!!)
        } else {
            dispatcher.set_initial_effect(EffectType.Delay, event, channel, line_offset)
        }
    }

    Spacer(Modifier.weight(.5F))

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .alpha(.2F)
                .width(Dimensions.EffectWidget.Delay.InputIconWidth),
            painter = painterResource(R.drawable.icon_hz),
            contentDescription = null
        )
        val numerator_key = remember { mutableStateOf(false) }
        key(numerator_key.value) {
            IntegerInput(
                numerator_label,
                contentPadding = Unpadded,
                text_align = TextAlign.Center,
                on_focus_exit = {
                    event.numerator = numerator_label.value
                    submit()
                },
                modifier = Modifier
                    .testTag(TestTag.DelayHzNumerator)
                    .height(Dimensions.EffectWidget.InputHeight)
                    .width(Dimensions.EffectWidget.Delay.InputWidth)
            ) {
                event.numerator = it
                submit()
            }
        }
    }
    DivisorSeparator()
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .alpha(.2F)
                .width(Dimensions.EffectWidget.Delay.InputIconWidth),
            painter = painterResource(R.drawable.icon_hz),
            contentDescription = null
        )
        val denominator_key = remember { mutableStateOf(false) }
        key(denominator_key.value) {
            IntegerInput(
                denominator_label,
                minimum = 1,
                on_focus_exit = {
                    event.denominator = denominator_label.value
                    submit()
                },
                contentPadding = Unpadded,
                text_align = TextAlign.Center,
                modifier = Modifier
                    .testTag(TestTag.DelayHzDenominator)
                    .height(Dimensions.EffectWidget.InputHeight)
                    .width(Dimensions.EffectWidget.Delay.InputWidth)
            ) {
                event.denominator = it
                submit()
            }
        }
    }

    Spacer(Modifier.weight(1F))

    Box(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .alpha(.2F)
                .width(Dimensions.EffectWidget.Delay.InputIconWidth),
            painter = painterResource(R.drawable.icon_echo),
            contentDescription = null
        )
        val echo_key = remember { mutableStateOf(false) }
        key(echo_key.value) {
            IntegerInput(
                echo_label,
                on_focus_exit = {
                    event.echo = echo_label.value -1
                    submit()
                },
                minimum = 1,
                contentPadding = Unpadded,
                text_align = TextAlign.Center,
                modifier = Modifier
                    .testTag(TestTag.DelayEcho)
                    .height(Dimensions.EffectWidget.InputHeight)
                    .width(Dimensions.EffectWidget.Delay.InputWidth)
            ) {
                event.echo = (it - 1)
                submit()
            }
        }
    }

    MediumSpacer()

    val fade_expanded = remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        Button(
            contentPadding = Dimensions.ContextMenuButtonPadding,
            shape = Shapes.ContextMenuButtonPrimary,
            modifier = Modifier
                .testTag(TestTag.DelayFadeButton)
                .height(Dimensions.ContextMenuButtonHeight)
                .width(Dimensions.ContextMenuButtonWidth),
            content = {
                Box(contentAlignment = Alignment.Center) {
                    if (!fade_expanded.value) {
                        Icon(
                            modifier = Modifier.alpha(.2f),
                            painter = painterResource(R.drawable.icon_volume),
                            contentDescription = stringResource(R.string.cd_fade)
                        )
                    }
                    Text(
                        text = "${(fade.floatValue * 100).roundToInt()}%",
                        style = Typography.ContextMenuButton
                    )
                }
            },
            onClick = {
                fade_expanded.value = !fade_expanded.value
            }
        )
        DropdownMenu(
            fade_expanded.value,
            onDismissRequest = { fade_expanded.value = false },
            shape = CircleShape,
            modifier = Modifier
                .height(Dimensions.EffectWidget.Delay.FadePopupHeight)
                .width(Dimensions.EffectWidget.Delay.FadePopupWidth)
        ) {
            Slider(
                value = fade.floatValue,
                steps = 100,
                colors = colors,
                valueRange = 0F..1F,
                modifier = Modifier
                    .testTag(TestTag.DelayFadeSlider)
                    .padding(vertical = Dimensions.EffectWidget.Delay.FadePopupPadding)
                    .graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .weight(1F)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxHeight,
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(-placeable.width, 0)
                        }
                    },

                onValueChange = {
                    event.fade = it
                    fade.floatValue = it
                },
                onValueChangeFinished = {
                    if (beat != null) {
                        dispatcher.set_effect(EffectType.Delay, event, channel, line_offset, beat, position!!)
                    } else {
                        dispatcher.set_initial_effect(EffectType.Delay, event, channel, line_offset)
                    }
                }
            )
        }
    }

    EffectTransitionButton(event, dispatcher, is_initial)
}

