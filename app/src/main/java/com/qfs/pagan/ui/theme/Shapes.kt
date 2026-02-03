package com.qfs.pagan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ui.theme.Dimensions.ContextMenuButtonRadius
import com.qfs.pagan.ui.theme.Dimensions.getter

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

    val ConfigChannelButtonEnd = RoundedCornerShape(
        0.dp,
        Dimensions.ConfigDrawerButtonRadius,
        Dimensions.ConfigDrawerButtonRadius,
        0.dp,
    )
    val ConfigChannelButtonStart = RoundedCornerShape(
        Dimensions.ConfigDrawerButtonRadius,
        0.dp,
        0.dp,
        Dimensions.ConfigDrawerButtonRadius,
    )
    val Container = RoundedCornerShape(12.dp)
    val ContextMenuButtonFull = RoundedCornerShape(
        topEnd = Dimensions.ContextMenuRadius,
        topStart = Dimensions.ContextMenuRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius
    )
    val ContextMenuButtonPrimary = RoundedCornerShape(ContextMenuButtonRadius)
    val ContextMenuButtonPrimaryStart = RoundedCornerShape(
        topStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
    )
    val ContextMenuButtonPrimaryEnd = RoundedCornerShape(
        topEnd = Dimensions.ContextMenuRadius,
        topStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
        bottomStart = Dimensions.ContextMenuButtonRadius
    )
    val ContextMenuButtonPrimaryBottom = RoundedCornerShape(
        bottomStart = Dimensions.ContextMenuRadius,
        topEnd = Dimensions.ContextMenuButtonRadius,
        topStart = Dimensions.ContextMenuButtonRadius,
        bottomEnd = Dimensions.ContextMenuButtonRadius,
    )

    val ContextMenuSecondaryButtonEnd: Shape
        get() = getter(
            small_portrait = ContextMenuButtonPrimary,
            small_landscape = ContextMenuButtonPrimaryEnd,
            medium_portrait = ContextMenuButtonPrimary,
        )
    val ContextMenuSecondaryButtonStart: Shape
        get() = getter(
            small_portrait = ContextMenuButtonPrimary,
            small_landscape = ContextMenuButtonPrimaryStart,
            medium_portrait = ContextMenuButtonPrimary,
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

    val LandingButtonShape = RoundedCornerShape(Dimensions.LandingButtonCornerRadius)

    val SectionButtonEnd = RoundedCornerShape(0.dp, 50.dp, 50.dp, 0.dp)
    val SectionButtonCenter = RectangleShape
    val SectionButtonStart = RoundedCornerShape(
        50.dp,
        0.dp,
        0.dp,
        50.dp
    )

    val TaggedBeat = RoundedCornerShape(12.dp)
    val TopBar = RectangleShape
}