package com.qfs.pagan.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

fun CMBoxBottomShape(): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
}

fun CMBoxEndShape(): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 4.dp,
        bottomStart = 0.dp,
        bottomEnd = 4.dp
    )
}