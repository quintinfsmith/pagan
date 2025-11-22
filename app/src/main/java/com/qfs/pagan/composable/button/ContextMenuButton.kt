package com.qfs.pagan.composable.button

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.qfs.pagan.R

@Composable
fun ContextMenuButton(modifier: Modifier = Modifier, onClick = () -> Unit, onLongClick = () -> Unit, content = @Composable () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
        modifier = modifier
            .padding(3.dp)
            .width(dimensionResource(R.dimen.icon_button_width))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        content = content
    )
}

@Composable
fun IconCMenuButton(modifier: Modifier = Modifier, onClick = () -> Unit, onLongClick = (() -> Unit)? = null, icon = Int, description = Int) {
    ContextMenuButton(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description)
            )
        }   
    )
}

@Composable
fun TextCMenuButton(modifier: Modifier, onClick = () -> Unit, onLongClick = (() -> Unit) ?= null, text: String) {
    ContextMenuButton(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        content = { Text(text) }
    )
}
