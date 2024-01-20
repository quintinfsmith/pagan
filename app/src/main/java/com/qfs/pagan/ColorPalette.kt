package com.qfs.pagan

import android.graphics.Color
enum class Palette {
    Background,
    Foreground,
    Lines,
    LeafInvalid,
    LeafInvalidText,
    Selection,
    SelectionText,
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
    private val default = Color.parseColor("#FF00FF")
    private val palette = HashMap<Palette, Int>()
    init {
        if (initial_palette != null) {
            for ((k, v) in initial_palette) {
                this.palette[k] = v
            }
        }
    }

    fun is_set():Boolean {
        return this.palette.isNotEmpty()
    }
    fun populate() {
        val palette = if (this.activity.is_night_mode()) {
            this._palette_night
        } else {
            this._palette_day
        }
        for ((k, v) in palette) {
            this.palette[k] = v
        }
    }
    fun unpopulate() {
        this.palette.clear()
    }
    fun get_palette(): HashMap<Palette, Int> {
        return this.palette
    }

    operator fun get(key: Palette): Int {
        return if (this.activity.configuration.use_palette && this.palette.containsKey(key)) {
            this.palette[key]!!
        } else if (this.activity.is_night_mode()) {
            this._palette_night.getOrDefault(key, this.default)
        } else {
            this._palette_day.getOrDefault(key, this.default)
        }
    }

    operator fun set(key: Palette, value: Int) {
        this.palette[key] = value
    }

    fun unset(key: Palette) {
        this.palette.remove(key)
    }

    private val _palette_night = hashMapOf<Palette, Int>(
        Pair(Palette.Background, this.activity.getColor(R.color.dark_main_bg)),
        Pair(Palette.Foreground, this.activity.getColor(R.color.dark_main_fg)),
        Pair(Palette.Lines, this.activity.getColor(R.color.dark_table_lines)),
        Pair(Palette.Leaf, this.activity.getColor(R.color.leaf)),
        Pair(Palette.LeafText, this.activity.getColor(R.color.leaf_text)),
        Pair(Palette.LeafInvalid, this.activity.getColor(R.color.leaf_invalid)),
        Pair(Palette.LeafInvalidText, this.activity.getColor(R.color.leaf_invalid_text)),
        Pair(Palette.LeafSelected, this.activity.getColor(R.color.leaf_selected)),
        Pair(Palette.LeafSelectedText, this.activity.getColor(R.color.leaf_selected_text)),
        Pair(Palette.Link, this.activity.getColor(R.color.leaf_linked)),
        Pair(Palette.LinkText, this.activity.getColor(R.color.leaf_linked_text)),
        Pair(Palette.LinkSelected, this.activity.getColor(R.color.leaf_linked_selected)),
        Pair(Palette.LinkSelectedText, this.activity.getColor(R.color.leaf_linked_selected_text)),
        Pair(Palette.LinkEmpty, this.activity.getColor(R.color.empty_linked)),
        Pair(Palette.LinkEmptySelected, this.activity.getColor(R.color.empty_linked_selected)),
        Pair(Palette.Selection, this.activity.getColor(R.color.empty_selected)),
        Pair(Palette.SelectionText, this.activity.getColor(R.color.empty_selected_text)),
        Pair(Palette.ChannelEven, this.activity.getColor(R.color.dark_channel_even)),
        Pair(Palette.ChannelEvenText, this.activity.getColor(R.color.dark_channel_even_text)),
        Pair(Palette.ChannelOdd, this.activity.getColor(R.color.dark_channel_odd)),
        Pair(Palette.ChannelOddText, this.activity.getColor(R.color.dark_channel_odd_text)),
        Pair(Palette.ColumnLabel, this.activity.getColor(R.color.dark_main_bg)),
        Pair(Palette.ColumnLabelText, this.activity.getColor(R.color.dark_main_fg)),
        Pair(Palette.Button, this.activity.getColor(R.color.dark_button)),
        Pair(Palette.ButtonText, this.activity.getColor(R.color.dark_button_text)),
        Pair(Palette.ButtonAlt, this.activity.getColor(R.color.dark_button_alt)),
        Pair(Palette.ButtonAltText, this.activity.getColor(R.color.dark_button_alt_text)),
        Pair(Palette.ButtonSelected, this.activity.getColor(R.color.dark_button_selected)),
        Pair(Palette.ButtonSelectedText, this.activity.getColor(R.color.dark_button_selected_text)),
        Pair(Palette.TitleBar, this.activity.getColor(R.color.dark_primary)),
        Pair(Palette.TitleBarText, this.activity.getColor(R.color.dark_primary_text))
    )

    private val _palette_day = hashMapOf<Palette, Int>(
        Pair(Palette.Background, this.activity.getColor(R.color.light_main_bg)),
        Pair(Palette.Foreground, this.activity.getColor(R.color.light_main_fg)),
        Pair(Palette.Lines, this.activity.getColor(R.color.light_table_lines)),
        Pair(Palette.Leaf, this.activity.getColor(R.color.leaf)),
        Pair(Palette.LeafText, this.activity.getColor(R.color.leaf_text)),
        Pair(Palette.LeafInvalid, this.activity.getColor(R.color.leaf_invalid)),
        Pair(Palette.LeafInvalidText, this.activity.getColor(R.color.leaf_invalid_text)),
        Pair(Palette.LeafSelected, this.activity.getColor(R.color.leaf_selected)),
        Pair(Palette.LeafSelectedText, this.activity.getColor(R.color.leaf_selected_text)),
        Pair(Palette.Link, this.activity.getColor(R.color.leaf_linked)),
        Pair(Palette.LinkText, this.activity.getColor(R.color.leaf_linked_text)),
        Pair(Palette.LinkSelected, this.activity.getColor(R.color.leaf_linked_selected_text)),
        Pair(Palette.LinkSelectedText, this.activity.getColor(R.color.leaf_linked_selected_text)),
        Pair(Palette.LinkEmpty, this.activity.getColor(R.color.empty_linked)),
        Pair(Palette.LinkEmptySelected, this.activity.getColor(R.color.empty_linked_selected)),
        Pair(Palette.Selection, this.activity.getColor(R.color.empty_selected)),
        Pair(Palette.SelectionText, this.activity.getColor(R.color.empty_selected_text)),
        Pair(Palette.ChannelEven, this.activity.getColor(R.color.light_channel_even)),
        Pair(Palette.ChannelEvenText, this.activity.getColor(R.color.light_channel_even_text)),
        Pair(Palette.ChannelOdd, this.activity.getColor(R.color.light_channel_odd)),
        Pair(Palette.ChannelOddText, this.activity.getColor(R.color.light_channel_odd_text)),
        Pair(Palette.ColumnLabel, this.activity.getColor(R.color.light_main_bg)),
        Pair(Palette.ColumnLabelText, this.activity.getColor(R.color.light_main_fg)),
        Pair(Palette.Button, this.activity.getColor(R.color.light_button)),
        Pair(Palette.ButtonText, this.activity.getColor(R.color.light_button_text)),
        Pair(Palette.ButtonAlt, this.activity.getColor(R.color.light_button_alt)),
        Pair(Palette.ButtonAltText, this.activity.getColor(R.color.light_button_alt_text)),
        Pair(Palette.ButtonSelected, this.activity.getColor(R.color.light_button_selected)),
        Pair(Palette.ButtonSelectedText, this.activity.getColor(R.color.light_button_selected_text)),
        Pair(Palette.TitleBar, this.activity.getColor(R.color.light_primary)),
        Pair(Palette.TitleBarText, this.activity.getColor(R.color.light_primary_text))
    )
}
