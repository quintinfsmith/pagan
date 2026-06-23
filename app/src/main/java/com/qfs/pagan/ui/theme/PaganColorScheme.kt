package com.qfs.pagan.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.min

data class PaganColorScheme(
    // ------------------ Editor -----------------//
    val LEAF_COLOR: Color = Defaults.LEAF_COLOR,
    val EFFECT_COLOR: Color = Defaults.EFFECT_COLOR,
    val LINE_COLOR: Color = Defaults.LINE_COLOR,
    val EFFECT_LINE_COLOR: Color = Defaults.EFFECT_LINE_COLOR,
    val SPILL: (Color) -> Color = Defaults.SPILL,
    val SELECTED_PRIMARY: (Boolean, Color) -> Color = Defaults.SELECTED_PRIMARY,
    val SELECTED_SECONDARY: (Boolean, Color) -> Color = Defaults.SELECTED_SECONDARY,
    val MUTED: (Boolean, Color) -> Color = Defaults.MUTED,

    val TABLE_ACCENT: Color = Defaults.TABLE_ACCENT,
    val TABLE_ACCENT_FOREGROUND: Color = Defaults.TABLE_ACCENT_FOREGROUND,
    val BUTTON_LINE: Color = Defaults.BUTTON_LINE,
    val BUTTON_LINE_FOREGROUND: Color = Defaults.BUTTON_LINE_FOREGROUND,
    val BUTTON_LINE_SELECTED: Color = Defaults.BUTTON_LINE_SELECTED,
    val BUTTON_LINE_SELECTED_FOREGROUND: Color = Defaults.BUTTON_LINE_SELECTED_FOREGROUND,
    val BUTTON_COLUMN: Color = Defaults.BUTTON_COLUMN,
    val BUTTON_COLUMN_FOREGROUND: Color = Defaults.BUTTON_COLUMN_FOREGROUND,
    val BUTTON_COLUMN_SELECTED: Color = Defaults.BUTTON_COLUMN_SELECTED,
    val BUTTON_COLUMN_SELECTED_FOREGROUND: Color = Defaults.BUTTON_COLUMN_SELECTED_FOREGROUND,
    val CHANNEL_SEPARATOR: Color = Defaults.CHANNEL_SEPARATOR,
    val TABLE_LINE: Color = Defaults.TABLE_LINE,

    val SHORTCUT: Color = Defaults.SHORTCUT,
    val SHORTCUT_FOREGROUND: Color = Defaults.SHORTCUT_FOREGROUND,
    val SHORTCUT_SELECTED: Color = Defaults.SHORTCUT_SELECTED,
    val SHORTCUT_SELECTED_FOREGROUND: Color = Defaults.SHORTCUT_SELECTED_FOREGROUND,

    //---------------- UI ----------- //
    val topbar: Color = Defaults.topbar,
    val topbar_text: Color = Defaults.topbar_foreground,
    val foreground: Color = Defaults.foreground,
    val background: Color = Defaults.background,
    val container: (Int) -> Color = Defaults.container,
    val button: Color = Defaults.button,
    val button_foreground: Color = Defaults.button_foreground,
    val button_disabled: Color = Defaults.button_disabled,
    val button_disabled_foreground: Color = Defaults.button_disabled_foreground,
    val NUMBER_SELECTOR: Color = Defaults.NUMBER_SELECTOR,
    val NUMBER_SELECTOR_FOREGROUND: Color = Defaults.NUMBER_SELECTOR_FOREGROUND,
    val NUMBER_SELECTOR_ALT: Color = Defaults.NUMBER_SELECTOR_ALT,
    val NUMBER_SELECTOR_ALT_FOREGROUND: Color = Defaults.NUMBER_SELECTOR_ALT_FOREGROUND,
    val NUMBER_SELECTOR_SELECTED: Color = Defaults.NUMBER_SELECTOR_SELECTED,
    val NUMBER_SELECTOR_SELECTED_FOREGROUND: Color = Defaults.NUMBER_SELECTOR_SELECTED_FOREGROUND,

    val SLIDER_THUMB: Color = Defaults.SLIDER_THUMB,
    val SLIDER_TICK: Color = Defaults.SLIDER_TICK,
    val SLIDER_TRACK: Color = Defaults.SLIDER_TRACK,
    val SLIDER_TICK_INACTIVE: Color = Defaults.SLIDER_TICK_INACTIVE,
    val SLIDER_TRACK_INACTIVE: Color = Defaults.SLIDER_TRACK_INACTIVE,

    val SWITCH_THUMB_CHECKED: Color = Defaults.SWITCH_THUMB_CHECKED,
    val SWITCH_TRACK_CHECKED: Color = Defaults.SWITCH_TRACK_CHECKED,
    val SWITCH_BORDER_CHECKED: Color = Defaults.SWITCH_BORDER_CHECKED,
    val SWITCH_ICON_CHECKED: Color = Defaults.SWITCH_ICON_CHECKED,
    val SWITCH_THUMB_UNCHECKED: Color = Defaults.SWITCH_THUMB_UNCHECKED,
    val SWITCH_TRACK_UNCHECKED: Color = Defaults.SWITCH_TRACK_UNCHECKED,
    val SWITCH_BORDER_UNCHECKED: Color = Defaults.SWITCH_BORDER_UNCHECKED,
    val SWITCH_ICON_UNCHECKED: Color = Defaults.SWITCH_ICON_UNCHECKED,

    val MENU_ITEM_SELECTED: Color = Defaults.MENU_ITEM_SELECTED,
    val MENU_ITEM_FOREGROUND_SELECTED: Color = Defaults.MENU_ITEM_FOREGROUND_SELECTED,

    val TEXT_SELECTION_HANDLE: Color = Defaults.TEXT_SELECTION_HANDLE,
    val TEXT_SELECTION_BACKGROUND: Color = Defaults.TEXT_SELECTION_BACKGROUND,
    val TEXT_FOCUS_COLOR: Color = Defaults.TEXT_FOCUS_COLOR,

    val LOADING_INDICATOR: Color = Defaults.LOADING_INDICATOR
) {
    companion object {
        object Defaults {
            // ----------- UI -------------------------//
            val topbar: Color = Color(0xFF372D40)
            val topbar_foreground: Color = Color(0xFFFFFFFF)
            val button: Color = topbar
            val button_foreground: Color = topbar_foreground
            val button_disabled: Color = Color(0x44372d40)
            val button_disabled_foreground = Color(0xCCFFFFFF)
            val background: Color = Color(0xFFEFEFEF)
            val foreground: Color = Color(0xFF2D2D2D)
            val container: (Int) -> Color = { depth ->
                Color(0xFFFFFFFF)
            }


            //-------------------Editor------------------------//
            val LEAF_COLOR: Color = Color(0xFF765bd5)
            val EFFECT_COLOR: Color = Color(0xFFCB9C20)
            val LINE_COLOR: Color = Color(0xFFE0E0E0)
            val EFFECT_LINE_COLOR: Color = Color(0xFFFFFFFF)
            val SPILL: (Color) -> Color = { base_color ->
                if ((base_color.red + base_color.green + base_color.blue) / 3F > .5F) {
                    Color(
                        red = base_color.red * .75F,
                        green = base_color.green * .75F,
                        blue = base_color.blue * .75F,
                        alpha = base_color.alpha
                    )
                } else {
                    Color(
                        red = min(1F, base_color.red / .75F),
                        green = min(1F, base_color.green / .75F),
                        blue = min(1F, base_color.blue / .75F),
                        alpha = base_color.alpha
                    )
                }
            }
            val SELECTED_PRIMARY: (Boolean, Color) -> Color = { is_empty, base_color ->
                val weight = .2F
                if (is_empty) {
                    val LINE_SELECTED = Color(0xFF5BA1D6)
                    Color(
                        red = (base_color.red * weight) + (LINE_SELECTED.red * (1F - weight)),
                        green = (base_color.green * weight) + (LINE_SELECTED.green * (1F - weight)),
                        blue = (base_color.blue * weight) + (LINE_SELECTED.blue * (1F - weight)),
                    )
                } else {
                    val SELECTION = Color(0xFF0033AA)
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
                    Pair(.5F, .4F)
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

            val TABLE_ACCENT = Color(0xFFE6E0DD)
            val TABLE_ACCENT_FOREGROUND = Color(0xFF2D2D2D)
            val TABLE_ACCENT_SELECTED = Color(0xFF5BA1D6)
            val TABLE_ACCENT_SELECTED_FOREGROUND = TABLE_ACCENT_FOREGROUND

            val BUTTON_LINE = TABLE_ACCENT
            val BUTTON_LINE_FOREGROUND = TABLE_ACCENT_FOREGROUND
            val BUTTON_LINE_SELECTED = TABLE_ACCENT_SELECTED
            val BUTTON_LINE_SELECTED_FOREGROUND = TABLE_ACCENT_SELECTED_FOREGROUND

            val BUTTON_COLUMN = TABLE_ACCENT
            val BUTTON_COLUMN_FOREGROUND = TABLE_ACCENT_FOREGROUND
            val BUTTON_COLUMN_SELECTED = TABLE_ACCENT_SELECTED
            val BUTTON_COLUMN_SELECTED_FOREGROUND = TABLE_ACCENT_SELECTED_FOREGROUND

            val CHANNEL_SEPARATOR = Color(0xFF2D2D2D)
            val TABLE_LINE = CHANNEL_SEPARATOR

            val SHORTCUT = TABLE_ACCENT
            val SHORTCUT_FOREGROUND = TABLE_ACCENT_FOREGROUND
            val SHORTCUT_SELECTED = TABLE_ACCENT_SELECTED
            val SHORTCUT_SELECTED_FOREGROUND = TABLE_ACCENT_SELECTED_FOREGROUND

            val NUMBER_SELECTOR = button
            val NUMBER_SELECTOR_FOREGROUND = button_foreground
            val NUMBER_SELECTOR_ALT = Color(0xFFDBD0E6)
            val NUMBER_SELECTOR_ALT_FOREGROUND = Color(0xFF2C2433)
            val NUMBER_SELECTOR_SELECTED = Color(0xFF5BA1D6)
            val NUMBER_SELECTOR_SELECTED_FOREGROUND = Color(0xFF000000)

            val SLIDER_THUMB = button
            val SLIDER_TRACK = button
            val SLIDER_TICK = button
            val SLIDER_TRACK_INACTIVE = Color(0xFFB3C1E6)
            val SLIDER_TICK_INACTIVE = Color(0xFFB3C1E6)

            val UNUSED = Color(0xFFFF00FF)

            val SWITCH_THUMB_CHECKED = button_foreground
            val SWITCH_TRACK_CHECKED = button
            val SWITCH_BORDER_CHECKED = button
            val SWITCH_ICON_CHECKED = button
            val SWITCH_THUMB_UNCHECKED = button
            val SWITCH_TRACK_UNCHECKED = button_foreground
            val SWITCH_BORDER_UNCHECKED = button
            val SWITCH_ICON_UNCHECKED = button_foreground

            val MENU_ITEM_SELECTED = Color(0xFF5BA1D6)
            val MENU_ITEM_FOREGROUND_SELECTED = Color(0xFF2D2D2D)

            val LOADING_INDICATOR = MENU_ITEM_SELECTED

            val TEXT_FOCUS_COLOR = MENU_ITEM_SELECTED
            val TEXT_SELECTION_HANDLE = MENU_ITEM_SELECTED
            val TEXT_SELECTION_BACKGROUND = Color(
                MENU_ITEM_SELECTED.red,
                MENU_ITEM_SELECTED.green,
                MENU_ITEM_SELECTED.blue,
                alpha = .5f
            )
        }
    }
}