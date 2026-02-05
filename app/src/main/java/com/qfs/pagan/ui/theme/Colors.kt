package com.qfs.pagan.ui.theme

import androidx.compose.ui.graphics.Color
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import kotlin.math.min

object Colors {
    val LEAF_COLOR = Color(0xFF765bd5)
    val LEAF_COLOR_INVALID = Color(0xFFe51C3A)
    val LEAF_COLOR_INVALID_SELECTED = Color(0xFF890E21)

    val EFFECT_COLOR = Color(0xFFCB9C20)
    val EFFECT_COLOR_SELECTED = Color(0xFF095660)
    val EFFECT_COLOR_SECONDARY = Color(0xFF567B80)
    val EFFECT_LINE_COLOR = Color(0xFFFFFFFF)
    val EFFECT_LINE_COLOR_NIGHT = Color(0xFF000000)

    val SELECTION = Color(0xFF25BAFF)
    val LINE_COLOR = Color(0xFFE0E0E0)
    val LINE_SELECTED = Color(0xFF5BA1D6)
    val LINE_COLOR_NIGHT = Color(0xFF232323)
    val LINE_COLOR_SECONDARY = Color(0xFF95BFDE)
    val LINE_COLOR_SECONDARY_NIGHT = Color(0xFF416B8B)

    val MUTED_LEAF_COLOR = Color(0xFF888888)
    val MUTED_LEAF_SELECTED = Color(0xFF444444)
    val MUTED_LINE_SELECTED = Color(0xFF999999)
    val MUTED_LINE_COLOR = Color(0xFF454545)
    val MUTED_SECONDARY = Color(0xFF929292)

    val MUTED_LEAF_COLOR_NIGHT = Color(0xFF5E5E5E)
    val MUTED_LEAF_SELECTED_NIGHT = Color(0xFFA9A9A9)
    val MUTED_LINE_SELECTED_NIGHT = Color(0xFFA9a9a9)
    val MUTED_LINE_COLOR_NIGHT = Color(0xFF454545)
    val MUTED_SECONDARY_NIGHT = Color(0xFF929292)

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
    ): Pair<Color, Color> {
        val event_color_base = if (is_muted) {
            if (dark_mode) {
                MUTED_LEAF_COLOR_NIGHT
            } else {
                MUTED_LEAF_COLOR
            }
        } else {
            line_palette.event ?: channel_palette.event ?: LEAF_COLOR
        }

        val effect_color_base = if (is_muted) {
            if (dark_mode) {
                MUTED_LEAF_COLOR_NIGHT
            } else {
                MUTED_LEAF_COLOR
            }
        } else {
            EFFECT_COLOR
        }

        // val event_bg_color_base = line_palette.event_bg ?: channel_palette.event_bg ?: if (dark_mode) LINE_COLOR_NIGHT else LINE_COLOR
        // val effect_bg_color_base = line_palette.effect_bg ?: channel_palette.effect_bg ?: if (dark_mode) EFFECT_LINE_COLOR_NIGHT else EFFECT_LINE_COLOR

        var primary_base = if (is_effect_line) {
            when (active) {
                LeafState.Active -> effect_color_base
                LeafState.Spill -> {
                    if ((effect_color_base.red + effect_color_base.green + effect_color_base.blue) / 3F > .5F) {
                        Color(
                            red = effect_color_base.red * .75F,
                            green = effect_color_base.green * .75F,
                            blue = effect_color_base.blue * .75F,
                            alpha = effect_color_base.alpha
                        )
                    } else {
                        Color(
                            red = min(1F, effect_color_base.red / .75F),
                            green = min(1F, effect_color_base.green / .75F),
                            blue = min(1F, effect_color_base.blue / .75F),
                            alpha = effect_color_base.alpha
                        )
                    }
                }
                LeafState.Empty -> null
            }
        } else {
            when (active) {
                LeafState.Active -> event_color_base
                LeafState.Spill -> {
                    Color(
                        red = event_color_base.red * .75F,
                        green = event_color_base.green * .75F,
                        blue = event_color_base.blue * .75F,
                        alpha = event_color_base.alpha
                    )
                }
                LeafState.Empty -> null
            }
        }

        // Null implies empty, we don't want to apply tint to empty leafs
        if (primary_base == null) {
            primary_base = if (!is_muted) {
                when (selected) {
                    LeafSelection.Primary -> LINE_SELECTED
                    LeafSelection.Secondary -> {
                        if (dark_mode) {
                            LINE_COLOR_SECONDARY_NIGHT
                        } else {
                            LINE_COLOR_SECONDARY
                        }
                    }
                    LeafSelection.Unselected -> {
                        if (dark_mode) {
                            if (is_effect_line) {
                                EFFECT_LINE_COLOR_NIGHT
                            } else {
                                LINE_COLOR_NIGHT
                            }
                        } else if (is_effect_line) {
                            EFFECT_LINE_COLOR
                        } else {
                            LINE_COLOR
                        }
                    }
                }
            } else if (!dark_mode)  {
                when (selected) {
                    LeafSelection.Secondary,
                    LeafSelection.Primary -> MUTED_LINE_SELECTED
                    LeafSelection.Unselected -> MUTED_LINE_COLOR
                }
            } else {
                when (selected) {
                    LeafSelection.Secondary,
                    LeafSelection.Primary -> MUTED_LINE_SELECTED_NIGHT
                    LeafSelection.Unselected -> MUTED_LINE_COLOR_NIGHT
                }
            }
        } else {
            when (selected) {
                LeafSelection.Unselected -> {}
                else -> {
                    primary_base = Color(
                        red = (primary_base.red * 1.3F).coerceIn(0F, 1F),
                        green = (primary_base.green * 1.3F).coerceIn(0F, 1F),
                        blue = (primary_base.blue * 1.3F).coerceIn(0F, 1F),
                    )
                }
            }
        }

        if (is_muted) {
            val avg = (primary_base.red + primary_base.green + primary_base.blue) / 3F
            primary_base = Color(
                red = avg,
                green = avg,
                blue = avg,
                alpha = primary_base?.alpha ?: 1F
            )
        }

        return Pair(
            primary_base,
            this.get_text(primary_base)
        )
    }
}