/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

enum class TestTag {
    Undo,
    Redo,
    MenuItem,
    OuterInsertBeat,
    DialogNumberInput,
    DialogNegative,
    DialogPositive,
    DialogNeutral,
    LandingImport,
    LandingSettings,
    LandingNewProject,
    LandingLoadProject,
    LandingRecent,
    LandingAbout,
    ShortCut,
    SortableMenu,
    MainRow,
    BeatLabel,
    TopBarKebab,
    AdjustSelection,
    BeatInsert,
    ChannelInsert,
    ChannelPercussionInsert,
    ChannelMute,
    ChannelColor,
    EffectTransition,
    EventOctave,
    EventOffset,
    ChannelPreset,
    ChannelEffects,
    BeatToggleTag,
    BeatRemove,
    ChannelRemove,
    LeafSplit,
    LeafInsert,
    LeafRemove,
    EventDuration,
    EventUnset,
    PercussionToggle,
    EffectHide,
    LineMute,
    LineColor,
    InstrumentSet,
    LineEffectRemove,
    LineRemove,
    LineNew,
    LineEffectsShow,
    RangeUnset,
    DelayHzNumerator,
    DelayHzDenominator,
    DelayEcho,
    DelayFadeButton,
    DelayFadeSlider,
    PanSlider,
    Tempo,
    VelocityInput,
    VelocityVSlider,
    VelocityHSlider,
    VelocitySlideNumerator,
    VelocitySlideDenominator,
    VelocityButton,
    VolumeSlider,
    VolumeButton,
    LineLabel,
    Leaf,
    ZoomSlider
}

fun test_tag_to_string(tag: TestTag, vararg args: Any?): String {
    var string = tag.name
    for (arg in args) {
        string += "|$arg"
    }
    return string
}

@Composable
fun Modifier.testTag(tag: TestTag, vararg args: Any?): Modifier {
    return this then Modifier.testTag(test_tag_to_string(tag, *args))
}
