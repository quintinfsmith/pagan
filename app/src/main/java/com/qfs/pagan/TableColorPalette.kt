package com.qfs.pagan

import androidx.compose.ui.graphics.Color

class TableColorPalette {
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

    companion object {
        val DEFAULT_CHANNEL_COLORS = arrayOf(
            Color(0xFF765bd5),
            Color(0xFF9250a8)
        )
        val BASE_SELECT = Color(0xFF0064FF)

        val channel_swatches = Array<Array<Color>>(this.DEFAULT_CHANNEL_COLORS.size) {
            MaterialColorCalculator.get(this.DEFAULT_CHANNEL_COLORS[it])
        }

        val mute_swatch = MaterialColorCalculator.get(Color(0xFFFFFFFF))
        val selected_swatch = MaterialColorCalculator.get(this.BASE_SELECT)
        //.val selected_swatch = arrayOf<Color>(
        //.    Color(0xFF0091FF), // Empty
        //.    Color(0xFF2499E1), // Empty Secondary
        //.    Color(0xFF2636b2), // Active
        //.    Color(0xFF4f5bbc), // Active Secondary
        //.    Color(0xFF385AF3), // Spill
        //.    Color(0xFF3147A8), // Spill Secondary
        //.)
        val ctl_swatch = MaterialColorCalculator.get(Color(0xFFFFD500))

        fun get_text(input: Color): Color {
            val avg = (input.red + input.green + input.blue) / 3F
            return if (avg > .5F) Color(0xFF000000)
                else  Color(0xFFFFFFFF)
        }


        fun get_ctl_color(active: LeafState, selected: LeafSelection, dark_mode: Boolean = false): Pair<Color, Color> {
            var background = when (selected) {
                LeafSelection.Primary -> {
                    val i = if (dark_mode) {
                        when (active) {
                            LeafState.Spill,
                            LeafState.Active -> 9
                            LeafState.Empty -> 11
                        }
                    } else {
                        when (active) {
                            LeafState.Spill,
                            LeafState.Active -> 6
                            LeafState.Empty -> 10
                        }
                    }
                    this.selected_swatch[i]
                }
                LeafSelection.Secondary -> {
                    val i = if (dark_mode) {
                        when (active) {
                            LeafState.Spill,
                            LeafState.Active -> 8
                            LeafState.Empty -> 10
                        }
                    } else {
                        when (active) {
                            LeafState.Spill,
                            LeafState.Active -> 7
                            LeafState.Empty -> 11
                        }
                    }
                    this.selected_swatch[i]
                }
                LeafSelection.Unselected -> {
                    val i = if (dark_mode) {
                        when (active) {
                            LeafState.Active -> 6
                            LeafState.Spill -> 5
                            LeafState.Empty -> 0
                        }
                    } else {
                        when (active) {
                            LeafState.Active -> 5
                            LeafState.Spill -> 6
                            LeafState.Empty -> 11
                        }
                    }
                    this.ctl_swatch[i]
                }
            }

            if (active == LeafState.Empty && dark_mode) {
                background = background.copy(alpha = .5F)
            }

            return Pair(background, this.get_text(background))
        }

        fun get_mute_color(active: LeafState, selected: LeafSelection, dark_mode: Boolean = false): Pair<Color, Color> {
            return Pair(Color(0xFFFF0000), Color(0xFF00FFFF))
        }

        fun get_std_color(channel: Int, line_offset: Int, active: LeafState, selected: LeafSelection, dark_mode: Boolean = false): Pair<Color, Color> {
            var background = when (selected) {
                LeafSelection.Primary -> {
                    val i = when (active) {
                        LeafState.Spill,
                        LeafState.Active -> 6
                        LeafState.Empty -> 9
                    }
                    this.selected_swatch[i]
                }
                LeafSelection.Secondary -> {
                    val i = when (active) {
                        LeafState.Spill,
                        LeafState.Active -> 7
                        LeafState.Empty -> 10
                    }
                    this.selected_swatch[i]
                }
                LeafSelection.Unselected -> {
                    val i = if (dark_mode) {
                        when (active) {
                            LeafState.Active -> 5
                            LeafState.Spill -> 4
                            LeafState.Empty -> 1
                        }
                    } else {
                        when (active) {
                            LeafState.Active -> 5
                            LeafState.Spill -> 7
                            LeafState.Empty -> 11
                        }
                    }
                    this.ctl_swatch[i]

                    val swatch = this.channel_swatches[channel % this.channel_swatches.size]
                    swatch[i + (line_offset % 2)]
                }
            }

            if (active == LeafState.Empty && dark_mode) {
                background = background.copy(alpha = .5F)
            }

            return Pair(background, this.get_text(background))
        }

    }
}