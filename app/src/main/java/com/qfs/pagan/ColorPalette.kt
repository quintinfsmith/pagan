package com.qfs.pagan

import android.graphics.Color
enum class Palette {
    Background,
    Foreground,
    Lines,
    LeafInvalid,
    LeafInvalidText,
    Selection,
    Leaf,
    LeafText,
    LeafSelected,
    LeafSelectedText,
    LinkEmpty,
    LinkEmptySelected,
    Link,
    LinkText,
    LinkSelected,
    LinkSelectedText,
    LabelSelected,
    LabelSelectedText,
    ChannelEven,
    ChannelEvenText,
    ChannelOdd,
    ChannelOddText,
    ColumnLabel,
    ColumnLabelText,
    Button,
    ButtonAlt,
    ButtonSelected,
    ButtonText,
    ButtonAltText,
    ButtonSelectedText,
    TitleBar,
    TitleBarText
}

class ColorMap(val activity: MainActivity, initial_palette: HashMap<Palette, Int>? = null) {
    private val default = Color.parseColor("#FFFFFF")
    private val palette = HashMap<Palette, Int>()
    init {
        if (initial_palette != null) {
            for ((k, v) in initial_palette) {
                this.palette[k] = v
            }
        }
    }
    operator fun get(key: Palette): Int {
        return this.palette.getOrDefault(
            key,
            if (this.activity.is_night_mode()) {
                this.palette_night.getOrDefault(key, this.default)
            } else {
                this.palette_day.getOrDefault(key, this.default)
            }
        )
    }

    operator fun set(key: Palette, value: Int) {
        this.palette[key] = value
    }

    fun unset(key: Palette) {
        this.palette.remove(key)
    }

    private val palette_night = hashMapOf<Palette, Int>(
        Pair(Palette.Background, R.color.dark_main_bg),
        Pair(Palette.Foreground, R.color.dark_main_fg),
        Pair(Palette.Lines, R.color.dark_table_lines),
        Pair(Palette.Leaf, R.color.leaf),
        Pair(Palette.LeafText, R.color.leaf_text),
        Pair(Palette.LeafSelected, R.color.leaf_selected),
        Pair(Palette.LeafSelectedText, R.color.leaf_selected_text),
        Pair(Palette.LeafInvalid, R.color.leaf_invalid),
        Pair(Palette.LeafInvalidText, R.color.leaf_invalid_text),
        Pair(Palette.Link, R.color.leaf_linked),
        Pair(Palette.LinkText, R.color.leaf_linked_text),
        Pair(Palette.LinkSelected, R.color.leaf_linked_selected),
        Pair(Palette.LinkSelectedText, R.color.leaf_linked_selected_text),
        Pair(Palette.LinkEmpty, R.color.empty_linked),
        Pair(Palette.LinkEmptySelected, R.color.empty_linked_selected),
        Pair(Palette.Selection, R.color.empty_selected),
        Pair(Palette.LabelSelected, R.color.empty_selected),
        Pair(Palette.LabelSelectedText, R.color.empty_selected_text),
        Pair(Palette.ChannelEven, R.color.dark_channel_even),
        Pair(Palette.ChannelEvenText, R.color.dark_channel_even_text),
        Pair(Palette.ChannelOdd, R.color.dark_channel_odd),
        Pair(Palette.ChannelOddText, R.color.dark_channel_odd_text),
        Pair(Palette.ColumnLabel, R.color.dark_main_bg),
        Pair(Palette.ColumnLabelText, R.color.dark_main_fg),
        Pair(Palette.Button, R.color.dark_button),
        Pair(Palette.ButtonText, R.color.dark_button_text),
        Pair(Palette.ButtonAlt, R.color.dark_button_alt),
        Pair(Palette.ButtonAltText, R.color.dark_button_alt_text),
        Pair(Palette.ButtonSelected, R.color.dark_button_selected),
        Pair(Palette.ButtonSelectedText, R.color.dark_button_selected_text),
        Pair(Palette.TitleBar, R.color.dark_primary),
        Pair(Palette.TitleBarText, R.color.dark_primary_text)
    )

    private val palette_day = hashMapOf<Palette, Int>(
        Pair(Palette.Background, R.color.light_main_bg),
        Pair(Palette.Foreground, R.color.light_main_fg),
        Pair(Palette.Lines, R.color.light_table_lines),
        Pair(Palette.Leaf, R.color.leaf),
        Pair(Palette.LeafText, R.color.leaf_text),
        Pair(Palette.LeafSelected, R.color.leaf_selected),
        Pair(Palette.LeafSelectedText, R.color.leaf_selected_text),
        Pair(Palette.Link, R.color.leaf_linked),
        Pair(Palette.LinkText, R.color.leaf_linked_text),
        Pair(Palette.LinkSelected, R.color.leaf_linked_selected_text),
        Pair(Palette.LinkEmpty, R.color.empty_linked),
        Pair(Palette.LinkEmptySelected, R.color.empty_linked_selected),
        Pair(Palette.Selection, R.color.empty_selected),
        Pair(Palette.LabelSelected, R.color.empty_selected),
        Pair(Palette.LabelSelectedText, R.color.empty_selected_text),
        Pair(Palette.ChannelEven, R.color.light_channel_even),
        Pair(Palette.ChannelEvenText, R.color.light_channel_even_text),
        Pair(Palette.ChannelOdd, R.color.light_channel_odd),
        Pair(Palette.ChannelOddText, R.color.light_channel_odd_text),
        Pair(Palette.ColumnLabel, R.color.light_main_bg),
        Pair(Palette.ColumnLabelText, R.color.light_main_fg),
        Pair(Palette.Button, R.color.light_button),
        Pair(Palette.ButtonText, R.color.light_button_text),
        Pair(Palette.ButtonAlt, R.color.light_button_alt),
        Pair(Palette.ButtonAltText, R.color.light_button_alt_text),
        Pair(Palette.ButtonSelected, R.color.light_button_selected),
        Pair(Palette.ButtonSelectedText, R.color.light_button_selected_text),
        Pair(Palette.TitleBar, R.color.light_primary),
        Pair(Palette.TitleBarText, R.color.light_primary_text)
    )
}
