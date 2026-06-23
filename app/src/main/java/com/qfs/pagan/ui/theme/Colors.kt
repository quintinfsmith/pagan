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

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.graphics.Color
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.ui.theme.PaganColorScheme.Companion.Defaults.UNUSED

object Colors {
    var active_color_scheme = PaganColorScheme()

    val LEAF_COLOR_INVALID = Color(0xFFe51C3A)
    val LEAF_COLOR_INVALID_SELECTED = Color(0xFF890E21)

    val EFFECT_LINE_COLOR = Color(0xFFFFFFFF)
    val EFFECT_LINE_COLOR_NIGHT = Color(0xFF000000)

    enum class LeafState {
        Active,
        Spill,
        Empty
    }

    enum class LeafSelection {
        Primary,
        Secondary,
        Unselected
    }

    fun get_text(input: Color): Color {
        // Green is brighter, weight it more
        val avg = (input.red + (input.green * 2F) + input.blue) / 3F
        return if (avg > .5F) Color(0xFF000000)
        else  Color(0xFFFFFFFF)
    }

    fun get_leaf_color(
        line_palette: OpusColorPalette,
        channel_palette: OpusColorPalette,
        active: LeafState,
        selected: LeafSelection,
        is_effect_line: Boolean,
        is_muted: Boolean,
    ): Triple<Color, Color, Color?> {
        val (event_color, line_color) = if (is_effect_line) {
            Pair(
                line_palette.effect ?: channel_palette.effect ?: active_color_scheme.EFFECT_COLOR,
                line_palette.effect_bg ?: channel_palette.effect_bg ?: active_color_scheme.EFFECT_LINE_COLOR
            )
        } else {
            Pair(
                line_palette.event ?: channel_palette.event ?: active_color_scheme.LEAF_COLOR,
                line_palette.event_bg ?: channel_palette.event_bg ?: active_color_scheme.LINE_COLOR
            )
        }

        val is_empty = active == LeafState.Empty

        var leaf_color = when(active) {
            LeafState.Active -> event_color
            LeafState.Spill -> active_color_scheme.SPILL(event_color)
            LeafState.Empty -> line_color
        }


        leaf_color = when(selected) {
            LeafSelection.Primary -> active_color_scheme.SELECTED_PRIMARY(is_empty, leaf_color)
            LeafSelection.Secondary -> active_color_scheme.SELECTED_SECONDARY(is_empty, leaf_color)
            LeafSelection.Unselected -> leaf_color
        }

        if (is_muted) {
            leaf_color = active_color_scheme.MUTED(is_empty, leaf_color)
        }

        return Triple(
            leaf_color,
            this.get_text(leaf_color),
            null
        )
    }

    fun get_button_colors(): ButtonColors {
        return ButtonColors(
            containerColor = this.active_color_scheme.button,
            contentColor = this.active_color_scheme.button_foreground,
            disabledContainerColor = this.active_color_scheme.button_disabled,
            disabledContentColor = this.active_color_scheme.button_disabled_foreground,
        )
    }

    fun get_outline_button_colors(): ButtonColors {
        return ButtonColors(
            containerColor = Color(0x00000000),
            disabledContainerColor = Color(0x00000000),
            contentColor = this.active_color_scheme.button,
            disabledContentColor = this.active_color_scheme.button_disabled,
        )
    }

    fun get_slider_colors(): SliderColors {
        return SliderColors(
            thumbColor = this.active_color_scheme.SLIDER_THUMB,
            activeTrackColor = this.active_color_scheme.SLIDER_TRACK,
            activeTickColor = this.active_color_scheme.SLIDER_TICK,
            inactiveTrackColor = this.active_color_scheme.SLIDER_TRACK_INACTIVE,
            inactiveTickColor = this.active_color_scheme.SLIDER_TICK_INACTIVE,
            disabledThumbColor = UNUSED,
            disabledActiveTrackColor = UNUSED,
            disabledActiveTickColor = UNUSED,
            disabledInactiveTrackColor = UNUSED,
            disabledInactiveTickColor = UNUSED
        )
    }

    fun get_pan_slider_colors(): SliderColors {
        return this.get_slider_colors().copy(
            inactiveTrackColor = this.active_color_scheme.SLIDER_TRACK_INACTIVE,
            inactiveTickColor = this.active_color_scheme.SLIDER_TRACK,
            activeTrackColor = this.active_color_scheme.SLIDER_TRACK_INACTIVE,
            activeTickColor = this.active_color_scheme.SLIDER_TRACK
        )
    }

    fun get_switch_colors(): SwitchColors {
        return SwitchColors(
            checkedThumbColor = this.active_color_scheme.SWITCH_THUMB_CHECKED,
            checkedTrackColor = this.active_color_scheme.SWITCH_TRACK_CHECKED,
            checkedBorderColor = this.active_color_scheme.SWITCH_BORDER_CHECKED,
            checkedIconColor = this.active_color_scheme.SWITCH_ICON_CHECKED,
            uncheckedThumbColor = this.active_color_scheme.SWITCH_THUMB_UNCHECKED,
            uncheckedTrackColor = this.active_color_scheme.SWITCH_TRACK_UNCHECKED,
            uncheckedBorderColor = this.active_color_scheme.SWITCH_BORDER_UNCHECKED,
            uncheckedIconColor = this.active_color_scheme.SWITCH_ICON_UNCHECKED,
            disabledCheckedThumbColor = UNUSED,
            disabledCheckedTrackColor = UNUSED,
            disabledCheckedBorderColor = UNUSED,
            disabledCheckedIconColor = UNUSED,
            disabledUncheckedThumbColor = UNUSED,
            disabledUncheckedTrackColor = UNUSED,
            disabledUncheckedBorderColor = UNUSED,
            disabledUncheckedIconColor = UNUSED
        )
    }

    fun get_textfield_colors(): TextFieldColors {
        return TextFieldColors(

        )
    }
}

fun Color.avg(): Float {
    return (this.red + this.green + this.blue) / 3
}
fun Color.grey(): Color {
    val avg = this.avg()
    return Color(avg, avg,avg, this.alpha)
}
