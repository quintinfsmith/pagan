package com.qfs.pagan.ui.theme

import androidx.compose.ui.graphics.Color
import com.qfs.pagan.ui.theme.PaganColorScheme.Companion.Defaults
import kotlin.math.min

data class PaganColorScheme(
    // ------------------ Editor -----------------//
    val leaf: Color = Defaults.leaf,
    val effect: Color = Defaults.effect,
    val line: Color = Defaults.line,
    val effect_line: Color = Defaults.effect_line,
    val spill: (Color) -> Color = Defaults.spill,
    val selected_primary: (Boolean, Color) -> Color = Defaults.selected_primary,
    val selected_secondary: (Boolean, Color) -> Color = Defaults.selected_secondary,
    val muted: (Boolean, Color) -> Color = Defaults.muted,

    val table_accent: Color = Defaults.table_accent,
    val table_accent_foreground: Color = Defaults.table_accent_foreground,
    val button_line: Color = Defaults.button_line,
    val button_line_foreground: Color = Defaults.button_line_foreground,
    val button_line_selected: Color = Defaults.button_line_selected,
    val button_line_selected_foreground: Color = Defaults.button_line_selected_foreground,
    val button_column: Color = Defaults.button_column,
    val button_column_foreground: Color = Defaults.button_column_foreground,
    val button_column_selected: Color = Defaults.button_column_selected,
    val button_column_selected_foreground: Color = Defaults.button_column_selected_foreground,
    val channel_separator: Color = Defaults.channel_separator,
    val table_line: Color = Defaults.table_line,

    val shortcut: Color = Defaults.shortcut,
    val shortcut_foreground: Color = Defaults.shortcut_foreground,
    val shortcut_selected: Color = Defaults.shortcut_selected,
    val shortcut_selected_foreground: Color = Defaults.shortcut_selected_foreground,

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

    val note_picker: Color = Defaults.note_picker,
    val note_picker_foreground: Color = Defaults.note_picker_foreground,
    val note_picker_alt: Color = Defaults.note_picker_alt,
    val note_picker_alt_foreground: Color = Defaults.note_picker_alt_foreground,
    val note_picker_selected: Color = Defaults.note_picker_selected,
    val note_picker_selected_foreground: Color = Defaults.note_picker_selected_foreground,

    val slider_thumb: Color = Defaults.slider_thumb,
    val slider_tick: Color = Defaults.slider_tick,
    val slider_track: Color = Defaults.slider_track,
    val slider_tick_inactive: Color = Defaults.slider_tick_inactive,
    val slider_track_inactive: Color = Defaults.slider_track_inactive,

    val switch_thumb_checked: Color = Defaults.switch_thumb_checked,
    val switch_track_checked: Color = Defaults.switch_track_checked,
    val switch_border_checked: Color = Defaults.switch_border_checked,
    val switch_icon_checked: Color = Defaults.switch_icon_checked,
    val switch_thumb_unchecked: Color = Defaults.switch_thumb_unchecked,
    val switch_track_unchecked: Color = Defaults.switch_track_unchecked,
    val switch_border_unchecked: Color = Defaults.switch_border_unchecked,
    val switch_icon_unchecked: Color = Defaults.switch_icon_unchecked,

    val menu_item_selected: Color = Defaults.menu_item_selected,
    val menu_item_foreground_selected: Color = Defaults.menu_item_foreground_selected,
    val menu_item: Color = Defaults.menu_item,
    val menu_item_foreground: Color = Defaults.menu_item_foreground,

    val menu_background: Color = Defaults.menu_background,

    val text_selection_handle: Color = Defaults.text_selection_handle,
    val text_selection_background: Color = Defaults.text_selection_background,
    val text_focus_color: Color = Defaults.text_focus_color,
    val text_background_focused: Color = Defaults.text_background_focused,
    val text_background_unfocused: Color = Defaults.text_background_unfocused,

    val loading_indicator: Color = Defaults.loading_indicator,
    val tuning_table_item: Color = Defaults.tuning_table_item,

    val context_menu_background: Color = Defaults.context_menu_background,
    val context_menu_foreground: Color = Defaults.context_menu_foreground,

    val soundfont_warning: Color = Defaults.soundfont_warning,
    val soundfont_warning_border: Color = Defaults.soundfont_warning_border,
    val soundfont_warning_foreground: Color = Defaults.soundfont_warning_foreground,

    val number_picker_foreground: Color = Defaults.number_picker_foreground,
    val number_picker_background: Color = Defaults.number_picker_background,

    val wide_beat_slider_track: Color = Defaults.wide_beat_slider_track,
    val wide_beat_slider_background: Color = Defaults.wide_beat_slider_background,
    val wide_beat_slider_track_selected: Color = Defaults.wide_beat_slider_track_selected,
    val wide_beat_slider_background_selected: Color = Defaults.wide_beat_slider_background_selected
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
                context_menu_background = background,
                context_menu_foreground = foreground,

                table_accent = table_accent,
                table_accent_foreground = table_line,

                button_line = table_accent,
                button_line_foreground = table_line,

                button_column = table_accent,
                button_column_foreground = table_line,

                channel_separator = table_line,
                table_line = table_line,

                shortcut = table_accent,
                shortcut_foreground = table_line,

                note_picker = button,
                note_picker_foreground = button_foreground,
                note_picker_alt = Color(0xFF3d3d3d),
                note_picker_alt_foreground = Color(0xFFEFEFEF),

                menu_item = container,
                menu_item_foreground = foreground,
                menu_background = background,

                tuning_table_item = container,

                switch_thumb_checked = button_foreground,
                switch_track_checked = button,
                switch_border_checked = button,
                switch_icon_checked = button,
                switch_thumb_unchecked = button,
                switch_track_unchecked = background,
                switch_border_unchecked = button,
                switch_icon_unchecked = button_foreground,

                slider_thumb = button,
                slider_track = button,
                slider_tick = button,

                wide_beat_slider_track = table_line,
                wide_beat_slider_background = table_line.merge(table_accent, .3F),

                number_picker_background = container,
                number_picker_foreground = foreground,
                // ------------ Editor --------------------//

                line = Color(0xFF2D2D2D),
                effect_line = Color(0xFF000000)
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
                context_menu_background = color_a,
                context_menu_foreground = color_d,

                table_accent = color_b,
                table_accent_foreground = color_d,

                button_line = color_b,
                button_line_foreground = color_d,

                button_column = color_b,
                button_column_foreground = color_d,

                channel_separator = color_d,
                table_line = color_d,

                shortcut = color_b,
                shortcut_foreground = color_d,

                note_picker = color_b,
                note_picker_foreground = color_d,
                note_picker_alt = color_b,
                note_picker_alt_foreground = color_d,

                menu_item = color_b,
                menu_item_foreground = color_d,
                menu_background = color_a,

                tuning_table_item = color_b,

                switch_thumb_checked = color_d,
                switch_track_checked = color_b,
                switch_border_checked = color_b,
                switch_icon_checked = color_a,
                switch_thumb_unchecked = color_b,
                switch_track_unchecked = color_a,
                switch_border_unchecked = color_b,
                switch_icon_unchecked = color_c,

                slider_thumb = color_b,
                slider_track = color_c,
                slider_tick = color_b,
                // ------------ Editor --------------------//

                line = Color(0xFF2D2D2D),
                effect_line = Color(0xFF000000)
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
            val leaf: Color = Color(0xFF765bd5)
            val effect: Color = Color(0xFFCB9C20)
            val line: Color = Color(0xFFE0E0E0)
            val effect_line: Color = Color(0xFFFFFFFF)
            val spill: (Color) -> Color = { base_color ->
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
            val selected_primary: (Boolean, Color) -> Color = { is_empty, base_color ->
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
            val selected_secondary: (Boolean, Color) -> Color = selected_primary
            val muted: (Boolean, Color) -> Color = { is_empty, line_color ->
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

            val table_accent = Color(0xFFE6E0DD)
            val table_accent_foreground = Color(0xFF2D2D2D)
            val table_accent_selected = Color(0xFF5BA1D6)
            val TABLE_ACCENT_SELECTED_FOREGROUND = table_accent_foreground

            val button_line = table_accent
            val button_line_foreground = table_accent_foreground
            val button_line_selected = table_accent_selected
            val button_line_selected_foreground = TABLE_ACCENT_SELECTED_FOREGROUND

            val button_column = table_accent
            val button_column_foreground = table_accent_foreground
            val button_column_selected = table_accent_selected
            val button_column_selected_foreground = TABLE_ACCENT_SELECTED_FOREGROUND

            val channel_separator = Color(0xFF2D2D2D)
            val table_line = channel_separator

            val shortcut = table_accent
            val shortcut_foreground = table_accent_foreground
            val shortcut_selected = table_accent_selected
            val shortcut_selected_foreground = TABLE_ACCENT_SELECTED_FOREGROUND

            val note_picker = button
            val note_picker_foreground = button_foreground
            val note_picker_alt = Color(0xFFE2DFE7)
            val note_picker_alt_foreground = Color(0xFF372D40)
            val note_picker_selected = Color(0xFF5BA1D6)
            val note_picker_selected_foreground = Color(0xFF000000)

            val slider_thumb = button
            val slider_track = button
            val slider_tick = button
            val slider_track_inactive = Color(0xFFB3C1E6)
            val slider_tick_inactive = Color(0xFFB3C1E6)

            val switch_thumb_checked = button_foreground
            val switch_track_checked = button
            val switch_border_checked = button
            val switch_icon_checked = button
            val switch_thumb_unchecked = button
            val switch_track_unchecked = background
            val switch_border_unchecked = button
            val switch_icon_unchecked = button_foreground

            val menu_item = container
            val menu_item_foreground = foreground
            val menu_item_selected = Color(0xFF5BA1D6)
            val menu_item_foreground_selected = Color(0xFF2D2D2D)
            val menu_background = background

            val loading_indicator = menu_item_selected

            val text_background_focused = Color(0x00000000)
            val text_background_unfocused = Color(0x00000000)
            val text_focus_color = menu_item_selected
            val text_selection_handle = menu_item_selected
            val text_selection_background = Color(
                menu_item_selected.red,
                menu_item_selected.green,
                menu_item_selected.blue,
                alpha = .5f
            )

            val tuning_table_item = container
            val context_menu_background = background
            val context_menu_foreground = foreground

            val soundfont_warning = Color(0xFF5BA1D6)
            val soundfont_warning_border = Color(0xFFBCD3E6)
            val soundfont_warning_foreground = Color(0xFF000000)
            val number_picker_foreground = foreground
            val number_picker_background = container

            val wide_beat_slider_track = button_column_foreground
            val wide_beat_slider_background = button_column.merge(Color(0xFF888888))
            val wide_beat_slider_track_selected = button_column_selected_foreground
            val wide_beat_slider_background_selected = button_column_selected.merge(Color(0xFFFFFFFF))
        }
    }
}