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
import com.qfs.pagan.ui.theme.Dimensioddns.Stroke

data class PaganDimensions(
    val LeafBaseWidth: Sized<Dp> = Sized(41.dp),
    val SpaceSmall: Sized<Dp> = Sized(2.dp),
    val SpaceMedium: Sized<Dp> = Sized(4.dp),
    val SpaceLarge: Sized<Dp> = Sized(8.dp),
    val StrokeThin: Sized<Dp> = Sized(1.dp),
    val StrokeMedium: Sized<Dp> = Sized(2.dp),
    val StrokeThick: Sized<Dp> = Sized(4.dp),
    val StrokeXThick: Sized<Dp> = Sized(8.dp),

    val AboutPadding: Sized<Dp> = Sized(12.dp),
    val AboutUrlSectionPadding: Sized<Dp> = Sized(6.dp),
    val BackGroundRadius: Sized<Dp> = Sized(12.dp),
    val BackGroundGap: Sized<Dp> = Sized(20.dp),
    val BackGroundBarWidth: Sized<Dp> = LeafBaseWidth * 2F,
    val BackGroundBarSmallHeight: Sized<Dp> = BackGroundBarWidth,
    val BackGroundBarLargeHeight: Sized<Dp> = BackGroundBarSmallHeight * 3,

    val BeatLabelHorizontalPadding: Sized<Dp> = Sized(3.dp),
    val BeatLabelVerticalPadding: Sized<Dp> = Sized(8.dp),
    val ButtonHeightNormal: Sized<Dp> = Sized(48.dp),
    val ButtonHeightSmall: Sized<Dp> = Sized(41.dp),
    val BugReportPadding: Sized<Dp> = Sized(24.dp),
    val ChannelGapHeight: Sized<Dp> = Sized(5.dp),
    val ColorPickerHexInputWidth: Sized<Dp> = Sized(42.dp),
    val ColorPickerLabelWidth: Sized<Dp> = Sized(64.dp),
    val ColorPickerInnerPadding: Sized<Dp> = Sized(8.dp),
    val ColorPickerPreviewHeight: Sized<Dp> = Sized(48.dp),
    val ColorPickerSliderWidth: Sized<Dp> = Sized(Layout.Small.short * .6F),

    val ConfigButtonPadding: Sized<PaddingValues> = Sized(PaddingValues(16.dp, 8.dp)),

    val ConfigChannelButtonHeight: Sized<Dp> = Sized(41.dp),
    val ConfigBottomButtonHeight: Sized<Dp> = Sized(48.dp),
    val ConfigBottomButtonWidth: Sized<Dp> = Sized(72.dp),
    val ConfigChannelSpacing: Sized<Dp> = Sized(4.dp),

    val ConfigDrawerPadding: Sized<Dp> = Sized(4.dp),
    val ConfigDrawerButtonPadding: Sized<Dp> = Sized(8.dp),
    val ConfigDrawerButtonExtraPadding: Sized<Dp> = Sized(16.dp),
    val ConfigDrawerButtonRadius: Sized<Dp> = Sized(12.dp),
    val ConfigDrawerChannelLabelPadding: Sized<Dp> = Sized(12.dp),


    val ContextMenuButtonHeight: Sized<Dp> = Sized(
        SmallPortrait to 48.dp,
        SmallLandscape to 41.dp,
        MediumPortrait to 48.dp,
        MediumLandscape to 48.dp
    ),

    val ContextMenuButtonWidth: Sized<Dp> = Sized(48.dp),
    val ContextMenuButtonIconWidth: Sized<Dp> = Sized(32.dp),

    val ContextMenuButtonPadding: Sized<PaddingValues> = Sized(
        SmallPortrait to PaddingValues(8.dp),
        SmallLandscape to PaddingValues(6.dp),
        MediumPortrait to PaddingValues(8.dp)
    ),
    val ContextMenuButtonRadius: Sized<Dp> = Sized(8.dp),
    val ContextMenuPadding: Sized<Dp> = SpaceMedium,
    val ContextMenuSpacing: Sized<Dp> = Sized(
        SmallPortrait to SpaceMedium[SmallPortrait],
        SmallLandscape to SpaceSmall[SmallLandscape],
        MediumPortrait to SpaceMedium[SmallPortrait]
    ),
    val ContextMenuRadius: Sized<Dp> = Sized(16.dp),

    val DashedBorderWidth: Sized<Dp> = StrokeMedium,
    val DashedBorderDash: Sized<Dp> = Sized(4.dp),
    val DashedBorderGap: Sized<Dp> = Sized(4.dp),

    val DialogAdjustInnerSpace: Sized<Dp> = Sized(64.dp),
    val DialogBarButtonHeight: Sized<Dp> = Sized(41.dp),
    val DialogBarPaddingVertical: Sized<Dp> = Sized(8.dp),
    val DialogBarSpacing: Sized<Dp> = Sized(12.dp),
    val DialogPadding: Sized<Dp> = Sized(16.dp),
    val DialogLineHeight: Sized<Dp> = Sized(41.dp),
    val DialogLinePadding: Sized<PaddingValues> = Sized(PaddingValues(5.dp, 12.dp)),
    val DialogTitlePadding: Sized<PaddingValues> = Sized(PaddingValues(16.dp, 12.dp)),
    val DialogRadius: Sized<Dp> = Sized(12.dp),

    val DivisorPadding: Sized<Dp> = Sized(4.dp),

    val EffectLineHeight: Sized<Dp> = Sized(30.dp),
    val EffectDialogIconWidth: Sized<Dp> = Sized(32.dp),
    val EffectDialogIconHeight: Sized<Dp> = Sized(32.dp),
    val EffectTransitionDialogIconHeight: Sized<Dp> = Sized(32.dp),

    val ExtraTableIconsPadding: Sized<Dp> = Sized(6.dp),

    val HexDisplayHeight: Sized<Dp> = Sized(48.dp),
    val HexDisplayStrokeWidth: Sized<Dp> = StrokeThin,

    val KnobPadding: Sized<PaddingValues> = Sized(PaddingValues(4.dp)),

    val NumberInputPadding: Sized<PaddingValues> = Sized(PaddingValues(12.dp)),

    val EffectWidgetPanCenterDotDiameter: Sized<Dp> = Sized(12.dp),
    val EffectWidgetPanSliderPadding: Sized<Dp> = ContextMenuButtonWidth,
    val EffectWidgetVelocityFadePopupHeight: Sized<Dp> = Sized(250.dp),
    val EffectWidgetVelocityFadePopupWidth: Sized<Dp> = Sized(50.dp),
    val EffectWidgetVelocityFadePopupPadding: Sized<Dp> = Sized(8.dp),
    val EffectWidgetVelocityInputWidth: Sized<Dp> = Sized(54.dp),
    val EffectWidgetVelocityInputIconWidth: Sized<Dp> = Sized(41.dp),
    val EffectWidgetDelayFadePopupHeight: Sized<Dp> = Sized(250.dp),
    val EffectWidgetDelayFadePopupWidth: Sized<Dp> = Sized(50.dp),
    val EffectWidgetDelayFadePopupPadding: Sized<Dp> = Sized(8.dp),
    val EffectWidgetDelayInputWidth: Sized<Dp> = Sized(54.dp),
    val EffectWidgetDelayInputIconWidth: Sized<Dp> = Sized(41.dp),
    val EffectWidgetInputHeight: Sized<Dp> = Sized(41.dp),

    val LandingButtonCornerRadius: Sized<Dp> = Sized(12.dp),
    val LandingButtonHeight: Sized<Dp> = Sized(
        SmallLandscape to 56.dp,
        SmallPortrait to 64.dp,
        MediumLandscape to 64.dp,
        MediumPortrait to 128.dp,
        LargePortrait to 128.dp,
        LargeLandscape to 96.dp,
        XLargePortrait to 128.dp
    ),

    val LandingPadding: Sized<Dp> = Sized(8.dp),
    val LandingIconButtonSize: Sized<Dp> = Sized(
        SmallLandscape to 41.dp,
        SmallPortrait to 41.dp,
        MediumPortrait to 48.dp,
        LargeLandscape to 64.dp
    ),
    val LandingIconButtonPadding: Sized<Dp> = Sized(
        SmallLandscape to 8.dp,
        SmallPortrait to 8.dp,
        MediumPortrait to 8.dp,
        LargePortrait to 12.dp
    ),

    val LinkUrlPadding: Sized<Dp> = Sized(4.dp),

    val LineHeight: Sized<Dp> = Sized(41.dp),
    val LineLabelWidth: Sized<Dp> = Sized(41.dp),
    val LineLabelPadding: Sized<PaddingValues> = Sized(PaddingValues(4.dp, 2.dp)),
    val LineCtlLabelPadding: Sized<PaddingValues> = Sized(PaddingValues(3.dp, 0.dp)),
    val LineLabelIconPaddingGlobal: Sized<PaddingValues> = Sized(PaddingValues(2.dp)),
    val LineLabelIconPaddingLine: Sized<PaddingValues> = Sized(PaddingValues(
        bottom = 2.dp,
        top = 2.dp,
        start = 1.dp,
        end = 7.dp,
    )),
    val LineLabelIconPaddingChannel: Sized<PaddingValues> = Sized(PaddingValues(
        bottom = 2.dp,
        top = 2.dp,
        start = 6.dp,
        end = 2.dp,
    )),

    val SelectionBorderPadding: Sized<PaddingValues> = Sized(PaddingValues(1.5.dp)),

    val NumberInputDialogPadding: Sized<PaddingValues> = Sized(PaddingValues(8.dp, 4.dp)),
    val FloatInputDialogWidth: Sized<Dp> = Sized(200.dp),
    val NumberInputDialogWidth: Sized<Dp> = Sized(128.dp),
    val NumberPickerRowHeight: Sized<Dp> = Sized(41.dp),
    val NumberPickerRowWidth: Sized<Dp> = Sized(80.dp),
    val NumberPickerStrokeWidth: Sized<Dp> = StrokeThin,

    val NotePickerButtonHeight: Sized<Dp> = Sized(
        SmallPortrait to 41.dp,
        MediumPortrait to 48.dp,
    ),
    val NotePickerButtonRadius: Sized<Dp> = Sized(4.dp),
    val NotePickerColumnWidth: Sized<Dp> = Sized(41.dp),
    val NotePickerSpacing: Sized<Dp> = Sized(3.dp),

    val OutlineButtonStrokeWidth: Sized<Dp> = StrokeThin,

    val PaletteDotPaddingEnd: Sized<Dp> = Sized(4.dp),
    val PaletteDotPaddingBottom: Sized<Dp> = Sized(6.dp),
    val PaletteDotSize: Sized<Dp> = Sized(12.dp),

    val PercussionSwitchIconPadding: Sized<Dp> = Sized(4.dp),
    val PresetMenuArrowWidth: Sized<Dp> = Sized(24.dp),
    val PresetMenuArrowHeight: Sized<Dp> = Sized(24.dp),
    val PreviewIconHeight: Sized<Dp> = Sized(41.dp),
    val PreviewIconPadding: Sized<Dp> = Sized(8.dp),
    val ProjectCardNotesPadding: Sized<Dp> = Sized(6.dp),

    val RadioMenuGap: Sized<Dp> = Sized(4.dp),
    val RadioMenuStrokeWidth: Sized<Dp> = StrokeMedium,

    val RelativeInputPopupPadding: Sized<Dp> = Sized(8.dp),
    val RelativeInputPopupItemPadding: Sized<Dp> = Sized(4.dp),
    val RelativeInputPopupItemHeight: Sized<Dp> = Sized(20.dp),

    val SettingsRadioIconHeight: Sized<Dp> = Sized(32.dp),
    val SettingsBoxPadding: Sized<Dp> = Sized(12.dp),
    val SectionMenuInternalPadding: Sized<Dp> = Sized(8.dp),
    val ShortcutIconPadding: Sized<Dp> = Sized(7.dp),

    val SortableMenuSortButtonDiameter: Sized<Dp> = Sized(36.dp),
    val SortableMenuSortButtonPadding: Sized<PaddingValues> = Sized(PaddingValues(6.dp)),
    val SortableMenuHeadSpacing: Sized<Dp> = Sized(4.dp),
    val SortableMenuBoxRadius: Sized<Dp> = Sized(8.dp),
    val SortableMenuLineGap: Sized<Dp> = Sized(4.dp),

    val SoundFontMenuButtonPadding: Sized<Dp> = Sized(8.dp),
    val SoundFontMenuButtonExtraPadding: Sized<Dp> = Sized(8.dp),
    val SoundFontMenuPadding: Sized<Dp> = Sized(12.dp),
    val SoundFontMenuInnerPadding: Sized<Dp> = Sized(8.dp),
    val SoundFontMenuIconHeight: Sized<Dp> = Sized(41.dp),
    val SFWarningInnerPadding: Sized<Dp> = Sized(10.dp),
    val SoundFontWarningBorderWidth: Sized<Dp> = Sized(4.dp),
    val SoundFontWarningPadding: Sized<Dp> = Sized(16.dp),

    val TableLineStroke: Sized<Dp> = StrokeThin,

    val TinyTuningDialogButtonSize: Sized<Dp> = Sized(41.dp),
    val TinyTuningDialogButtonPadding: Sized<Dp> = Sized(4.dp),
    val TinyTuningDialogInnerPadding: Sized<Dp> = Sized(6.dp),
    val TinyTuningDialogInputWidth: Sized<Dp> = Sized(64.dp),

    val TopBarHeight: Sized<Dp> = Sized(
        SmallLandscape to 41.dp,
        SmallPortrait to 48.dp,
        MediumPortrait to 48.dp,
        MediumLandscape to 54.dp,
        LargePortrait to 54.dp,
        LargeLandscape to 54.dp,
    ),
    val TopBarIconHeight: Sized<Dp> = Sized(32.dp),
    val TopBarIconWidth: Sized<Dp> = Sized(54.dp),
    val TopBarItemSpace: Sized<Dp> = Sized(8.dp),

    val TransposeDialogInnerPadding: Sized<Dp> = Sized(8.dp),
    val TransposeDialogInputPadding: Sized<Dp> = Sized(8.dp),
    val TransposeDialogInputWidth: Sized<Dp> = Sized(64.dp),

    val TuningDialogStrokeWidth: Sized<Dp> = StrokeThin,
    val TuningDialogBoxPadding: Sized<Dp> = Sized(6.dp),
    val TuningDialogLinePadding: Sized<Dp> = Sized(8.dp),
    val TuningDialogLineSpacing: Sized<Dp> = Sized(2.dp),

    val ZoomBarTitleHeight: Sized<Dp> = Sized(32.dp),

    val Unpadded: Sized<PaddingValues> = Sized(PaddingValues(0.dp))
)

