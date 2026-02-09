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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ComponentActivity.pow
import com.qfs.pagan.ComponentActivity.sqrt
import com.qfs.pagan.ui.theme.Dimensions.ContextMenuButtonRadius
import com.qfs.pagan.ui.theme.Dimensions.getter

object Shapes {
    val NumberSelectorButton = RoundedCornerShape(Dimensions.NumberSelectorButtonRadius)
    val NumberSelectorButtonStart = RoundedCornerShape(
        topStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.NumberSelectorButtonRadius,
        bottomStart = Dimensions.NumberSelectorButtonRadius,
        bottomEnd = Dimensions.NumberSelectorButtonRadius,
    )
    val NumberSelectorButtonEnd = RoundedCornerShape(
        topStart = Dimensions.NumberSelectorButtonRadius,
        topEnd = Dimensions.ContextMenuRadius,
        bottomStart = Dimensions.NumberSelectorButtonRadius,
        bottomEnd = Dimensions.NumberSelectorButtonRadius,
    )

    val ConfigChannelButtonEnd = RoundedCornerShape(
        0.dp,
        Dimensions.ConfigDrawerButtonRadius,
        Dimensions.ConfigDrawerButtonRadius,
        0.dp,
    )
    val ConfigChannelButtonStart = RoundedCornerShape(
        Dimensions.ConfigDrawerButtonRadius,
        0.dp,
        0.dp,
        Dimensions.ConfigDrawerButtonRadius,
    )
    val Container = RoundedCornerShape(12.dp)
    val ContextMenuButtonFull = RoundedCornerShape(
        topEnd = Dimensions.ContextMenuRadius,
        topStart = Dimensions.ContextMenuRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius
    )
    val ContextMenuButtonPrimary = RoundedCornerShape(ContextMenuButtonRadius)
    val ContextMenuButtonPrimaryStart = RoundedCornerShape(
        topStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
    )
    val ContextMenuButtonPrimaryEnd = RoundedCornerShape(
        topEnd = Dimensions.ContextMenuRadius,
        topStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius
    )
    val ContextMenuButtonPrimaryBottom = RoundedCornerShape(
        bottomStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuButtonRadius,
        topStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
    )

    val ContextMenuSecondaryButtonEnd: Shape
        get() = getter(
            small_portrait = ContextMenuButtonPrimary,
            small_landscape = ContextMenuButtonPrimaryEnd,
            medium_portrait = ContextMenuButtonPrimary,
        )
    val ContextMenuSecondaryButtonStart: Shape
        get() = getter(
            small_portrait = ContextMenuButtonPrimary,
            small_landscape = ContextMenuButtonPrimaryStart,
            medium_portrait = ContextMenuButtonPrimary,
        )


    val CMBoxBottom = RoundedCornerShape(
        topStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val CMBoxEnd = RoundedCornerShape(
        topEnd = 0.dp,
        topStart = Dimensions.ContextMenuRadius,
        bottomStart = Dimensions.ContextMenuRadius,
        bottomEnd = 0.dp
    )

    val Drawer = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)

    val LandingButtonShape = RoundedCornerShape(Dimensions.LandingButtonCornerRadius)

    object RadioMenu {
        val Start = RoundedCornerShape(50F, 0F, 0F, 50F)
        val Middle = RectangleShape
        val End = RoundedCornerShape(0F, 50F, 50F, 0F)
    }

    val SectionButtonEnd = RoundedCornerShape(0.dp, 50.dp, 50.dp, 0.dp)
    val SectionButtonCenter = RectangleShape
    val SectionButtonStart = RoundedCornerShape(
        50.dp,
        0.dp,
        0.dp,
        50.dp
    )

    val SettingsBox = RoundedCornerShape((Dimensions.SettingsBoxPadding.pow(2) * 2).sqrt())

    val SortableMenuBox = RoundedCornerShape(Dimensions.SortableMenuBoxRadius)

    val TaggedBeat = RoundedCornerShape(12.dp)
    val TopBar = RectangleShape
}