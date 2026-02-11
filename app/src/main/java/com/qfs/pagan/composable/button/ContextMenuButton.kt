/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography


@Composable
fun IconCMenuButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: Int,
    description: Int,
    enabled: Boolean = true,
    shape: Shape = Shapes.ContextMenuButtonPrimary,
    contentPadding: PaddingValues = Dimensions.ContextMenuButtonPadding
) {
    Button(
        enabled = enabled,
        modifier = modifier
            .height(Dimensions.ContextMenuButtonHeight)
            .width(Dimensions.ContextMenuButtonWidth),
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        contentPadding = contentPadding,
        shape = shape,
        content = {
            Icon(
                modifier = Modifier.width(Dimensions.ContextMenuButtonIconWidth),
                painter = painterResource(icon),
                contentDescription = stringResource(description),
            )
        }
    )
}

@Composable
fun TextCMenuButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit) ?= null,
    text: String,
    shape: Shape = Shapes.ContextMenuButtonPrimary,
    enabled: Boolean = true,
    contentPadding: PaddingValues = Dimensions.ContextMenuButtonPadding
) {
    Button(
        enabled = enabled,
        modifier = modifier.height(Dimensions.ContextMenuButtonHeight),
        contentPadding = contentPadding,
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        content = {
            Text(
                text = text,
                maxLines = 1,
                style = Typography.ContextMenuButton,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
}
