package com.qfs.pagan.composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
fun MagicButtonShape(): RoundedCornerShape {
    return RoundedCornerShape(6.dp)
}
fun CMBoxBottomShape(): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
}

fun CMBoxEndShape(): RoundedCornerShape {
    return RoundedCornerShape(
        topEnd = 0.dp,
        topStart = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 0.dp
    )
}