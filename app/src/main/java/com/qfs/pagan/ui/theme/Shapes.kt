package com.qfs.pagan.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.qfs.pagan.ui.theme.Dimensions.ContextMenuButtonPadding
import com.qfs.pagan.ui.theme.Dimensions.ContextMenuButtonRadius

object Shapes {
    val NumberSelectorButton = RoundedCornerShape(Dimensions.NumberSelectorButtonRadius)
    val NumberSelectorButtonStart = RoundedCornerShape(
        topStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.NumberSelectorButtonRadius,
        bottomStart = Dimensions.NumberSelectorButtonRadius,
        bottomEnd = Dimensions.NumberSelectorButtonRadius,
    )
    val NumberSelectorButtonEnd = RoundedCornerShape(
        topStart = Dimensions.NumberSelectorButtonRadius,
        topEnd = Dimensions.ContextMenuRadius,
        bottomStart = Dimensions.NumberSelectorButtonRadius,
        bottomEnd = Dimensions.NumberSelectorButtonRadius,
    )
    val ContextMenuButtonPrimary = CircleShape
    val ContextMenuButtonSecondary = RoundedCornerShape(ContextMenuButtonRadius)
    val ContextMenuButtonSecondaryStart = RoundedCornerShape(
        topStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
    )
    val ContextMenuButtonSecondaryEnd = RoundedCornerShape(
        topEnd = Dimensions.ContextMenuRadius,
        topStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius
    )
    val ContextMenuButtonSecondaryBottom = RoundedCornerShape(
        bottomStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuButtonRadius,
        topStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
    )

    val CMBoxBottom = RoundedCornerShape(
        topStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuRadius,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    val CMBoxEnd = RoundedCornerShape(
        topEnd = 0.dp,
        topStart = Dimensions.ContextMenuRadius,
        bottomStart = Dimensions.ContextMenuRadius,
        bottomEnd = 0.dp
    )

    val Drawer = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp)

    val TaggedBeat = RoundedCornerShape(12.dp)

    val SectionButtonEnd = RoundedCornerShape(0.dp, 50.dp, 50.dp, 0.dp)
    val SectionButtonCenter = RectangleShape
    val SectionButtonStart = RoundedCornerShape(50.dp, 0.dp, 0.dp, 50.dp)
}