class PaganDimensionsWrapper(val inner: PaganDimensions) {
    var active_layout_size: LayoutSize = LayoutSize.SmallPortrait
    fun <T, U: Sized<T>> getter(v: U): T {
        return v[this.active_layout_size]
    }

    fun set_active_layout_size(width: Dp, height: Dp): LayoutSize {
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

    val LeafBaseWidth: Dp get() { return this.getter(this.inner.LeafBaseWidth) }
    val SpaceSmall: Dp get() { return this.getter(this.inner.SpaceSmall) }
    val SpaceMedium: Dp get() { return this.getter(this.inner.SpaceMedium) }
    val SpaceLarge: Dp get() { return this.getter(this.inner.SpaceLarge) }
    val StrokeThin: Dp get() { return this.getter(this.inner.StrokeThin) }
    val StrokeMedium: Dp get() { return this.getter(this.inner.StrokeMedium) }
    val StrokeThick: Dp get() { return this.getter(this.inner.StrokeThick) }
    val StrokeXThick: Dp get() { return this.getter(this.inner.StrokeXThick) }

    val AboutPadding: Dp get() { return this.getter(this.inner.AboutPadding) }
    val AboutUrlSectionPadding: Dp get() { return this.getter(this.inner.AboutUrlSectionPadding) }
    val BackGroundRadius: Dp get() { return this.getter(this.inner.BackGroundRadius) }
    val BackGroundGap: Dp get() { return this.getter(this.inner.BackGroundGap) }
    val BackGroundBarWidth: Dp get() { return this.getter(this.inner.BackGroundBarWidth) }
    val BackGroundBarSmallHeight: Dp get() { return this.getter(this.inner.BackGroundBarSmallHeight) }
    val BackGroundBarLargeHeight: Dp get() { return this.getter(this.inner.BackGroundBarLargeHeight) }
    val BeatLabelHorizontalPadding: Dp get() { return this.getter(this.inner.BeatLabelHorizontalPadding) }
    val BeatLabelVerticalPadding: Dp get() { return this.getter(this.inner.BeatLabelVerticalPadding) }
    val ButtonHeightNormal: Dp get() { return this.getter(this.inner.ButtonHeightNormal) }
    val ButtonHeightSmall: Dp get() { return this.getter(this.inner.ButtonHeightSmall) }
    val BugReportPadding: Dp get() { return this.getter(this.inner.BugReportPadding) }
    val ChannelGapHeight: Dp get() { return this.getter(this.inner.ChannelGapHeight) }
    val ColorPickerHexInputWidth: Dp get() { return this.getter(this.inner.ColorPickerHexInputWidth) }
    val ColorPickerLabelWidth: Dp get() { return this.getter(this.inner.ColorPickerLabelWidth) }
    val ColorPickerInnerPadding: Dp get() { return this.getter(this.inner.ColorPickerInnerPadding) }
    val ColorPickerPreviewHeight: Dp get() { return this.getter(this.inner.ColorPickerPreviewHeight) }
    val ColorPickerSliderWidth: Dp get() { return this.getter(this.inner.ColorPickerSliderWidth) }
    val ConfigButtonPadding: PaddingValues get() { return this.getter(this.inner.ConfigButtonPadding) }
    val ConfigChannelButtonHeight: Dp get() { return this.getter(this.inner.ConfigChannelButtonHeight) }
    val ConfigBottomButtonHeight: Dp get() { return this.getter(this.inner.ConfigBottomButtonHeight) }
    val ConfigBottomButtonWidth: Dp get() { return this.getter(this.inner.ConfigBottomButtonWidth) }
    val ConfigChannelSpacing: Dp get() { return this.getter(this.inner.ConfigChannelSpacing) }
    val ConfigDrawerPadding: Dp get() { return this.getter(this.inner.ConfigDrawerPadding) }
    val ConfigDrawerButtonPadding: Dp get() { return this.getter(this.inner.ConfigDrawerButtonPadding) }
    val ConfigDrawerButtonExtraPadding: Dp get() { return this.getter(this.inner.ConfigDrawerButtonExtraPadding) }
    val ConfigDrawerButtonRadius: Dp get() { return this.getter(this.inner.ConfigDrawerButtonRadius) }
    val ConfigDrawerChannelLabelPadding: Dp get() { return this.getter(this.inner.ConfigDrawerChannelLabelPadding) }
    val ContextMenuButtonHeight: Dp get() { return this.getter(this.inner.ContextMenuButtonHeight) }
    val ContextMenuButtonWidth: Dp get() { return this.getter(this.inner.ContextMenuButtonWidth) }
    val ContextMenuButtonIconWidth: Dp get() { return this.getter(this.inner.ContextMenuButtonIconWidth) }
    val ContextMenuButtonPadding: PaddingValues get() { return this.getter(this.inner.ContextMenuButtonPadding) }
    val ContextMenuButtonRadius: Dp get() { return this.getter(this.inner.ContextMenuButtonRadius) }
    val ContextMenuPadding: Dp get() { return this.getter(this.inner.ContextMenuPadding) }
    val ContextMenuSpacing: Dp get() { return this.getter(this.inner.ContextMenuSpacing) }
    val ContextMenuRadius: Dp get() { return this.getter(this.inner.ContextMenuRadius) }
    val DialogAdjustInnerSpace: Dp get() { return this.getter(this.inner.DialogAdjustInnerSpace) }
    val DashedBorderWidth: Dp get() { return this.getter(this.inner.DashedBorderWidth) }
    val DashedBorderDash: Dp get() { return this.getter(this.inner.DashedBorderDash) }
    val DashedBorderGap: Dp get() { return this.getter(this.inner.DashedBorderGap) }
    val DialogBarButtonHeight: Dp get() { return this.getter(this.inner.DialogBarButtonHeight) }
    val DialogBarPaddingVertical: Dp get() { return this.getter(this.inner.DialogBarPaddingVertical) }
    val DialogBarSpacing: Dp get() { return this.getter(this.inner.DialogBarSpacing) }
    val DialogPadding: Dp get() { return this.getter(this.inner.DialogPadding) }
    val DialogLineHeight: Dp get() { return this.getter(this.inner.DialogLineHeight) }
    val DialogLinePadding: PaddingValues get() { return this.getter(this.inner.DialogLinePadding) }
    val DialogTitlePadding: PaddingValues get() { return this.getter(this.inner.DialogTitlePadding) }
    val DialogRadius: Dp get() { return this.getter(this.inner.DialogRadius) }
    val DivisorPadding: Dp get() { return this.getter(this.inner.DivisorPadding) }
    val EffectLineHeight: Dp get() { return this.getter(this.inner.EffectLineHeight) }
    val EffectDialogIconWidth: Dp get() { return this.getter(this.inner.EffectDialogIconWidth) }
    val EffectDialogIconHeight: Dp get() { return this.getter(this.inner.EffectDialogIconHeight) }
    val EffectTransitionDialogIconHeight: Dp get() { return this.getter(this.inner.EffectTransitionDialogIconHeight) }
    val ExtraTableIconsPadding: Dp get() { return this.getter(this.inner.ExtraTableIconsPadding) }
    val HexDisplayHeight: Dp get() { return this.getter(this.inner.HexDisplayHeight) }
    val HexDisplayStrokeWidth: Dp get() { return this.getter(this.inner.HexDisplayStrokeWidth) }
    val KnobPadding: PaddingValues get() { return this.getter(this.inner.KnobPadding) }
    val NumberInputPadding: PaddingValues get() { return this.getter(this.inner.NumberInputPadding) }
    val EffectWidgetPanCenterDotDiameter: Dp get() { return this.getter(this.inner.EffectWidgetPanCenterDotDiameter) }
    val EffectWidgetPanSliderPadding: Dp get() { return this.getter(this.inner.EffectWidgetPanSliderPadding) }
    val EffectWidgetVelocityFadePopupHeight: Dp get() { return this.getter(this.inner.EffectWidgetVelocityFadePopupHeight) }
    val EffectWidgetVelocityFadePopupWidth: Dp get() { return this.getter(this.inner.EffectWidgetVelocityFadePopupWidth) }
    val EffectWidgetVelocityFadePopupPadding: Dp get() { return this.getter(this.inner.EffectWidgetVelocityFadePopupPadding) }
    val EffectWidgetVelocityInputWidth: Dp get() { return this.getter(this.inner.EffectWidgetVelocityInputWidth) }
    val EffectWidgetVelocityInputIconWidth: Dp get() { return this.getter(this.inner.EffectWidgetVelocityInputIconWidth) }
    val EffectWidgetDelayFadePopupHeight: Dp get() { return this.getter(this.inner.EffectWidgetDelayFadePopupHeight) }
    val EffectWidgetDelayFadePopupWidth: Dp get() { return this.getter(this.inner.EffectWidgetDelayFadePopupWidth) }
    val EffectWidgetDelayFadePopupPadding: Dp get() { return this.getter(this.inner.EffectWidgetDelayFadePopupPadding) }
    val EffectWidgetDelayInputWidth: Dp get() { return this.getter(this.inner.EffectWidgetDelayInputWidth) }
    val EffectWidgetDelayInputIconWidth: Dp get() { return this.getter(this.inner.EffectWidgetDelayInputIconWidth) }
    val EffectWidgetInputHeight: Dp get() { return this.getter(this.inner.EffectWidgetInputHeight) }
    val LandingButtonCornerRadius: Dp get() { return this.getter(this.inner.LandingButtonCornerRadius) }
    val LandingButtonHeight: Dp get() { return this.getter(this.inner.LandingButtonHeight) }
    val LandingPadding: Dp get() { return this.getter(this.inner.LandingPadding) }
    val LandingIconButtonSize: Dp get() { return this.getter(this.inner.LandingIconButtonSize) }
    val LandingIconButtonPadding: Dp get() { return this.getter(this.inner.LandingIconButtonPadding) }
    val LinkUrlPadding: Dp get() { return this.getter(this.inner.LinkUrlPadding) }
    val LineHeight: Dp get() { return this.getter(this.inner.LineHeight) }
    val LineLabelWidth: Dp get() { return this.getter(this.inner.LineLabelWidth) }
    val LineLabelPadding: PaddingValues get() { return this.getter(this.inner.LineLabelPadding) }
    val LineCtlLabelPadding: PaddingValues get() { return this.getter(this.inner.LineCtlLabelPadding) }
    val LineLabelIconPaddingGlobal: PaddingValues get() { return this.getter(this.inner.LineLabelIconPaddingGlobal) }
    val LineLabelIconPaddingLine: PaddingValues get() { return this.getter(this.inner.LineLabelIconPaddingLine) }
    val LineLabelIconPaddingChannel: PaddingValues get() { return this.getter(this.inner.LineLabelIconPaddingChannel) }
    val SelectionBorderPadding: PaddingValues get() { return this.getter(this.inner.SelectionBorderPadding) }
    val NumberInputDialogPadding: PaddingValues get() { return this.getter(this.inner.NumberInputDialogPadding) }
    val FloatInputDialogWidth: Dp get() { return this.getter(this.inner.FloatInputDialogWidth) }
    val NumberInputDialogWidth: Dp get() { return this.getter(this.inner.NumberInputDialogWidth) }
    val NumberPickerRowHeight: Dp get() { return this.getter(this.inner.NumberPickerRowHeight) }
    val NumberPickerRowWidth: Dp get() { return this.getter(this.inner.NumberPickerRowWidth) }
    val NumberPickerStrokeWidth: Dp get() { return this.getter(this.inner.NumberPickerStrokeWidth) }
    val NotePickerButtonHeight: Dp get() { return this.getter(this.inner.NotePickerButtonHeight) }
    val NotePickerButtonRadius: Dp get() { return this.getter(this.inner.NotePickerButtonRadius) }
    val NotePickerColumnWidth: Dp get() { return this.getter(this.inner.NotePickerColumnWidth) }
    val NotePickerSpacing: Dp get() { return this.getter(this.inner.NotePickerSpacing) }
    val OutlineButtonStrokeWidth: Dp get() { return this.getter(this.inner.OutlineButtonStrokeWidth) }
    val PaletteDotPaddingEnd: Dp get() { return this.getter(this.inner.PaletteDotPaddingEnd) }
    val PaletteDotPaddingBottom: Dp get() { return this.getter(this.inner.PaletteDotPaddingBottom) }
    val PaletteDotSize: Dp get() { return this.getter(this.inner.PaletteDotSize) }
    val PercussionSwitchIconPadding: Dp get() { return this.getter(this.inner.PercussionSwitchIconPadding) }
    val PresetMenuArrowWidth: Dp get() { return this.getter(this.inner.PresetMenuArrowWidth) }
    val PresetMenuArrowHeight: Dp get() { return this.getter(this.inner.PresetMenuArrowHeight) }
    val PreviewIconHeight: Dp get() { return this.getter(this.inner.PreviewIconHeight) }
    val PreviewIconPadding: Dp get() { return this.getter(this.inner.PreviewIconPadding) }
    val ProjectCardNotesPadding: Dp get() { return this.getter(this.inner.ProjectCardNotesPadding) }
    val RadioMenuGap: Dp get() { return this.getter(this.inner.RadioMenuGap) }
    val RadioMenuStrokeWidth: Dp get() { return this.getter(this.inner.RadioMenuStrokeWidth) }
    val RelativeInputPopupPadding: Dp get() { return this.getter(this.inner.RelativeInputPopupPadding) }
    val RelativeInputPopupItemPadding: Dp get() { return this.getter(this.inner.RelativeInputPopupItemPadding) }
    val RelativeInputPopupItemHeight: Dp get() { return this.getter(this.inner.RelativeInputPopupItemHeight) }
    val SettingsRadioIconHeight: Dp get() { return this.getter(this.inner.SettingsRadioIconHeight) }
    val SettingsBoxPadding: Dp get() { return this.getter(this.inner.SettingsBoxPadding) }
    val SectionMenuInternalPadding: Dp get() { return this.getter(this.inner.SectionMenuInternalPadding) }
    val ShortcutIconPadding: Dp get() { return this.getter(this.inner.ShortcutIconPadding) }
    val SortableMenuSortButtonDiameter: Dp get() { return this.getter(this.inner.SortableMenuSortButtonDiameter) }
    val SortableMenuSortButtonPadding: PaddingValues get() { return this.getter(this.inner.SortableMenuSortButtonPadding) }
    val SortableMenuHeadSpacing: Dp get() { return this.getter(this.inner.SortableMenuHeadSpacing) }
    val SortableMenuBoxRadius: Dp get() { return this.getter(this.inner.SortableMenuBoxRadius) }
    val SortableMenuLineGap: Dp get() { return this.getter(this.inner.SortableMenuLineGap) }
    val SoundFontMenuButtonPadding: Dp get() { return this.getter(this.inner.SoundFontMenuButtonPadding) }
    val SoundFontMenuButtonExtraPadding: Dp get() { return this.getter(this.inner.SoundFontMenuButtonExtraPadding) }
    val SoundFontMenuPadding: Dp get() { return this.getter(this.inner.SoundFontMenuPadding) }
    val SoundFontMenuInnerPadding: Dp get() { return this.getter(this.inner.SoundFontMenuInnerPadding) }
    val SoundFontMenuIconHeight: Dp get() { return this.getter(this.inner.SoundFontMenuIconHeight) }
    val SFWarningInnerPadding: Dp get() { return this.getter(this.inner.SFWarningInnerPadding) }
    val SoundFontWarningBorderWidth: Dp get() { return this.getter(this.inner.SoundFontWarningBorderWidth) }
    val SoundFontWarningPadding: Dp get() { return this.getter(this.inner.SoundFontWarningPadding) }
    val TableLineStroke: Dp get() { return this.getter(this.inner.TableLineStroke) }
    val TinyTuningDialogButtonSize: Dp get() { return this.getter(this.inner.TinyTuningDialogButtonSize) }
    val TinyTuningDialogButtonPadding: Dp get() { return this.getter(this.inner.TinyTuningDialogButtonPadding) }
    val TinyTuningDialogInnerPadding: Dp get() { return this.getter(this.inner.TinyTuningDialogInnerPadding) }
    val TinyTuningDialogInputWidth: Dp get() { return this.getter(this.inner.TinyTuningDialogInputWidth) }
    val TopBarHeight: Dp get() { return this.getter(this.inner.TopBarHeight) }
    val TopBarIconHeight: Dp get() { return this.getter(this.inner.TopBarIconHeight) }
    val TopBarIconWidth: Dp get() { return this.getter(this.inner.TopBarIconWidth) }
    val TopBarItemSpace: Dp get() { return this.getter(this.inner.TopBarItemSpace) }
    val TransposeDialogInnerPadding: Dp get() { return this.getter(this.inner.TransposeDialogInnerPadding) }
    val TransposeDialogInputPadding: Dp get() { return this.getter(this.inner.TransposeDialogInputPadding) }
    val TransposeDialogInputWidth: Dp get() { return this.getter(this.inner.TransposeDialogInputWidth) }
    val TuningDialogStrokeWidth: Dp get() { return this.getter(this.inner.TuningDialogStrokeWidth) }
    val TuningDialogBoxPadding: Dp get() { return this.getter(this.inner.TuningDialogBoxPadding) }
    val TuningDialogLinePadding: Dp get() { return this.getter(this.inner.TuningDialogLinePadding) }
    val TuningDialogLineSpacing: Dp get() { return this.getter(this.inner.TuningDialogLineSpacing) }
    val ZoomBarTitleHeight: Dp get() { return this.getter(this.inner.ZoomBarTitleHeight) }
    val Unpadded: PaddingValues get() { return this.getter(this.inner.Unpadded) }

}

object Dimensioddns {
    var active_layout_size = LayoutSize.SmallPortrait

