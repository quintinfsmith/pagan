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
    MainRow,
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
    VelocitySlider,
    VelocityButton,
    VolumeSlider,
    VolumeButton,
    LineLabel,
    Leaf
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
