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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {

    val text_color = if (selected) Colors.active_color_scheme.menu_item_foreground_selected
        else Colors.active_color_scheme.menu_item_foreground

    DropdownMenuItem(
        {
            ProvideTextStyle(Typography.DropdownMenu) {
                text()
            }
        },
        onClick,
        Modifier
            .padding(4.dp)
            .background(
                if (selected) Colors.active_color_scheme.menu_item_selected
                else Colors.active_color_scheme.menu_item,
                Shapes.SortableMenuBox
            ),
        leadingIcon,
        trailingIcon,
        enabled,
        MenuDefaults.itemColors().copy(
            textColor =  text_color,
            trailingIconColor = text_color,
            leadingIconColor = text_color,
        ),
        contentPadding,
        interactionSource
    )
}