package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ContextMenuButtonPadding

@Composable
fun ContextMenuButtonPadding(): PaddingValues {
    return PaddingValues(
        horizontal = dimensionResource(R.dimen.contextmenu_button_padding_h),
        vertical = dimensionResource(R.dimen.contextmenu_button_padding_v)
    )
}

@Composable
fun ContextMenuButtonShape(): RoundedCornerShape {
    return RoundedCornerShape(dimensionResource(R.dimen.contextmenu_button_radius))
}

@Composable
fun IconCMenuButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: Int,
    description: Int,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ContextMenuButtonPadding()
) {
    Button(
        enabled = enabled,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        contentPadding = contentPadding,
        shape = ContextMenuButtonShape(),
        content = {
            Icon(
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
    enabled: Boolean = true,
    contentPadding: PaddingValues = ContextMenuButtonPadding()
) {
    Button(
        enabled = enabled,
        modifier = modifier,
        contentPadding = contentPadding,
        shape = ContextMenuButtonShape(),
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        content = { Text(text = text) }
    )
}
