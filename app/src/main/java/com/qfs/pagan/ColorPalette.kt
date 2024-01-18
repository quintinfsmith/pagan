package com.qfs.pagan

import kotlinx.serialization.Serializable

//<item name="android:popupMenuStyle: "@style/PopupMenu"
//<item name="android:statusBarColor: "?attr/colorPrimaryVariant"
@Serializable
data class ColorPalette(
    val alias: String,
    val background: Int = 0,
    val foreground: Int = 0,
    val lines: Int = 0,
    val leaf_invalid: Int = 0,
    val leaf_invalid_text: Int = 0,
    val selection: Int = 0,
    val leaf: Int = 0,
    val leaf_text: Int = 0,
    val leaf_selected: Int = 0,
    val leaf_selected_text: Int = 0,
    val link_empty: Int = 0,
    val link_empty_selected: Int = 0,
    val link: Int = 0,
    val link_text: Int = 0,
    val link_selected: Int = 0,
    val link_selected_text: Int = 0,
    val label_selected: Int = 0,
    val label_selected_text: Int = 0,
    val channel_even: Int = 0,
    val channel_even_text: Int = 0,
    val channel_odd: Int = 0,
    val channel_odd_text: Int = 0,
    val column_label: Int = 0,
    val column_label_text: Int = 0,
    val button: Int = 0,
    val button_alt: Int = 0,
    val button_selected: Int = 0,
    val button_text: Int = 0,
    val button_alt_text: Int = 0,
    val button_selected_text: Int = 0,
    val title_bar: Int = 0,
    val title_bar_text: Int = 0
)