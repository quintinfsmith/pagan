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
                modifier = Modifier.width(32.dp),
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
