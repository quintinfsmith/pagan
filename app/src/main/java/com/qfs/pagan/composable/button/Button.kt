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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.qfs.pagan.composable.pressable
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.ui.theme.Shadows
import com.qfs.pagan.ui.theme.Typography

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    border: BorderStroke = ButtonDefaults.outlinedButtonBorder(),

    shape: Shape = ButtonDefaults.shape,
    outerPadding: PaddingValues = Unpadded,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(),
        shape = shape,
        border = border,
        outerPadding = outerPadding,
        contentPadding = contentPadding,
        shadow = null,
        content = content
    )
}

@Composable
fun Button(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,

    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,
    shadow: Shadow? = Shadows.Button,
    outerPadding: PaddingValues = Unpadded,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val pressed = remember { mutableStateOf(false) }
    ProvideContentColorTextStyle(contentColor = colors.contentColor, textStyle = Typography.Button) {
        Box(
            modifier = modifier
                .pressable(pressed)
                .then(
                    if (shadow == null || pressed.value || !enabled) {
                        Modifier
                    } else {
                        Modifier.dropShadow(
                            shape = shape,
                            shadow = shadow
                        )
                    }
                )
                .padding(outerPadding)
                .clip(shape)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .then(
                    if (enabled) {
                        Modifier.combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongClick,
                        )
                        .background(color = colors.containerColor, shape)
                    } else {
                        Modifier.background(color = colors.disabledContentColor, shape)
                    }
                )
                //.minimumInteractiveComponentSize()
                .semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
internal fun ProvideContentColorTextStyle(
    contentColor: Color,
    textStyle: TextStyle = TextStyle.Default,
    content: @Composable () -> Unit,
) {
    val mergedStyle = LocalTextStyle.current.merge(textStyle)
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides mergedStyle,
        content = content,
    )
}

@Composable
fun SmallButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,

    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    border: BorderStroke? = null,

    shadow: Shadow? = Shadows.Button,
    outerPadding: PaddingValues = Unpadded,
    contentPadding: PaddingValues = Unpadded,
    content: @Composable RowScope.() -> Unit
) {
    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
        Button(
            onClick= onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            colors = colors,
            border = border,
            shadow = shadow,
            outerPadding = outerPadding,
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
fun SmallOutlinedButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    border: BorderStroke = ButtonDefaults.outlinedButtonBorder(),
    outerPadding: PaddingValues = Unpadded,
    contentPadding: PaddingValues = Unpadded,
    content: @Composable RowScope.() -> Unit
) {
    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
        OutlinedButton(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
            enabled = enabled,
            border = border,
            outerPadding = outerPadding,
            contentPadding = contentPadding,
            content = content
        )
    }
}
