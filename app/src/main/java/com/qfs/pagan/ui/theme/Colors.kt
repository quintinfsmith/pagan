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

import androidx.compose.ui.graphics.Color
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import kotlin.math.min

object Colors {
    val LEAF_COLOR = Color(0xFF765bd5)
    val EFFECT_COLOR = Color(0xFFCB9C20)
    val LINE_COLOR = Color(0xFFE0E0E0)
    val LINE_COLOR_NIGHT = Color(0xFF232323)

    val SPILL: (Color) -> Color = { base_color ->
        val avg = base_color.avg()
        val weight = .75F
        Color(
            red = ((base_color.red * weight) + (avg * (1 - weight))) * .75F,
            green = ((base_color.green * weight) + (avg * (1 - weight))) * .75F,
            blue = ((base_color.blue * weight) + (avg * (1 - weight))) * .75F,
            alpha = base_color.alpha
        )
    }

    val LINE_SELECTED = Color(0xFF5BA1D6)
    val SELECTION = Color(0xFF0033AA)

    val SELECTED_PRIMARY: (Boolean, Color) -> Color = { is_empty, base_color ->
        if (is_empty) {
            val weight = .2F
            Color(
                red = (base_color.red * weight) + (LINE_SELECTED.red * (1F - weight)),
                green = (base_color.green * weight) + (LINE_SELECTED.green * (1F - weight)),
                blue = (base_color.blue * weight) + (LINE_SELECTED.blue * (1F - weight)),
            )
        } else {
            val weight = .4F
            Color(
                red = (base_color.red * weight) + (SELECTION.red * (1F - weight)),
                green = (base_color.green * weight) + (SELECTION.green * (1F - weight)),
                blue = (base_color.blue * weight) + (SELECTION.blue * (1F - weight)),
            )
        }
    }

    val SELECTED_SECONDARY: (Boolean, Color) -> Color = SELECTED_PRIMARY

    val MUTED: (Boolean, Color) -> Color = { is_empty, line_color ->
        val (grey, weight) = if (is_empty) {
            Pair(.5F, .5F)
        } else {
            Pair(line_color.avg(), .3F)
        }
        Color(
            red = (line_color.red * weight) + (grey * (1F - weight)),
            green = (line_color.green * weight) + (grey * (1F - weight)),
            blue = (line_color.blue * weight) + (grey * (1F - weight)),
            line_color.alpha
        )
    }

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
        dark_mode: Boolean = false
    ): Triple<Color, Color, Color?> {
        val (event_color, line_color) = if (is_effect_line) {
            Pair(
                line_palette.effect ?: channel_palette.effect ?: EFFECT_COLOR,
                line_palette.effect_bg ?: channel_palette.effect_bg ?: if (dark_mode) EFFECT_LINE_COLOR_NIGHT else EFFECT_LINE_COLOR
            )
        } else {
            Pair(
                line_palette.event ?: channel_palette.event ?: LEAF_COLOR,
                line_palette.event_bg ?: channel_palette.event_bg ?: if (dark_mode) LINE_COLOR_NIGHT else LINE_COLOR
            )
        }

        val is_empty = active == LeafState.Empty

        var leaf_color = when(active) {
            LeafState.Active -> event_color
            LeafState.Spill -> SPILL(event_color)
            LeafState.Empty -> line_color
        }


        leaf_color = when(selected) {
            LeafSelection.Primary -> SELECTED_PRIMARY(is_empty, leaf_color)
            LeafSelection.Secondary -> SELECTED_SECONDARY(is_empty, leaf_color)
            LeafSelection.Unselected -> leaf_color
        }

        if (is_muted) {
            leaf_color = MUTED(is_empty, leaf_color)
        }

        return Triple(
            leaf_color,
            this.get_text(leaf_color),
            null
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