package com.qfs.pagan.composable.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun IconCMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, icon: Int, description: Int) {
    BetterButton(
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick ?: {},
        contentPadding = PaddingValues(8.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
        )
    }
}

@Composable
fun TextCMenuButton(modifier: Modifier = Modifier, onClick: () -> Unit, onLongClick: (() -> Unit) ?= null, text: String) {
    BetterButton(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        onClick = onClick,
        onLongClick = onLongClick ?: {}
    ) {
        Text(text = text)
    }
}
