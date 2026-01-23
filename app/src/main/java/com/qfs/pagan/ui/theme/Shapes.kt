package com.qfs.pagan.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

object Shapes {
    val NumberSelectorButton = RectangleShape
    val ContextMenuButtonPrimary = CircleShape
    val ContextMenuButtonSecondary = RectangleShape

    val CMBoxBottom = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val CMBoxEnd = RoundedCornerShape(
        topEnd = 0.dp,
        topStart = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 0.dp
    )

    val Drawer = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)

    val TaggedBeat = RoundedCornerShape(12.dp)

    val SectionButtonEnd = RoundedCornerShape(0.dp, 50.dp, 50.dp, 0.dp)
    val SectionButtonCenter = RectangleShape
    val SectionButtonStart = RoundedCornerShape(50.dp, 0.dp, 0.dp, 50.dp)
}