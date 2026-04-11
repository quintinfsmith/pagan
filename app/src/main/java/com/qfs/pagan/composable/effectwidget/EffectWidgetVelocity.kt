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

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionDispatcher
import com.qfs.pagan.ComponentActivity.PaganComponentActivity
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.Values
import com.qfs.pagan.composable.DivisorSeparator
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.composable.MediumSpacer
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.DropdownMenuItem
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.VelocityEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionDispatcher, event: OpusVelocityEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val working_value = remember { mutableFloatStateOf(event.value) }
    val velocity_input_value = remember { mutableIntStateOf((event.value * 100F).toInt()) }
    val slide_enabled = remember { mutableStateOf<Boolean>(event.slide != null) }
    val slide_width_mode = remember { mutableStateOf(event.slide?.first ?: OpusVelocityEvent.SlideMaxWidth.Note) }
    val denominator_label: MutableState<Int> = remember { mutableStateOf(event.slide?.second ?: Values.Defaults.SlideDenominator) }
    val (channel, line_offset, beat, position) = ui_facade.get_location_ints()
    val is_percussion = channel != null && ui_facade.channel_data[channel].percussion.value
    val active_layout_size = (LocalActivity.current as PaganComponentActivity).view_model.active_layout_size
    val default_colors = SliderDefaults.colors()
    val colors = default_colors.copy(
        activeTickColor = default_colors.inactiveTickColor,
        inactiveTickColor = default_colors.activeTickColor
    )

    val submit = {
        if (beat != null) {
            dispatcher.set_effect(EffectType.Velocity, event, channel, line_offset, beat, position!!, true)
        } else {
            dispatcher.set_initial_effect(EffectType.Velocity, event, channel, line_offset, true)
        }
    }

    val velocity_expanded = remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1F, fill = false)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(IntrinsicSize.Min)
                ) {
                    Button(
                        contentPadding = Dimensions.ContextMenuButtonPadding,
                        shape = Shapes.ContextMenuButtonPrimary,
                        modifier = Modifier
                            .testTag(TestTag.VelocityButton)
                            .height(Dimensions.ContextMenuButtonHeight)
                            .width(Dimensions.ContextMenuButtonWidth),
                        content = {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.icon_velocity),
                                    contentDescription = stringResource(R.string.ctl_desc_velocity)
                                )
                            }
                        },
                        onClick = {
                            velocity_expanded.value = !velocity_expanded.value
                        }
                    )
                    DropdownMenu(
                        velocity_expanded.value,
                        onDismissRequest = { velocity_expanded.value = false },
                        shape = CircleShape,
                        modifier = Modifier
                            .height(Dimensions.EffectWidget.Velocity.FadePopupHeight)
                            .width(Dimensions.EffectWidget.Velocity.FadePopupWidth)
                    ) {
                        Slider(
                            value = working_value.floatValue,
                            steps = 100,
                            colors = colors,
                            valueRange = 0F..1F,
                            modifier = Modifier
                                .testTag(TestTag.VelocityVSlider)
                                .padding(vertical = Dimensions.EffectWidget.Velocity.FadePopupPadding)
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
                                event.value = it
                                working_value.floatValue = it
                                velocity_input_value.intValue = (it * 100).toInt()
                            },

                            onValueChangeFinished = {
                                velocity_expanded.value = false
                                if (beat != null) {
                                    dispatcher.set_effect(
                                        EffectType.Velocity,
                                        event,
                                        channel,
                                        line_offset,
                                        beat,
                                        position!!,
                                        true
                                    )
                                } else {
                                    dispatcher.set_initial_effect(
                                        EffectType.Velocity,
                                        event,
                                        channel,
                                        line_offset,
                                        true
                                    )
                                }
                            }
                        )
                    }
                }
            }

            MediumSpacer()

            key(working_value.floatValue) {
                IntegerInput(
                    (event.value * 100F).toInt(),
                    minimum = 0,
                    maximum = 100,
                    contentPadding = Unpadded,
                    text_align = TextAlign.Center,
                    revert_on_exit = true,
                    modifier = Modifier
                        .testTag(TestTag.VelocityInput)
                        .height(Dimensions.EffectWidget.InputHeight)
                        .width(Dimensions.EffectWidget.Velocity.InputWidth)
                ) {
                    event.value = it.toFloat() / 100F
                    working_value.floatValue = event.value
                    submit()
                }
            }

        }
    }

    MediumSpacer()
    if (!is_percussion) {
        if (slide_enabled.value) {
            Column(
                Modifier
                    .height(IntrinsicSize.Min)
                    .weight(1F),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.velocity_widget_slide_duration))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box {
                        val bend_mode_dropdown_visible = remember { mutableStateOf(false) }
                        DropdownMenu(
                            expanded = bend_mode_dropdown_visible.value,
                            onDismissRequest = { bend_mode_dropdown_visible.value = false }
                        ) {
                            if (active_layout_size.value == LayoutSize.SmallPortrait) {
                                DropdownMenuItem(
                                    text = { Text(R.string.velocity_widget_disable_sliding) },
                                    onClick = {
                                        if (event.slide != null) {
                                            event.slide = null
                                            submit()
                                        }
                                        slide_enabled.value = false
                                        bend_mode_dropdown_visible.value = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(R.string.velocity_widget_relative_to_beat) },
                                onClick = {
                                    if (event.slide?.first != OpusVelocityEvent.SlideMaxWidth.Beat) {
                                        event.slide = Pair(
                                            OpusVelocityEvent.SlideMaxWidth.Beat,
                                            event.slide?.second ?: Values.Defaults.SlideDenominator
                                        )
                                        submit()
                                    }
                                    slide_width_mode.value = OpusVelocityEvent.SlideMaxWidth.Beat
                                    bend_mode_dropdown_visible.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(R.string.velocity_widget_relative_to_note) },
                                onClick = {
                                    if (event.slide?.first != OpusVelocityEvent.SlideMaxWidth.Note) {
                                        event.slide = Pair(
                                            OpusVelocityEvent.SlideMaxWidth.Note,
                                            event.slide?.second ?: Values.Defaults.SlideDenominator
                                        )
                                        submit()
                                    }
                                    slide_width_mode.value = OpusVelocityEvent.SlideMaxWidth.Note
                                    bend_mode_dropdown_visible.value = false
                                }
                            )
                        }

                        Button(
                            onClick = {
                                bend_mode_dropdown_visible.value = !bend_mode_dropdown_visible.value
                            }
                        ) {
                            when (slide_width_mode.value) {
                                OpusVelocityEvent.SlideMaxWidth.Beat -> Text(R.string.velocity_widget_slide_beat)
                                OpusVelocityEvent.SlideMaxWidth.Note -> Text(R.string.velocity_widget_slide_note)
                            }
                        }
                    }

                    DivisorSeparator()

                    Box(
                        modifier = Modifier,
                        contentAlignment = Alignment.Center
                    ) {
                        IntegerInput(
                            denominator_label.value,
                            minimum = 1,
                            revert_on_exit = true,
                            contentPadding = Unpadded,
                            text_align = TextAlign.Center,
                            modifier = Modifier
                                .testTag(TestTag.VelocitySlideDenominator)
                                .height(Dimensions.EffectWidget.InputHeight)
                                .width(Dimensions.EffectWidget.Velocity.InputWidth)
                        ) {
                            event.slide = Pair(slide_width_mode.value, it)
                            submit()
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1F))
            Text(R.string.velocity_widget_enable_sliding)
        }

        MediumSpacer()

        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (active_layout_size.value != LayoutSize.SmallPortrait || !slide_enabled.value) {
                    Switch(
                        checked = slide_enabled.value,
                        onCheckedChange = {
                            slide_enabled.value = it
                            if (it) {
                                event.slide = Pair(
                                    slide_width_mode.value,
                                    denominator_label.value
                                )
                            } else {
                                event.slide = null
                            }
                            submit()
                        }
                    )

                    MediumSpacer()
                }

                EffectTransitionButton(event, dispatcher, is_initial)
            }
        }
    } else {
        EffectTransitionButton(event, dispatcher, is_initial)
    }
}
