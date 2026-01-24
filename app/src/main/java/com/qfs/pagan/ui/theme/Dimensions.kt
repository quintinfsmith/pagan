package com.qfs.pagan.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp

object Dimensions {
    object ButtonHeight {
        val Normal = 48.dp
        val Small = 41.dp
    }

    val ConfigButtonPadding = PaddingValues(
        horizontal = 16.dp,
        vertical = 8.dp
    )

    val ConfigChannelButtonHeight = 41.dp
    val ConfigChannelBottomButtonHeight = 48.dp
    val ConfigChannelSpacing = 4.dp
    val ConfigDrawerPadding = 4.dp

    val ContextMenuButtonPadding = PaddingValues(8.dp)
    val ContextMenuButtonRadius = 8.dp
    val ContextMenuPadding = 4.dp
    val ContextMenuRadius = 16.dp

    val DialogPadding = 16.dp
    val DialogLineHeight = 41.dp
    val DialogLinePadding = PaddingValues(vertical = 5.dp, horizontal = 12.dp)

    val LandingPadding = 8.dp

    val LeafBaseWidth = 41.dp

    object LayoutSize {
        object XLarge {
            val short = 720.dp
            val long = 960.dp
        }
        object Large {
            val short = 480.dp
            val long = 640.dp
        }
        object Medium {
            val short = 320.dp
            val long = 470.dp
        }
        object Small {
            val short = 320.dp
            val long = 426.dp
        }
    }

    val NumberSelectorButtonHeight = ButtonHeight.Small
    val NumberSelectorButtonRadius = 4.dp
    val NumberSelectorHighlightedBorderPadding = 4.dp
    val NumberSelectorSpacing = 3.dp

    val SettingsRadioIconHeight = 32.dp

    val TopBarHeight = 54.dp
    val TopBarIconSize = 32.dp
    val TopBarItemSpace = 16.dp
}