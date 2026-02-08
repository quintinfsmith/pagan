/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.composable.wrappers.Slider
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography

@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    color: MutableState<Color>,
    show_alpha: Boolean = false
) {
    val alpha = remember { mutableFloatStateOf(color.value.alpha) }
    val red = remember { mutableFloatStateOf(color.value.red) }
    val green = remember { mutableFloatStateOf(color.value.green) }
    val blue = remember { mutableFloatStateOf(color.value.blue) }

    Column(modifier) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .background(color.value, shape = Shapes.Container)
                .height(Dimensions.ColorPickerPreviewHeight)
        )
        Spacer(Modifier.height(Dimensions.ColorPickerInnerPadding))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                R.string.red,
                style = Typography.ColorPickerLabel,
                modifier = Modifier.width(Dimensions.ColorPickerLabelWidth)
            )
            Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
            Slider(
                red.floatValue,
                colors = SliderDefaults.colors().copy(
                    activeTrackColor = Color(red = red.floatValue, green = 0F, blue = 0F)
                ),
                modifier = Modifier
                    .weight(1F, fill = false),
                onValueChange = {
                    red.floatValue = it
                    color.value = Color(
                        red = red.floatValue,
                        green = green.floatValue,
                        blue = blue.floatValue,
                        alpha = alpha.floatValue
                    )
                }
            )
            Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
            HexDisplay(value = red.value)
        }
        Spacer(Modifier.height(Dimensions.ColorPickerInnerPadding))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                R.string.green,
                style = Typography.ColorPickerLabel,
                modifier = Modifier.width(Dimensions.ColorPickerLabelWidth)
            )
            Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
            Slider(
                green.floatValue,
                colors = SliderDefaults.colors().copy(
                    activeTrackColor = Color(red = 0F, green = green.floatValue, blue = 0F)
                ),
                modifier = Modifier
                    .weight(1F, fill = false),
                onValueChange = {
                    green.floatValue = it
                    color.value = Color(
                        red = red.floatValue,
                        green = green.floatValue,
                        blue = blue.floatValue,
                        alpha = alpha.floatValue
                    )
                }
            )
            Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
            HexDisplay(value = green.value)
        }
        Spacer(Modifier.height(Dimensions.ColorPickerInnerPadding))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                R.string.blue,
                style = Typography.ColorPickerLabel,
                modifier = Modifier.width(Dimensions.ColorPickerLabelWidth)
            )
            Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
            Slider(
                blue.floatValue,
                colors = SliderDefaults.colors().copy(
                    activeTrackColor = Color(red = 0F, green = 0F, blue = blue.floatValue)
                ),
                modifier = Modifier
                    .weight(1F, fill = false),
                onValueChange = {
                    blue.floatValue = it
                    color.value = Color(
                        red = red.floatValue,
                        green = green.floatValue,
                        blue = blue.floatValue,
                        alpha = alpha.floatValue
                    )
                }
            )
            Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
            HexDisplay(value = blue.value)
        }
        if (show_alpha) {
            Spacer(Modifier.height(Dimensions.ColorPickerInnerPadding))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    R.string.alpha,
                    style = Typography.ColorPickerLabel,
                    modifier = Modifier.width(Dimensions.ColorPickerLabelWidth)
                )
                Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
                Slider(
                    alpha.floatValue,
                    modifier = Modifier
                        .width(Dimensions.ColorPickerSliderWidth)
                        .weight(1F, fill = false),
                    onValueChange = {
                        alpha.floatValue = it
                        color.value = Color(
                            red = red.floatValue,
                            green = green.floatValue,
                            blue = blue.floatValue,
                            alpha = alpha.floatValue
                        )
                    }
                )
                Spacer(Modifier.width(Dimensions.ColorPickerInnerPadding))
                HexDisplay(value = alpha.value)
            }
        }
    }
}

@Composable
fun HexDisplay(modifier: Modifier = Modifier, value: Float) {
    Surface(
        tonalElevation = 1.dp,
        shape = Shapes.Container,
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.onSurface)
    ) {
        Box(
            modifier = Modifier.height(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = modifier
                    .width(Dimensions.ColorPickerHexInputWidth),
                text = "%02X".format((value * 255F).toInt()),
                style = Typography.ColorPickerHexLabel
            )
        }
    }
}