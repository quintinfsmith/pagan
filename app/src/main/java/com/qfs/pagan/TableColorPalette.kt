package com.qfs.pagan

import androidx.compose.ui.graphics.Color
import com.qfs.pagan.structure.Rational
import kotlin.math.min

class TableColorPalette {
    class Swatch(base: Long, selector_tint: Color = SELECTION_TINT) {
        companion object {
            val ratio_alternate = Rational(1, 4)
            val ratio_spill = Rational(2, 5)
            val ratio_empty = Rational(3, 1)
            val ratio_selected = Rational(3, 1)
            val ratio_secondary = Rational(4, 3)
            val mix_alternate = Color(188,205,184)
            val mix_empty = Color(255, 255, 255, 0x11)
            val mix_spill = Color(0xAA, 0xAA, 0xAA)
        }

        val default = Color(base)
        val alternate = TableColorPalette.mix_colors(mix_alternate, this.default, ratio_alternate)

        val default_spill = TableColorPalette.mix_colors(mix_spill, this.default, ratio_spill)
        val default_empty = this.default.copy(alpha = .2F)
        val default_selected = TableColorPalette.mix_colors(selector_tint, this.default, ratio_selected)
        val default_spill_selected = TableColorPalette.mix_colors(selector_tint, this.default_spill, ratio_selected)
        val default_empty_selected = TableColorPalette.mix_colors(selector_tint, this.default_empty, ratio_selected)
        val default_secondary = TableColorPalette.mix_colors(selector_tint, this.default, ratio_secondary)
        val default_spill_secondary = TableColorPalette.mix_colors(selector_tint, this.default_spill, ratio_secondary)
        val default_empty_secondary = TableColorPalette.mix_colors(selector_tint, this.default_empty, ratio_secondary)

        val alternate_spill = TableColorPalette.mix_colors(mix_spill, this.alternate, ratio_spill)
        val alternate_empty = this.alternate.copy(alpha = .2F)
        val alternate_selected = TableColorPalette.mix_colors(selector_tint, this.alternate, ratio_selected)
        val alternate_spill_selected = TableColorPalette.mix_colors(selector_tint, this.alternate_spill, ratio_selected)
        val alternate_empty_selected = TableColorPalette.mix_colors(selector_tint, this.alternate_empty, ratio_selected)
        val alternate_secondary = TableColorPalette.mix_colors(selector_tint, this.alternate, ratio_secondary)
        val alternate_spill_secondary = TableColorPalette.mix_colors(selector_tint, this.alternate_spill, ratio_secondary)
        val alternate_empty_secondary = TableColorPalette.mix_colors(selector_tint, this.alternate_empty, ratio_secondary)

        val text = if (((this.default.red + this.default.green + this.default.blue) / 3) > 0xAA) {
            Color(0xFF000000)
        } else {
            Color(0xFFFFFFFF)
        }
        val text_selected = if (((this.default_selected.red + this.default_selected.green + this.default_selected.blue) / 3) > 0xAA) {
            Color(0xFF000000)
        } else {
            Color(0xFFFFFFFF)
        }
        val text_secondary = if (((this.default_secondary.red + this.default_secondary.green + this.default_secondary.blue) / 3) > 0xAA) {
            Color(0xFF000000)
        } else {
            Color(0xFFFFFFFF)
        }

        fun get(line: Int, active: Boolean, selected: Boolean, secondary: Boolean, spill: Boolean): Pair<Color, Color> {
            return if (selected) {
                Pair(
                    if (line % 2 == 0) {
                        if (active) this.default_selected
                        else if (spill) this.default_spill_selected
                        else this.default_empty_selected
                    } else {
                        if (active) this.alternate_selected
                        else if (spill) this.alternate_spill_selected
                        else this.alternate_empty_selected
                    },
                    this.text_selected
                )
            } else if (secondary) {
                Pair(
                    if (line % 2 == 0) {
                        if (active) this.default_secondary
                        else if (spill) this.default_spill_secondary
                        else this.default_empty_secondary
                    } else {
                        if (active) this.alternate_secondary
                        else if (spill) this.alternate_spill_secondary
                        else this.alternate_empty_secondary
                    },
                    this.text_secondary
                )
            } else {
                Pair(
                    if (line % 2 == 0) {
                        if (active) this.default
                        else if (spill) this.default_spill
                        else this.default_empty
                    } else {
                        if (active) this.alternate
                        else if (spill) this.alternate_spill
                        else this.alternate_empty
                    },
                    this.text
                )
            }
        }
    }

    companion object {
        val SELECTION_TINT = Color(0xFF43a2fc)
        fun mix_colors(first: Color, second: Color, ratio: Rational): Color {
            val denominator = ratio.numerator + ratio.denominator
            val alpha = (first.alpha * ratio.numerator / denominator) + (second.alpha * ratio.denominator / denominator)
            val red = (first.red * ratio.numerator / denominator) + (second.red * ratio.denominator / denominator)
            val blue = (first.blue * ratio.numerator / denominator) + (second.blue * ratio.denominator / denominator)
            val green = (first.green * ratio.numerator / denominator) + (second.green * ratio.denominator / denominator)
            return Color(red, green, blue, alpha)
        }

        val DEFAULT_CHANNEL_COLORS = arrayOf(
            0xFF765bd5,
            0xFF9250a8,
        )

        val channel_swatches: Array<Swatch> = Array(DEFAULT_CHANNEL_COLORS.size) { Swatch(DEFAULT_CHANNEL_COLORS[it]) }

        val ctl_swatch = Swatch(0xFFBBAA00)
        val mute_swatch = Swatch(0xFFAAAAAA)

        fun get_channel_swatch(channel: Int): Swatch {
            return this.channel_swatches[channel % this.channel_swatches.size]
        }
    }
}