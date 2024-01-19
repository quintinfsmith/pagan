package com.qfs.pagan

import kotlinx.serialization.Serializable

//<item name="android:popupMenuStyle: "@style/PopupMenu"
//<item name="android:statusBarColor: "?attr/colorPrimaryVariant"
@Serializable
data class ColorPalette(
    var alias: String,
    var background: Int = 0,
    var foreground: Int = 0,
    var lines: Int = 0,
    var leaf_invalid: Int = 0,
    var leaf_invalid_text: Int = 0,
    var selection: Int = 0,
    var leaf: Int = 0,
    var leaf_text: Int = 0,
    var leaf_selected: Int = 0,
    var leaf_selected_text: Int = 0,
    var link_empty: Int = 0,
    var link_empty_selected: Int = 0,
    var link: Int = 0,
    var link_text: Int = 0,
    var link_selected: Int = 0,
    var link_selected_text: Int = 0,
    var label_selected: Int = 0,
    var label_selected_text: Int = 0,
    var channel_even: Int = 0,
    var channel_even_text: Int = 0,
    var channel_odd: Int = 0,
    var channel_odd_text: Int = 0,
    var column_label: Int = 0,
    var column_label_text: Int = 0,
    var button: Int = 0,
    var button_alt: Int = 0,
    var button_selected: Int = 0,
    var button_text: Int = 0,
    var button_alt_text: Int = 0,
    var button_selected_text: Int = 0,
    var title_bar: Int = 0,
    var title_bar_text: Int = 0
)