package com.qfs.pagan

import android.graphics.Color
import kotlinx.serialization.Serializable

//<item name="android:popupMenuStyle: "@style/PopupMenu"
//<item name="android:statusBarColor: "?attr/colorPrimaryVariant"
@Serializable
data class ColorPalette(
    val alias: String,
    val background: Int = Color.parseColor("#1d1d1d"),
    val lines: Int = Color.parseColor("#000000"),
    val leaf_invalid: Int = Color.parseColor("#000000"),
    val leaf_invalid_text: Int = Color.parseColor("#000000"),
    val selection: Int = Color.parseColor("#000000"),
    val leaf: Int = Color.parseColor("#aa00ff"),
    val leaf_text: Int = Color.parseColor("#000000"),
    val leaf_selected: Int = Color.parseColor("#000000"),
    val leaf_selected_text: Int = Color.parseColor("#000000"),
    val link_empty: Int = Color.parseColor("#000000"),
    val link_empty_selected: Int = Color.parseColor("#000000"),
    val link: Int = Color.parseColor("#000000"),
    val link_text: Int = Color.parseColor("#000000"),
    val link_selected: Int = Color.parseColor("#000000"),
    val link_selected_text: Int = Color.parseColor("#000000"),
    val label_selected: Int = Color.parseColor("#000000"),
    val label_selected_text: Int = Color.parseColor("#000000"),
    val channel_even: Int = Color.parseColor("#000000"),
    val channel_even_text: Int = Color.parseColor("#000000"),
    val channel_odd: Int = Color.parseColor("#000000"),
    val channel_odd_text: Int = Color.parseColor("#000000")

//    val colorPrimaryVariant: Int = Color.parseColor("#171717"),
//    val colorOnPrimary: Int = Color.parseColor("#EFEFEF"),
//    val colorSecondary: Int = Color.parseColor("@color/blue_sky"),
//    val colorSecondaryVariant: Int = Color.parseColor("@color/blue_dark"),
//    val colorOnSecondary: Int = Color.parseColor("#1A1a1a"),
//
//    val alert_fg: Int = Color.parseColor("@color/white"),
//    val drawer_bg: Int = Color.parseColor("#2D2D2D"),
//    val main_bg: Int = Color.parseColor("#2D2D2D"),
//    val main_fg: Int = Color.parseColor("#efefef"),
//    val label_fg: Int = Color.parseColor("@color/white"),
//    val label_fg_focused: Int = Color.parseColor("#000000"),
//    val popup_fg: Int = Color.parseColor("?attr/colorOnPrimary"),
//    val popup_bg: Int = Color.parseColor("?attr/colorPrimaryVariant"),
//    val context_menu_bg: Int = Color.parseColor("?attr/main_bg"),
//
//    val button_fg_default: Int = Color.parseColor("?attr/colorOnPrimary"),
//    val button_fg_pressed: Int = Color.parseColor("?attr/colorOnPrimary"),
//    val button_bg_default: Int = Color.parseColor("?attr/colorPrimary"),
//    val button_bg_pressed: Int = Color.parseColor("#433947"),
//    val button_stroke_color: Int = Color.parseColor("#3F3F3F"),
//
//    val leaf_fg_active: Int = Color.parseColor("?attr/colorOnSecondary"),
//    val leaf_fg_active_focused: Int = Color.parseColor("@color/white"),
//    val leaf_fg_active_pressed: Int = Color.parseColor("@color/white"),
//    val leaf_fg_active_focused_pressed: Int = Color.parseColor("?attr/colorOnSecondary"),
//
//    val leaf_bg_default: Int = Color.parseColor("?attr/main_bg"),
//    val leaf_bg_focused: Int = Color.parseColor("@color/blue_sky"),
//
//    val leaf_bg_invalid: Int = Color.parseColor("#aa4433"),
//    val leaf_bg_active: Int = Color.parseColor("@color/purple"),
//    val leaf_bg_active_focused: Int = Color.parseColor("@color/purple_plum"),
//
//    val leaf_bg_linked: Int = Color.parseColor("#B07419"),
//    val leaf_bg_focused_linked: Int = Color.parseColor("#D8944B"),
//    val leaf_bg_active_linked: Int = Color.parseColor("#A79C35"),
//    val leaf_bg_active_focused_linked: Int = Color.parseColor("#D8BA1D"),
//
//    val label_bg: Int = Color.parseColor("?attr/leaf_bg_default"),
//    val label_bg_channel_even: Int = Color.parseColor("?attr/leaf_bg_default"),
//    val label_bg_channel_odd: Int = Color.parseColor("#232323"),
//    val label_bg_focused: Int = Color.parseColor("?attr/leaf_bg_focused"),
//
//    val numberselector_bg_default: Int = Color.parseColor("?attr/button_bg_default"),
//    val numberselector_bg_second: Int = Color.parseColor("#575A62"),
//    val numberselector_fg_default: Int = Color.parseColor("?attr/button_fg_default"),
//    val numberselector_bg_selected: Int = Color.parseColor("?attr/leaf_bg_active"),
//    val numberselector_fg_selected: Int = Color.parseColor("?attr/leaf_fg_active"),
//
//    val colorIconFG: Int = Color.parseColor("@attr/button_fg_default"),
//    val table_lines: Int = Color.parseColor("#808080"),
//
//    val soundfont_warning_background: Int = Color.parseColor("@color/blue_sky"),
//    val soundfont_warning_frame: Int = Color.parseColor("@color/white"),
//    val soundfont_warning_text: Int = Color.parseColor("@color/white"),
//    val soundfont_warning_link: Int = Color.parseColor("@color/blue_dark"),
//    val drop_down_bg: Int = Color.parseColor("#FFFFFF"),
)