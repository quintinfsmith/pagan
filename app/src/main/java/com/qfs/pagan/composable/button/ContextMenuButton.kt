package com.qfs.pagan.composable.button

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun ContextMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
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
fun IconCMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, icon: Int, description: Int) {
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
fun TextCMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit) ?= null, text: String) {
    ContextMenuButton(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        content = { Text(text) }
    )
}
