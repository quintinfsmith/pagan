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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun ColumnScope.TableLine(color: Color) {
    Spacer(
        Modifier.Companion
            .background(color = color)
            .fillMaxWidth()
            .height(Dimensions.TableLineStroke)
    )
}

@Composable
fun RowScope.TableLine(color: Color) {
    Spacer(
        Modifier
            .background(color = color)
            .fillMaxHeight()
            .width(Dimensions.TableLineStroke)
    )
}

@Composable
fun BoxScope.TableLine(color: Color) {
    Spacer(
        Modifier.Companion
            .background(color = color)
            .align(Alignment.Companion.CenterEnd)
            .fillMaxHeight()
            .width(Dimensions.TableLineStroke)
    )
}