package com.qfs.pagan.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qfs.pagan.LayoutSize
import com.qfs.pagan.LayoutSize.LargeLandscape
import com.qfs.pagan.LayoutSize.LargePortrait
import com.qfs.pagan.LayoutSize.MediumLandscape
import com.qfs.pagan.LayoutSize.MediumPortrait
import com.qfs.pagan.LayoutSize.SmallLandscape
import com.qfs.pagan.LayoutSize.SmallPortrait
import com.qfs.pagan.LayoutSize.XLargeLandscape
import com.qfs.pagan.LayoutSize.XLargePortrait
import com.qfs.pagan.LayoutSize as VLayoutSize

object Dimensions {
    var active_layout_width = mutableStateOf(0.dp)
    var active_layout_height = mutableStateOf(0.dp)
    private var active_layout_size = mutableStateOf(VLayoutSize.SmallPortrait)

    fun set_active_layout_dimensions(width: Dp, height: Dp) {
        Dimensions.active_layout_height.value = height
        Dimensions.active_layout_width.value = width
        this.active_layout_size.value = if (width >= height) {
            if (width >= Layout.XLarge.long && height >= Layout.XLarge.short) LayoutSize.XLargeLandscape
            else if (width >= Layout.Large.short && height >= Layout.Large.long) LayoutSize.LargeLandscape
            else if (width >= Layout.Medium.short && height >= Layout.Medium.long) LayoutSize.MediumLandscape
            else LayoutSize.SmallLandscape
        } else {
            if (height >= Layout.XLarge.long && width >= Layout.XLarge.short) LayoutSize.XLargePortrait
            else if (height >= Layout.Large.short && width >= Layout.Large.long) LayoutSize.LargePortrait
            else if (height >= Layout.Medium.short && width >= Layout.Medium.long) LayoutSize.MediumPortrait
            else LayoutSize.SmallPortrait
        }
    }

    object Layout {
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

    fun <T> getter(
        small_portrait: T? = null,
        small_landscape: T? = null,
        medium_portrait: T? = null,
        medium_landscape: T? = null,
        large_portrait: T? = null,
        large_landscape: T? = null,
        xlarge_portrait: T? = null,
        xlarge_landscape: T? = null
    ): T {

        val mapped_values = mapOf(
            SmallPortrait to small_portrait,
            SmallLandscape to small_landscape,
            MediumPortrait to medium_portrait,
            MediumLandscape to medium_landscape,
            LargePortrait to large_portrait,
            LargeLandscape to large_landscape,
            XLargePortrait to xlarge_portrait,
            XLargeLandscape to xlarge_landscape
        )

        val active_size = this.active_layout_size.value

        // If the value is mapped, use it
        mapped_values[active_size]?.let { return it }

        // If the value is not mapped, but the other orientation of the same size is, then use it
        when (active_size) {
            VLayoutSize.SmallPortrait -> mapped_values[SmallLandscape]?.let { return it }
            VLayoutSize.SmallLandscape -> mapped_values[SmallPortrait]?.let { return it }
            VLayoutSize.MediumPortrait -> mapped_values[MediumLandscape]?.let { return it }
            VLayoutSize.MediumLandscape -> mapped_values[MediumPortrait]?.let { return it }
            VLayoutSize.LargePortrait -> mapped_values[LargeLandscape]?.let { return it }
            VLayoutSize.LargeLandscape -> mapped_values[LargePortrait]?.let { return it }
            VLayoutSize.XLargePortrait -> mapped_values[XLargeLandscape]?.let { return it }
            VLayoutSize.XLargeLandscape -> mapped_values[XLargePortrait]?.let { return it }
        }

        // Find the next-smallest mapped value
        var actual_layout_passed = false
        var working_value: T? = null
        for (layout_size in listOf(SmallPortrait, SmallLandscape, MediumPortrait, MediumLandscape, LargePortrait, LargeLandscape, XLargePortrait, XLargeLandscape)) {
            if (actual_layout_passed && working_value != null) break
            working_value = mapped_values[layout_size] ?: working_value
            actual_layout_passed = actual_layout_passed || (layout_size == active_size)
        }
        working_value?.let { return it }

        throw Exception("Value Not Set") // TODO: Specify
    }


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

    val ContextMenuButtonHeight: Dp
        get() = getter(
            small_portrait = 41.dp,
            small_landscape = 41.dp,
            medium_portrait = 48.dp,
            medium_landscape = 48.dp,
        )

    val ContextMenuButtonPadding = PaddingValues(8.dp)
    val ContextMenuButtonRadius = 8.dp
    val ContextMenuPadding = 4.dp
    val ContextMenuRadius = 16.dp

    val DialogPadding = 16.dp
    val DialogLineHeight = 41.dp
    val DialogLinePadding = PaddingValues(vertical = 5.dp, horizontal = 12.dp)

    val LandingPadding: Dp
        get() = getter(
            small_portrait = 8.dp,
        )

    val LeafBaseWidth = 41.dp

    val NumberSelectorButtonHeight: Dp
        get() = getter(
            small_portrait = 32.dp,
            medium_portrait = 48.dp,
        )
    val NumberSelectorButtonRadius = 4.dp
    val NumberSelectorHighlightedBorderPadding = 4.dp
    val NumberSelectorSpacing = 3.dp

    val SettingsRadioIconHeight = 32.dp

    val TopBarHeight = 54.dp
    val TopBarIconSize = 32.dp
    val TopBarItemSpace = 16.dp
}