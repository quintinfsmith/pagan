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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ComponentActivity.pow
import com.qfs.pagan.ComponentActivity.sqrt

data class PaganShapes(
    val ButtonDefault: Shape = CircleShape,
    val NumberSelectorButton: Shape = RoundedCornerShape(MasterTheme.dimensions.NotePickerButtonRadius),
    val NumberSelectorButtonStart: Shape = RoundedCornerShape(
        topStart = MasterTheme.dimensions.ContextMenuRadius,
        topEnd = MasterTheme.dimensions.NotePickerButtonRadius,
        bottomStart = MasterTheme.dimensions.NotePickerButtonRadius,
        bottomEnd = MasterTheme.dimensions.NotePickerButtonRadius,
    ),
    val NumberSelectorButtonEnd: Shape = RoundedCornerShape(
        topStart = MasterTheme.dimensions.NotePickerButtonRadius,
        topEnd = MasterTheme.dimensions.ContextMenuRadius,
        bottomStart = MasterTheme.dimensions.NotePickerButtonRadius,
        bottomEnd = MasterTheme.dimensions.NotePickerButtonRadius,
    ),
    val ConfigChannelButtonEnd: Shape = RoundedCornerShape(
        0.dp,
        MasterTheme.dimensions.ConfigDrawerButtonRadius,
        MasterTheme.dimensions.ConfigDrawerButtonRadius,
        0.dp,
    ),
    val ConfigChannelButtonStart: Shape = RoundedCornerShape(
        MasterTheme.dimensions.ConfigDrawerButtonRadius,
        0.dp,
        0.dp,
        MasterTheme.dimensions.ConfigDrawerButtonRadius,
    ),
    val Container: Shape = RoundedCornerShape(12.dp),
    val ContextMenuButtonFull: Shape = RoundedCornerShape(
        topEnd = MasterTheme.dimensions.ContextMenuRadius,
        topStart = MasterTheme.dimensions.ContextMenuRadius,
        bottomEnd = MasterTheme.dimensions.ContextMenuButtonRadius,
        bottomStart = MasterTheme.dimensions.ContextMenuButtonRadius
    ),
    val ContextMenuButtonPrimary: Shape = RoundedCornerShape(MasterTheme.dimensions.ContextMenuButtonRadius),
    val ContextMenuButtonPrimaryStart: Shape = RoundedCornerShape(
        topStart = MasterTheme.dimensions.ContextMenuRadius,
        topEnd = MasterTheme.dimensions.ContextMenuButtonRadius,
        bottomStart = MasterTheme.dimensions.ContextMenuButtonRadius,
        bottomEnd = MasterTheme.dimensions.ContextMenuButtonRadius,
    ),
    val ContextMenuButtonPrimaryEnd: Shape = RoundedCornerShape(
        topEnd = MasterTheme.dimensions.ContextMenuRadius,
        topStart = MasterTheme.dimensions.ContextMenuButtonRadius,
        bottomEnd = MasterTheme.dimensions.ContextMenuButtonRadius,
        bottomStart = MasterTheme.dimensions.ContextMenuButtonRadius
    ),
    val ContextMenuButtonPrimaryBottom: Shape = RoundedCornerShape(
        bottomStart = MasterTheme.dimensions.ContextMenuRadius,
        topEnd = MasterTheme.dimensions.ContextMenuButtonRadius,
        topStart = MasterTheme.dimensions.ContextMenuButtonRadius,
        bottomEnd = MasterTheme.dimensions.ContextMenuButtonRadius,
    ),
    val CMBoxBottom: Shape = RoundedCornerShape(
        topStart = MasterTheme.dimensions.ContextMenuRadius,
        topEnd = MasterTheme.dimensions.ContextMenuRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    ),
    val CMBoxEnd: Shape = RoundedCornerShape(
        topEnd = 0.dp,
        topStart = MasterTheme.dimensions.ContextMenuRadius,
        bottomStart = MasterTheme.dimensions.ContextMenuRadius,
        bottomEnd = 0.dp
    ),
    val Drawer: Shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp),
    val LandingButtonShape: Shape = RoundedCornerShape(MasterTheme.dimensions.LandingButtonCornerRadius),
    val LandingButtonShapeNeedsSF: Shape = RoundedCornerShape(MasterTheme.dimensions.LandingButtonCornerRadius, 0.dp, 0.dp, MasterTheme.dimensions.LandingButtonCornerRadius),
    val NumberInputDialog: Shape = Container,
    val ProjectCardNotes: Shape = RoundedCornerShape(4.dp),

    val RadioMenuStart: Shape = RoundedCornerShape(50F, 0F, 0F, 50F),
    val RadioMenuMiddle: Shape = RectangleShape,
    val RadioMenuEnd: Shape = RoundedCornerShape(0F, 50F, 50F, 0F),

    val SectionButtonEnd: Shape = RoundedCornerShape(0.dp, 50.dp, 50.dp, 0.dp),
    val SectionButtonCenter: Shape = RectangleShape,
    val SectionButtonStart: Shape = RoundedCornerShape(50.dp, 0.dp, 0.dp, 50.dp),
    val SettingsBox: Shape = RoundedCornerShape((MasterTheme.dimensions.SettingsBoxPadding.pow(2) * 2).sqrt()),
    val SortableMenuBox: Shape = RoundedCornerShape(MasterTheme.dimensions.SortableMenuBoxRadius),
    val SoundFontWarningBox: Shape = RoundedCornerShape(24.dp),
    val TaggedBeat: Shape = RoundedCornerShape(6.dp),
    val TopBar: Shape = RectangleShape,
    val TuningDialogBox: Shape = Container
)