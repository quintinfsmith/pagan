package com.qfs.pagan.ui.theme

import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

object Shadows {
    val Button = Shadow(
        radius = 1.dp,
        spread = 0.dp,
        alpha = .3f,
        offset = DpOffset(2.dp, 2.dp),
    )
    val Drawer = Shadow(
        radius = 1.dp,
        spread = 0.dp,
        alpha = .3f,
        offset = DpOffset(2.dp, 2.dp),
    )
    val ContextMenu = Drawer
}