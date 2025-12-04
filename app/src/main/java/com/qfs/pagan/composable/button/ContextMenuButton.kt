package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.height
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
fun IconCMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, icon: Int, description: Int, enabled: Boolean = true, contentPadding: PaddingValues = PaddingValues(8.dp)) {
    BetterButton(
        enabled = enabled,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        contentPadding = contentPadding,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
            )
        }
    )
}

@Composable
fun TextCMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit) ?= null, text: String, enabled: Boolean = true, contentPadding: PaddingValues = PaddingValues(8.dp)) {
    BetterButton(
        enabled = enabled,
        modifier = modifier,
        contentPadding = contentPadding,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        content = { Text(text = text) }
    )
}
