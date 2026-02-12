/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun HalfBorderBox(
    modifier: Modifier = Modifier,
    border_width: Dp = Dimensions.TableLineStroke,
    border_color: Color,
    content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier,
        contentAlignment = Alignment.BottomEnd,
        content = {
            Box(
                modifier = Modifier.fillMaxSize(),
                content = content,
                contentAlignment = Alignment.Center
            )
            Spacer(
                Modifier
                    .width(border_width)
                    .background(border_color)
                    .fillMaxHeight()
            )
        }
    )
}