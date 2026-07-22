/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.wrappers

import androidx.annotation.IntRange
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.qfs.pagan.ui.theme.Colors

// Every time i set "steps" to be the *actual* number of discrete values I want - 2 I get a stress headache. So I'm not doing that.
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    @IntRange(from = 0) steps: Int = 2,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = Colors.get_slider_colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val animated_value by animateFloatAsState(
        targetValue = value,
        label = "slider"
    )
    Slider(
        animated_value,
        onValueChange,
        modifier,
        enabled,
        valueRange,
        steps - 2,
        onValueChangeFinished,
        colors,
        interactionSource
    )
}