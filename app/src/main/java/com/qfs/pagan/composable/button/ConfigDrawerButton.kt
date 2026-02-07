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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes

@Composable
fun ConfigDrawerTopButton(modifier: Modifier = Modifier, content: (@Composable RowScope.() -> Unit), onClick: () -> Unit) {
    Button(
        modifier = modifier.height(Dimensions.ConfigChannelButtonHeight),
        shape = RoundedCornerShape(Dimensions.ConfigDrawerButtonRadius),
        contentPadding = Dimensions.ConfigButtonPadding,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun ConfigDrawerBottomButton(modifier: Modifier = Modifier, icon: Int, description: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        modifier = modifier
            .height(Dimensions.ConfigBottomButtonHeight)
            .widthIn(Dimensions.ConfigBottomButtonWidth),
        shape = RoundedCornerShape(Dimensions.ConfigDrawerButtonRadius),
        contentPadding = Dimensions.ConfigButtonPadding,
        onClick = onClick,
        enabled = enabled,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description)
            )
        }
    )
}

@Composable
fun ConfigDrawerChannelLeftButton(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(Dimensions.ConfigChannelButtonHeight),
        contentPadding = PaddingValues(
            top = Dimensions.ConfigDrawerButtonPadding,
            start = Dimensions.ConfigDrawerButtonPadding,
            bottom = Dimensions.ConfigDrawerButtonPadding,
            end = Dimensions.ConfigDrawerButtonExtraPadding
        ),
        shape = Shapes.ConfigChannelButtonStart,
        colors = colors,
        onClick = onClick,
        content = content
    )
}
@Composable
fun ConfigDrawerChannelRightButton(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit)
{
    Button(
        contentPadding = PaddingValues(
            top = Dimensions.ConfigDrawerButtonPadding,
            start = Dimensions.ConfigDrawerButtonExtraPadding,
            bottom = Dimensions.ConfigDrawerButtonPadding,
            end = Dimensions.ConfigDrawerButtonPadding
        ),
        shape = Shapes.ConfigChannelButtonEnd,
        modifier = modifier.height(Dimensions.ConfigChannelButtonHeight),
        colors = colors,
        onClick = onClick,
        content = content
    )
}
