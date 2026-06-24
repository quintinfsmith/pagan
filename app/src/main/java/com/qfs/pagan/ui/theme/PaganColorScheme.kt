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
    val scrim: Color = Defaults.scrim,
    val container: Color = Defaults.container,
    val container_border: Color = Defaults.container_border,
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
    val MENU_ITEM: Color = Defaults.MENU_ITEM,
    val MENU_ITEM_FOREGROUND: Color = Defaults.MENU_ITEM_FOREGROUND,

    val MENU_BACKGROUND: Color = Defaults.MENU_BACKGROUND,

    val TEXT_SELECTION_HANDLE: Color = Defaults.TEXT_SELECTION_HANDLE,
    val TEXT_SELECTION_BACKGROUND: Color = Defaults.TEXT_SELECTION_BACKGROUND,
    val TEXT_FOCUS_COLOR: Color = Defaults.TEXT_FOCUS_COLOR,
    val TEXT_BACKGROUND_FOCUSED: Color = Defaults.TEXT_BACKGROUND_FOCUSED,
    val TEXT_BACKGROUND_UNFOCUSED: Color = Defaults.TEXT_BACKGROUND_UNFOCUSED,

    val LOADING_INDICATOR: Color = Defaults.LOADING_INDICATOR,
    val TUNING_TABLE_ITEM: Color = Defaults.TUNING_TABLE_ITEM,

    val CONTEXT_MENU_BACKGROUND: Color = Defaults.CONTEXT_MENU_BACKGROUND,
    val CONTEXT_MENU_FOREGROUND: Color = Defaults.CONTEXT_MENU_FOREGROUND,

    val SOUNDFONT_WARNING: Color = Defaults.SOUNDFONT_WARNING,
    val SOUNDFONT_WARNING_BORDER: Color = Defaults.SOUNDFONT_WARNING_BORDER,
    val SOUNDFONT_WARNING_FOREGROUND: Color = Defaults.SOUNDFONT_WARNING_FOREGROUND,

    val NUMBERPICKER_FOREGROUND: Color = Defaults.NUMBERPICKER_FOREGROUND,
    val NUMBERPICKER_BACKGROUND: Color = Defaults.NUMBERPICKER_BACKGROUND,

    val WIDE_BEAT_SLIDER_TRACK: Color = Defaults.WIDE_BEAT_SLIDER_TRACK,
    val WIDE_BEAT_SLIDER_BACKGROUND: Color = Defaults.WIDE_BEAT_SLIDER_BACKGROUND,
    val WIDE_BEAT_SLIDER_TRACK_SELECTED: Color = Defaults.WIDE_BEAT_SLIDER_TRACK_SELECTED,
    val WIDE_BEAT_SLIDER_BACKGROUND_SELECTED: Color = Defaults.WIDE_BEAT_SLIDER_BACKGROUND_SELECTED
) {
    companion object {
        fun Dark(): PaganColorScheme {
            val background = Color(0xFF1D1D1D)
            val foreground = Color(0xFFE6E4E3)
            val table_line = Color(0xFFe6ded9)
            val table_accent = Color(0xFF3d3d3d)
            val button = Color(0xFFACACAC)
            val button_foreground = Color(0xFF2D2D2D)
            val container = Color(0xFF2D2D2D)

            return PaganColorScheme(
                topbar = container,
                topbar_text = foreground,
                background = background,
                foreground = foreground,
                button = button,
                button_foreground = button_foreground,
                button_disabled = button.merge(background, .3f),
                button_disabled_foreground = button_foreground.merge(background, .3f),

                container = container,
                container_border = foreground,
                CONTEXT_MENU_BACKGROUND = background,
                CONTEXT_MENU_FOREGROUND = foreground,

                TABLE_ACCENT = table_accent,
                TABLE_ACCENT_FOREGROUND = table_line,

                BUTTON_LINE = table_accent,
                BUTTON_LINE_FOREGROUND = table_line,

                BUTTON_COLUMN = table_accent,
                BUTTON_COLUMN_FOREGROUND = table_line,

                CHANNEL_SEPARATOR = table_line,
                TABLE_LINE = table_line,

                SHORTCUT = table_accent,
                SHORTCUT_FOREGROUND = table_line,

                NUMBER_SELECTOR = button,
                NUMBER_SELECTOR_FOREGROUND = button_foreground,
                NUMBER_SELECTOR_ALT = Color(0xFF3d3d3d),
                NUMBER_SELECTOR_ALT_FOREGROUND = Color(0xFFEFEFEF),

                MENU_ITEM = container,
                MENU_ITEM_FOREGROUND = foreground,
                MENU_BACKGROUND = background,

                TUNING_TABLE_ITEM = container,

                SWITCH_THUMB_CHECKED = button_foreground,
                SWITCH_TRACK_CHECKED = button,
                SWITCH_BORDER_CHECKED = button,
                SWITCH_ICON_CHECKED = button,
                SWITCH_THUMB_UNCHECKED = button,
                SWITCH_TRACK_UNCHECKED = background,
                SWITCH_BORDER_UNCHECKED = button,
                SWITCH_ICON_UNCHECKED = button_foreground,

                SLIDER_THUMB = button,
                SLIDER_TRACK = button,
                SLIDER_TICK = button,
                // ------------ Editor --------------------//

                LINE_COLOR = Color(0xFF2D2D2D),
                EFFECT_LINE_COLOR = Color(0xFF000000)
            )
        }

        fun FourColor(): PaganColorScheme {
            val color_a = Color(0xFF0a1e0a)
            val color_b = Color(0xFF466446)
            val color_c = Color(0xFF96c896)
            val color_d = Color(0xFFFFFFFF)

            return PaganColorScheme(
                topbar = color_a,
                topbar_text = color_c,
                background = color_a,
                foreground = color_d,
                button = color_b,
                button_foreground = color_a,
                button_disabled = color_c,
                button_disabled_foreground = color_d,

                container = color_a,
                container_border = color_c,
                CONTEXT_MENU_BACKGROUND = color_a,
                CONTEXT_MENU_FOREGROUND = color_d,

                TABLE_ACCENT = color_b,
                TABLE_ACCENT_FOREGROUND = color_d,

                BUTTON_LINE = color_b,
                BUTTON_LINE_FOREGROUND = color_d,

                BUTTON_COLUMN = color_b,
                BUTTON_COLUMN_FOREGROUND = color_d,

                CHANNEL_SEPARATOR = color_d,
                TABLE_LINE = color_d,

                SHORTCUT = color_b,
                SHORTCUT_FOREGROUND = color_d,

                NUMBER_SELECTOR = color_b,
                NUMBER_SELECTOR_FOREGROUND = color_d,
                NUMBER_SELECTOR_ALT = color_b,
                NUMBER_SELECTOR_ALT_FOREGROUND = color_d,

                MENU_ITEM = color_b,
                MENU_ITEM_FOREGROUND = color_d,
                MENU_BACKGROUND = color_a,

                TUNING_TABLE_ITEM = color_b,

                SWITCH_THUMB_CHECKED = color_d,
                SWITCH_TRACK_CHECKED = color_b,
                SWITCH_BORDER_CHECKED = color_b,
                SWITCH_ICON_CHECKED = color_a,
                SWITCH_THUMB_UNCHECKED = color_b,
                SWITCH_TRACK_UNCHECKED = color_a,
                SWITCH_BORDER_UNCHECKED = color_b,
                SWITCH_ICON_UNCHECKED = color_c,

                SLIDER_THUMB = color_b,
                SLIDER_TRACK = color_c,
                SLIDER_TICK = color_b,
                // ------------ Editor --------------------//

                LINE_COLOR = Color(0xFF2D2D2D),
                EFFECT_LINE_COLOR = Color(0xFF000000)
            )
        }

        object Defaults {
            val UNUSED = Color(0xFFFF00FF)
            // ----------- UI -------------------------//
            val topbar: Color = Color(0xFF372D40)
            val topbar_foreground: Color = Color(0xFFFFFFFF)
            val scrim: Color = Color(0xFF000000)
            val button: Color = topbar
            val button_foreground: Color = topbar_foreground
            val button_disabled: Color = Color(0x44372d40)
            val button_disabled_foreground = Color(0xCCFFFFFF)
            val background: Color = Color(0xFFEFEFEF)
            val foreground: Color = Color(0xFF2D2D2D)
            val container: Color = Color(0xFFFFFFFF)
            val container_border: Color = foreground

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
            val NUMBER_SELECTOR_ALT = Color(0xFF4D5058)
            val NUMBER_SELECTOR_ALT_FOREGROUND = Color(0xFFFFFFFF)
            val NUMBER_SELECTOR_SELECTED = Color(0xFF5BA1D6)
            val NUMBER_SELECTOR_SELECTED_FOREGROUND = Color(0xFF000000)

            val SLIDER_THUMB = button
            val SLIDER_TRACK = button
            val SLIDER_TICK = button
            val SLIDER_TRACK_INACTIVE = Color(0xFFB3C1E6)
            val SLIDER_TICK_INACTIVE = Color(0xFFB3C1E6)


            val SWITCH_THUMB_CHECKED = button_foreground
            val SWITCH_TRACK_CHECKED = button
            val SWITCH_BORDER_CHECKED = button
            val SWITCH_ICON_CHECKED = button
            val SWITCH_THUMB_UNCHECKED = button
            val SWITCH_TRACK_UNCHECKED = background
            val SWITCH_BORDER_UNCHECKED = button
            val SWITCH_ICON_UNCHECKED = button_foreground

            val MENU_ITEM = container
            val MENU_ITEM_FOREGROUND = foreground
            val MENU_ITEM_SELECTED = Color(0xFF5BA1D6)
            val MENU_ITEM_FOREGROUND_SELECTED = Color(0xFF2D2D2D)
            val MENU_BACKGROUND = background

            val LOADING_INDICATOR = MENU_ITEM_SELECTED

            val TEXT_BACKGROUND_FOCUSED = Color(0x00000000)
            val TEXT_BACKGROUND_UNFOCUSED = Color(0x00000000)
            val TEXT_FOCUS_COLOR = MENU_ITEM_SELECTED
            val TEXT_SELECTION_HANDLE = MENU_ITEM_SELECTED
            val TEXT_SELECTION_BACKGROUND = Color(
                MENU_ITEM_SELECTED.red,
                MENU_ITEM_SELECTED.green,
                MENU_ITEM_SELECTED.blue,
                alpha = .5f
            )

            val TUNING_TABLE_ITEM = container
            val CONTEXT_MENU_BACKGROUND = background
            val CONTEXT_MENU_FOREGROUND = foreground

            val SOUNDFONT_WARNING = Color(0xFF5BA1D6)
            val SOUNDFONT_WARNING_BORDER = Color(0xFFBCD3E6)
            val SOUNDFONT_WARNING_FOREGROUND = Color(0xFF000000)
            val NUMBERPICKER_FOREGROUND = foreground
            val NUMBERPICKER_BACKGROUND = container

            val WIDE_BEAT_SLIDER_TRACK = BUTTON_COLUMN_FOREGROUND
            val WIDE_BEAT_SLIDER_BACKGROUND = BUTTON_COLUMN.merge(Color(0xFF888888))
            val WIDE_BEAT_SLIDER_TRACK_SELECTED = BUTTON_COLUMN_SELECTED_FOREGROUND
            val WIDE_BEAT_SLIDER_BACKGROUND_SELECTED = BUTTON_COLUMN_SELECTED.merge(Color(0xFFFFFFFF))
        }
    }
}