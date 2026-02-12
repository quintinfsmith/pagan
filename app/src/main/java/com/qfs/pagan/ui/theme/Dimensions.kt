/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.ui.theme

import androidx.compose.foundation.layout.PaddingValues
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
    var active_layout_width = 0.dp
    var active_layout_height = 0.dp
    var active_layout_size = VLayoutSize.SmallPortrait

    fun set_active_layout_dimensions(width: Dp, height: Dp): VLayoutSize {
        Dimensions.active_layout_height = height
        Dimensions.active_layout_width = width
        this.active_layout_size = if (width >= height) {
            if (height >= Layout.XLarge.short) LayoutSize.XLargeLandscape
            else if (height >= Layout.Large.short) LayoutSize.LargeLandscape
            else if (height >= Layout.Medium.short) LayoutSize.MediumLandscape
            else LayoutSize.SmallLandscape
        } else {
            if (width >= Layout.XLarge.short) LayoutSize.XLargePortrait
            else if (width >= Layout.Large.short) LayoutSize.LargePortrait
            else if (width >= Layout.Medium.short) LayoutSize.MediumPortrait
            else LayoutSize.SmallPortrait
        }
        return this.active_layout_size
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
            val short = 361.dp
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

        val active_size = this.active_layout_size

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

    ///////////////////////////////////////////////////////////////////////

    val AboutPadding = 12.dp
    val AboutUrlSectionPadding = 6.dp

    object Background {
        val Radius = 12.dp
        val Gap = 20.dp
        val BarWidth = LeafBaseWidth * 2F
        val BarSmallHeight = BarWidth
        val BarLargeHeight = BarSmallHeight * 3
    }

    val BeatLabelPadding = PaddingValues(
        horizontal = 4.dp,
        vertical = 8.dp
    )

    object ButtonHeight {
        val Normal = 48.dp
        val Small = 41.dp
    }

    val BugReportPadding = 24.dp

    val ChannelGapHeight = 5.dp

    val ColorPickerHexInputWidth = 42.dp
    val ColorPickerLabelWidth = 64.dp
    val ColorPickerInnerPadding = 8.dp
    val ColorPickerPreviewHeight = 48.dp
    val ColorPickerSliderWidth = Layout.Small.short * .6F

    val ConfigButtonPadding = PaddingValues(
        horizontal = 16.dp,
        vertical = 8.dp
    )

    val ConfigChannelButtonHeight = 41.dp
    val ConfigBottomButtonHeight = 48.dp
    val ConfigBottomButtonWidth = 72.dp
    val ConfigChannelSpacing = 4.dp

    val ConfigDrawerPadding = 4.dp
    val ConfigDrawerButtonPadding = 8.dp
    val ConfigDrawerButtonExtraPadding = 16.dp
    val ConfigDrawerButtonRadius = 12.dp
    val ConfigDrawerChannelLabelPadding = 12.dp

    val ContextMenuButtonHeight: Dp
        get() = getter(
            small_portrait = 48.dp,
            small_landscape = 41.dp,
            medium_portrait = 48.dp,
            medium_landscape = 48.dp,
        )
    val ContextMenuButtonWidth: Dp
        get() = getter(
            small_portrait = 48.dp,
        )
    val ContextMenuButtonIconWidth = 32.dp

    val ContextMenuButtonPadding = PaddingValues(8.dp)
    val ContextMenuButtonRadius = 8.dp
    val ContextMenuPadding = Space.Medium
    val ContextMenuRadius = 16.dp

    object DashedBorder {
        val Width = Stroke.Medium
        val Dash = 4.dp
        val Gap = 4.dp
    }

    val DialogAdjustInnerSpace = 64.dp
    val DialogBarButtonHeight = 41.dp
    val DialogBarPaddingVertical = 8.dp
    val DialogBarSpacing = 12.dp
    val DialogPadding = 16.dp
    val DialogLineHeight = 41.dp
    val DialogLinePadding = PaddingValues(vertical = 5.dp, horizontal = 12.dp)
    val DialogTitlePadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp)
    val DialogRadius = 12.dp

    val DivisorPadding = 4.dp

    val EffectLineHeight = 30.dp
    val EffectDialogIconWidth = 32.dp
    val EffectTransitionDialogIconHeight = 32.dp

    val ExtraTableIconsPadding = 6.dp

    val HexDisplayStrokeWidth = Stroke.Thin
    val HexDisplayHeight = 48.dp

    val NumberInputPadding = PaddingValues(12.dp)

    object EffectWidget {
        object Pan {
            val CenterDotDiameter = 12.dp
        }
        object Delay {
            val FadePopupHeight = 250.dp
            val FadePopupWidth = 50.dp
            val FadePopupPadding = 8.dp
            val InputWidth = 54.dp
            val InputIconWidth = 41.dp
        }
        object Tempo {}
        val InputHeight = 41.dp
    }

    val LandingButtonCornerRadius = 12.dp
    val LandingButtonHeight: Dp
        get() = getter(
            small_landscape = 41.dp,
            small_portrait = 48.dp,
            medium_portrait = 48.dp
        )
    val LandingPadding: Dp
        get() = getter(
            small_portrait = 8.dp,
        )
    val LandingIconButtonSize: Dp
        get() = getter(
            small_landscape = 32.dp,
            small_portrait = 41.dp,
            medium_portrait = 48.dp,
            large_landscape = 54.dp
        )
    val LandingIconButtonPadding: Dp
        get() = getter(
            small_landscape = 8.dp,
            small_portrait = 8.dp,
            medium_portrait = 8.dp,
            large_portrait = 12.dp
        )

    val LeafBaseWidth = 41.dp
    val LinkUrlPadding = 4.dp

    val LineHeight = 41.dp
    val LineLabelWidth = 41.dp
    val LineLabelPadding = 4.dp
    val LineLabelIconPadding = 4.dp

    val SelectionBorderPadding = PaddingValues(
        top = 2.dp,
        bottom = 2.dp,
        end = 3.dp,
        start = 2.dp
    )

    val NumberInputDialogPadding = 8.dp
    val NumberPickerRowHeight = 41.dp
    val NumberPickerRowWidth = 80.dp
    val NumberPickerStrokeWidth = Stroke.Thin
    val NumberSelectorButtonHeight: Dp
        get() = getter(
            small_portrait = 41.dp,
            medium_portrait = 48.dp,
        )

    val NumberSelectorButtonRadius = 4.dp
    val NumberSelectorColumnWidth = 41.dp
    val NumberSelectorSpacing = 3.dp

    val OutlineButtonStrokeWidth = Stroke.Thin

    val ProjectCardNotesPadding = 6.dp

    val PercussionSwitchIconPadding = 4.dp

    object RadioMenu {
        val Gap = 4.dp
        val StrokeWidth = Stroke.Medium
    }

    object RelativeInputPopup {
        val Padding = 8.dp
        val ItemPadding = 4.dp
        val ItemHeight = 20.dp
    }

    val SettingsRadioIconHeight = 32.dp
    val SettingsBoxPadding = 12.dp

    val ShortcutIconPadding = 7.dp

    val SortableMenuSortButtonDiameter = 36.dp
    val SortableMenuSortButtonPadding = PaddingValues(6.dp)
    val SortableMenuHeadSpacing = 4.dp
    val SortableMenuBoxRadius = 8.dp
    val SortableMenuLineGap = 4.dp

    object Space {
        val Small = 2.dp
        val Medium = 4.dp
        val Large = 8.dp
    }

    val SoundFontMenuPadding = 12.dp
    val SoundFontMenuInnerPadding = 8.dp
    val SoundFontMenuIconHeight = 41.dp
    val SFWarningInnerPadding = 10.dp
    val SoundFontWarningBorderWidth = 4.dp
    val SoundFontWarningPadding = 16.dp

    object Stroke {
        val Thin = 1.dp
        val Medium = 2.dp
    }

    val TableLineStroke = Stroke.Thin

    val TinyTuningDialogButtonSize = 41.dp
    val TinyTuningDialogButtonPadding = 4.dp
    val TinyTuningDialogInnerPadding = 6.dp
    val TinyTuningDialogInputWidth = 64.dp

    val TopBarHeight: Dp
        get() = getter(
            small_landscape = 41.dp,
            small_portrait = 48.dp,
            medium_portrait = 48.dp,
            medium_landscape = 54.dp,
            large_portrait = 54.dp,
            large_landscape = 54.dp,
        )
    val TopBarIconHeight = 32.dp
    val TopBarIconWidth = 54.dp
    val TopBarItemSpace = 8.dp

    val TransposeDialogInnerPadding = 8.dp
    val TransposeDialogInputPadding = 8.dp
    val TransposeDialogInputWidth = 64.dp

    val TuningDialogStrokeWidth = Stroke.Thin
    val TuningDialogBoxPadding = 6.dp
    val TuningDialogLinePadding = 4.dp

    val Unpadded = PaddingValues(0.dp)

}