    fun set_active_layout_size(width: Dp, height: Dp): LayoutSize {
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
            SmallPortrait -> mapped_values[SmallLandscape]?.let { return it }
            SmallLandscape -> mapped_values[SmallPortrait]?.let { return it }
            MediumPortrait -> mapped_values[MediumLandscape]?.let { return it }
            MediumLandscape -> mapped_values[MediumPortrait]?.let { return it }
            LargePortrait -> mapped_values[LargeLandscape]?.let { return it }
            LargeLandscape -> mapped_values[LargePortrait]?.let { return it }
            XLargePortrait -> mapped_values[XLargeLandscape]?.let { return it }
            XLargeLandscape -> mapped_values[XLargePortrait]?.let { return it }
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

    val BeatLabelHorizontalPadding = 3.dp
    val BeatLabelVerticalPadding = 8.dp

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

    val ContextMenuButtonPadding: PaddingValues
        get() = getter(
            small_portrait = PaddingValues(8.dp),
            small_landscape = PaddingValues(6.dp),
            medium_portrait = PaddingValues(8.dp)
        )
    val ContextMenuButtonRadius = 8.dp
    val ContextMenuPadding = Space.Medium
    val ContextMenuSpacing: Dp
        get() = getter(
            small_portrait = Space.Medium,
            small_landscape = Space.Small,
            medium_portrait = Space.Medium,
        )
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
    val EffectDialogIconHeight = 32.dp
    val EffectTransitionDialogIconHeight = 32.dp

    val ExtraTableIconsPadding = 6.dp

    val HexDisplayStrokeWidth = Stroke.Thin
    val HexDisplayHeight = 48.dp

    val KnobPadding = PaddingValues(4.dp)

    val NumberInputPadding = PaddingValues(12.dp)

    object EffectWidget {
        object Pan {
            val CenterDotDiameter = 12.dp
            val SliderPadding = ContextMenuButtonWidth
        }
        object Velocity {
            val FadePopupHeight = 250.dp
            val FadePopupWidth = 50.dp
            val FadePopupPadding = 8.dp
            val InputWidth = 54.dp
            val InputIconWidth = 41.dp
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
            small_landscape = 56.dp,
            small_portrait = 64.dp,
            medium_landscape = 64.dp,
            medium_portrait = 128.dp,
            large_portrait = 128.dp,
            large_landscape = 96.dp,
            xlarge_portrait = 128.dp
        )
    val LandingPadding: Dp
        get() = getter(
            small_portrait = 8.dp,
        )
    val LandingIconButtonSize: Dp
        get() = getter(
            small_landscape = 41.dp,
            small_portrait = 41.dp,
            medium_portrait = 48.dp,
            large_landscape = 64.dp
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
    val LineLabelPadding = PaddingValues(
        horizontal = 4.dp,
        vertical = 2.dp
    )
    val LineCtlLabelPadding = PaddingValues(
        horizontal = 3.dp,
        vertical = 0.dp
    )
    val LineLabelIconPaddingGlobal = PaddingValues(
        bottom = 2.dp,
        top = 2.dp,
        start = 2.dp,
        end = 2.dp,
    )
    val LineLabelIconPaddingLine = PaddingValues(
        bottom = 2.dp,
        top = 2.dp,
        start = 1.dp,
        end = 7.dp,
    )
    val LineLabelIconPaddingChannel = PaddingValues(
        bottom = 2.dp,
        top = 2.dp,
        start = 6.dp,
        end = 2.dp,
    )

    val SelectionBorderPadding = PaddingValues(1.5.dp)

    val NumberInputDialogPadding = PaddingValues(
        horizontal = 8.dp,
        vertical = 4.dp
    )

    val FloatInputDialogWidth = 200.dp
    val NumberInputDialogWidth = 128.dp
    val NumberPickerRowHeight = 41.dp
    val NumberPickerRowWidth = 80.dp
    val NumberPickerStrokeWidth = Stroke.Thin

    val NotePickerButtonHeight: Dp
        get() = getter(
            small_portrait = 41.dp,
            medium_portrait = 48.dp,
        )
    val NotePickerButtonRadius = 4.dp
    val NotePickerColumnWidth = 41.dp
    val NotePickerSpacing = 3.dp

    val OutlineButtonStrokeWidth = Stroke.Thin

    val PaletteDotPaddingEnd = 4.dp
    val PaletteDotPaddingBottom = 6.dp
    val PaletteDotSize = 12.dp

    val PercussionSwitchIconPadding = 4.dp
    val PresetMenuArrowWidth = 24.dp
    val PresetMenuArrowHeight = 24.dp
    val PreviewIconHeight = 41.dp
    val PreviewIconPadding = 8.dp
    val ProjectCardNotesPadding = 6.dp

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
    val SectionMenuInternalPadding = 8.dp
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

    val SoundFontMenuButtonPadding = 8.dp
    val SoundFontMenuButtonExtraPadding = 8.dp
    val SoundFontMenuPadding = 12.dp
    val SoundFontMenuInnerPadding = 8.dp
    val SoundFontMenuIconHeight = 41.dp
    val SFWarningInnerPadding = 10.dp
    val SoundFontWarningBorderWidth = 4.dp
    val SoundFontWarningPadding = 16.dp

    object Stroke {
        val Thin = 1.dp
        val Medium = 2.dp
        val Thick = 4.dp
        val XThick = 8.dp
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
    val TuningDialogLinePadding = 8.dp
    val TuningDialogLineSpacing = 2.dp

    val Unpadded = PaddingValues(0.dp)

    val ZoomBarTitleHeight = 32.dp
}
