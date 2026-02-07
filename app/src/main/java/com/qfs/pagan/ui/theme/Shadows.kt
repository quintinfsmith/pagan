/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

object Shadows {
    val Button = Shadow(
        radius = 0.dp,
        spread = 0.dp,
        alpha = .5f,
        offset = DpOffset(1.dp, 2.dp),
    )
    val Drawer = Shadow(
        radius = 4.dp,
        spread = 0.dp,
        alpha = .3f,
        offset = DpOffset(0.dp, 2.dp),
    )
    val ContextMenu = Drawer
    val TopBar = Shadow(
        radius = 0.dp,
        spread = 0.dp,
        alpha = .5f,
        offset = DpOffset(0.dp, 2.dp),
    )
    val ActiveLeaf = Shadow(
        radius = 0.dp,
        spread = 4.dp,
        alpha = 1f,
        offset = DpOffset(0.dp, 0.dp),
        color = Color.Yellow
    )
}