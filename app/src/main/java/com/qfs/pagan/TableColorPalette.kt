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
        val LEAF_COLOR = Color(0xFF765bd5)
        val LEAF_COLOR_SPILL = Color(0xFF5944a3)
        val LEAF_COLOR_SELECTED = Color(0xFF2636b2)
        val LEAF_COLOR_SECONDARY = Color(0xFF4F5BBC)
        val LEAF_COLOR_INVALID = Color(0xFFe51C3A)
        val LEAF_COLOR_INVALID_SELECTED = Color(0xFF890E21)

        val EFFECT_COLOR = Color(0xFFCB9C10)
        val EFFECT_COLOR_SPILL = Color(0xFF886E20)
        val EFFECT_COLOR_SELECTED = Color(0xFF095660)
        val EFFECT_COLOR_SECONDARY = Color(0xFF567B80)

        val LINE_COLOR = Color(0xFFEFEFEF)
        val LINE_COLOR_NIGHT = Color(0xFF232323)
        val LINE_SELECTED = Color(0xFF5BA1D6)
        val LINE_COLOR_SECONDARY = Color(0x995Ba1D6)

        val MUTED_LEAF_COLOR = Color(0xFF6E6E6E)
        val MUTED_LEAF_SPILL = Color(0xFF53533)
        val MUTED_LEAF_SELECTED = Color(0xFF444444)
        val MUTED_LEAF_SECONDARY = Color(0xFF626262)
        val MUTED_LINE_SELECTED = Color(0xFF999999)
        val MUTED_LINE_COLOR = Color(0xFF454545)
        val MUTED_SECONDARY = Color(0xFF929292)

        val MUTED_LEAF_COLOR_NIGHT = Color(0xFF7E7E7E)
        val MUTED_LEAF_SPILL_NIGHT = Color(0xFF7e7e7e)
        val MUTED_LEAF_SELECTED_NIGHT = Color(0xFFA9A9A9)
        val MUTED_LEAF_SECONDARY_NIGHT = Color(0xFF929292)
        val MUTED_LINE_SELECTED_NIGHT = Color(0xFFA9a9a9)
        val MUTED_LINE_COLOR_NIGHT = Color(0xFF454545)
        val MUTED_SECONDARY_NIGHT = Color(0xFF929292)


        //<color name="ctl_leaf_text">#FF000000</color>
        //<color name="ctl_leaf_selected_text">#FFFFFF</color>
        // <color name="leaf_text">#000000</color>
        // <color name="leaf_selected_text">#FFFFFF</color>
        // <color name="leaf_secondary_text">#000000</color>
        // <color name="leaf_invalid">#E51C3A</color>
        // <color name="leaf_invalid_text">#FFFFFF</color>
        // <color name="leaf_invalid_selected_text">#A27E7E</color>




        fun get_text(input: Color): Color {
            val avg = (input.red + input.green + input.blue) / 3F
            return if (avg > .4F) Color(0xFF000000)
            else  Color(0xFFFFFFFF)
        }

        fun get_color(
            active: LeafState,
            selected: LeafSelection,
            is_effect_line: Boolean,
            is_muted: Boolean,
            dark_mode: Boolean = false
        ): Pair<Color, Color> {
            val background = if (is_muted) {
                if (dark_mode) {
                    when (active) {
                        LeafState.Active,
                        LeafState.Spill -> {
                            when (selected) {
                                LeafSelection.Primary -> this.MUTED_LEAF_SELECTED_NIGHT
                                LeafSelection.Secondary -> this.MUTED_LEAF_SECONDARY_NIGHT
                                LeafSelection.Unselected -> this.MUTED_LEAF_COLOR_NIGHT
                            }
                        }
                        LeafState.Empty -> {
                            when (selected) {
                                LeafSelection.Primary -> this.MUTED_LINE_SELECTED_NIGHT
                                LeafSelection.Secondary -> this.MUTED_LINE_SELECTED_NIGHT
                                LeafSelection.Unselected -> this.MUTED_LINE_COLOR_NIGHT
                            }
                        }
                    }
                } else {
                    when (active) {
                        LeafState.Active,
                        LeafState.Spill -> {
                            when (selected) {
                                LeafSelection.Primary -> this.MUTED_LEAF_SELECTED
                                LeafSelection.Secondary -> this.MUTED_LEAF_SECONDARY
                                LeafSelection.Unselected -> this.MUTED_LEAF_COLOR
                            }
                        }
                        LeafState.Empty -> {
                            when (selected) {
                                LeafSelection.Primary -> this.MUTED_LINE_SELECTED
                                LeafSelection.Secondary -> this.MUTED_LINE_SELECTED // TODO?
                                LeafSelection.Unselected -> this.MUTED_LINE_COLOR
                            }
                        }
                    }
                }
            } else if (is_effect_line) {
                when (active) {
                    LeafState.Active -> {
                        when (selected) {
                            LeafSelection.Primary -> this.EFFECT_COLOR_SELECTED
                            LeafSelection.Secondary -> this.EFFECT_COLOR_SECONDARY
                            LeafSelection.Unselected -> this.EFFECT_COLOR
                        }
                    }
                    LeafState.Spill -> {
                        when (selected) {
                            LeafSelection.Primary -> this.EFFECT_COLOR_SELECTED
                            LeafSelection.Secondary -> this.EFFECT_COLOR_SECONDARY
                            LeafSelection.Unselected -> this.EFFECT_COLOR_SPILL
                        }
                    }
                    LeafState.Empty -> {
                        if (dark_mode) {
                            when (selected) {
                                LeafSelection.Primary -> this.LINE_SELECTED
                                LeafSelection.Secondary -> this.LINE_COLOR_SECONDARY
                                LeafSelection.Unselected -> this.LINE_COLOR_NIGHT
                            }
                        } else {
                            when (selected) {
                                LeafSelection.Primary -> this.LINE_SELECTED
                                LeafSelection.Secondary -> this.LINE_COLOR_SECONDARY
                                LeafSelection.Unselected -> this.LINE_COLOR
                            }
                        }
                    }
                }
            } else {
                when (active) {
                    LeafState.Active -> {
                        when (selected) {
                            LeafSelection.Primary -> this.LEAF_COLOR_SELECTED
                            LeafSelection.Secondary -> this.LEAF_COLOR_SECONDARY
                            LeafSelection.Unselected -> this.LEAF_COLOR
                        }
                    }
                    LeafState.Spill -> {
                        when (selected) {
                            LeafSelection.Primary -> this.LEAF_COLOR_SELECTED
                            LeafSelection.Secondary -> this.LEAF_COLOR_SECONDARY
                            LeafSelection.Unselected -> this.LEAF_COLOR_SPILL
                        }
                    }
                    LeafState.Empty -> {
                        if (dark_mode) {
                            when (selected) {
                                LeafSelection.Primary -> this.LINE_SELECTED
                                LeafSelection.Secondary -> this.LINE_COLOR_SECONDARY
                                LeafSelection.Unselected -> this.LINE_COLOR_NIGHT
                            }
                        } else {
                            when (selected) {
                                LeafSelection.Primary -> this.LINE_SELECTED
                                LeafSelection.Secondary -> this.LINE_COLOR_SECONDARY
                                LeafSelection.Unselected -> this.LINE_COLOR
                            }
                        }
                    }
                }
            }

            return Pair(
                background,
                this.get_text(background)
            )
        }
    }
}