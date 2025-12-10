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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,

    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(),
        shape = ButtonDefaults.outlinedShape,
        border = ButtonDefaults.outlinedButtonBorder(),
        contentPadding = contentPadding,
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

    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
   // val containerColor = colors.containerColor(enabled)
   // val shadowElevation = elevation?.shadowElevation(enabled)?.value ?: 0.dp
   // val contentColor = colors.contentColor(enabled)
    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .clip(shape)
            .padding(vertical = 2.dp, horizontal = 1.dp)
            .minimumInteractiveComponentSize(),
        content = {
            ProvideContentColorTextStyle(contentColor = colors.contentColor, textStyle = TextStyle.Default) {
                Box(
                    modifier = modifier
                        .then(if (border != null) modifier.border(border, shape) else modifier)
                        .then(
                            if (enabled) {
                                modifier.combinedClickable(
                                    onClick = onClick,
                                    onLongClick = onLongClick,
                                )
                                .background(color = colors.containerColor, shape)
                            } else {
                                modifier.background(color = colors.disabledContentColor, shape)
                            }
                        )
                        .minimumInteractiveComponentSize()
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        Modifier
                            .padding(contentPadding)
                            .defaultMinSize(
                                minWidth = ButtonDefaults.MinWidth,
                                minHeight = ButtonDefaults.MinHeight,
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content,
                    )
                }
            }
        }
    )
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
