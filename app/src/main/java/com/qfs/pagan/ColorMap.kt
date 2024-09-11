package com.qfs.pagan

import android.graphics.Color

class ColorMap(initial_palette: HashMap<Palette, Int>? = null) {
    class InvalidColorException(msg: String): Exception(msg) {}
    enum class Palette {
        Background,
        Foreground,
        Lines,
        LeafInvalid,
        LeafInvalidText,
        LeafInvalidSelected,
        LeafInvalidSelectedText,
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
        ButtonText,
        ButtonAltText,
        TitleBar,
        TitleBarText,
        CtlLine,
        CtlLineSelection,
        CtlLineText,
        CtlLineSelectionText,
        CtlLeaf,
        CtlLeafText,
        CtlLeafSelected,
        CtlLeafSelectedText,
        //----(implicitly calculated)-----------------
        Spill,
        SpillLink,
        SecondarySelection,
        SecondarySelectionInvalid,
        SecondarySelectionLinkActive
    }
    private val _default = Color.parseColor("#FF00FF")
    private val _palette = HashMap<Palette, Int>()
    private var _palette_fallback = HashMap<Palette, Int>()
    var use_palette = false

    init {
        if (initial_palette != null) {
            this.set_palette(initial_palette)
        }
    }

    fun is_set():Boolean {
        return this._palette.isNotEmpty()
    }
    fun populate() {
        for ((k, v) in this._palette_fallback) {
            this._palette[k] = v
        }
    }
    fun unpopulate() {
        this._palette.clear()
    }
    fun get_palette(): HashMap<Palette, Int> {
        return this._palette
    }
    fun set_palette(palette: HashMap<Palette, Int>) {
        for ((k, v) in palette) {
            this._palette[k] = v
        }
        val calculated_colors = listOf(
            ColorMap.Palette.Spill,
            ColorMap.Palette.SpillLink,
            ColorMap.Palette.SecondarySelection,
            ColorMap.Palette.SecondarySelectionInvalid,
            ColorMap.Palette.SecondarySelectionLinkActive
        )
        for (key in calculated_colors) {
            this._palette[key] = this.calculate_color(key)
        }
    }

    fun set_fallback_palette(palette: HashMap<Palette, Int>) {
        this._palette_fallback = palette
        val calculated_colors = listOf(
            ColorMap.Palette.Spill,
            ColorMap.Palette.SpillLink,
            ColorMap.Palette.SecondarySelection,
            ColorMap.Palette.SecondarySelectionInvalid,
            ColorMap.Palette.SecondarySelectionLinkActive
        )
        for (key in calculated_colors) {
            this._palette_fallback[key] = this.calculate_color(key, true)
        }
    }

    operator fun get(key: Palette): Int {
        return if (this.use_palette && this._palette.containsKey(key)) {
            this._palette[key]!!
        } else {
            this._palette_fallback.getOrDefault(key, this._default)
        }
    }

    fun calculate_color(key: ColorMap.Palette, fallback: Boolean = false): Int {
        return when (key) {
            ColorMap.Palette.Spill -> {
                val col_leaf: Color = Color.valueOf(this[ColorMap.Palette.Leaf]!!)
                val col_empty: Color = Color.valueOf(this[ColorMap.Palette.ChannelEven]!!)
                Color.rgb(
                    ((col_leaf.red() * .7F) + (col_empty.red() * .3F)).toFloat(),
                    ((col_leaf.green() * .7F) + (col_empty.green() * .3F)).toFloat(),
                    ((col_leaf.blue() * .7F) + (col_empty.blue() * .3F)).toFloat()
                )
            }
            ColorMap.Palette.SpillLink -> {
                val col_link: Color = Color.valueOf(this[ColorMap.Palette.Link]!!)
                val col_link_empty: Color = Color.valueOf(this[ColorMap.Palette.LinkEmpty]!!)
                Color.rgb(
                    ((col_link.red() * .7F) + (col_link_empty.red() * .3F)).toFloat(),
                    ((col_link.green() * .7F) + (col_link_empty.green() * .3F)).toFloat(),
                    ((col_link.blue() * .7F) + (col_link_empty.blue() * .3F)).toFloat()
                )
            }
            ColorMap.Palette.SecondarySelection -> {
                val col_leaf_selection: Color = Color.valueOf(this[ColorMap.Palette.LeafSelected]!!)
                val col_selection: Color = Color.valueOf(this[ColorMap.Palette.Selection]!!)

                Color.rgb(
                    ((col_selection.red() * .5F) + (col_leaf_selection.red() * .5F)).toFloat(),
                    ((col_selection.green() * .5F) + (col_leaf_selection.green() * .5F)).toFloat(),
                    ((col_selection.blue() * .5F) + (col_leaf_selection.blue() * .5F)).toFloat()
                )
            }
            ColorMap.Palette.SecondarySelectionLinkActive -> {
                val col_link_selected: Color = Color.valueOf(this[ColorMap.Palette.LinkSelected]!!)
                val col_empty_link_selected: Color = Color.valueOf(this[ColorMap.Palette.LinkEmptySelected]!!)

                Color.rgb(
                    ((col_link_selected.red() * .5F) + (col_empty_link_selected.red() * .5F)).toFloat(),
                    ((col_link_selected.green() * .5F) + (col_empty_link_selected.green() * .5F)).toFloat(),
                    ((col_link_selected.blue() * .5F) + (col_empty_link_selected.blue() * .5F)).toFloat()
                )
            }
            ColorMap.Palette.SecondarySelectionInvalid -> {
                val col_leaf_invalid_selected = Color.valueOf(this[ColorMap.Palette.LeafInvalidSelected]!!)
                val col_leaf_invalid = Color.valueOf(this[ColorMap.Palette.LeafInvalid]!!)
                Color.rgb(
                    ((col_leaf_invalid_selected.red() * .7F) + (col_leaf_invalid.red() * .3F)).toFloat(),
                    ((col_leaf_invalid_selected.green() * .7F) + (col_leaf_invalid.green() * .3F)).toFloat(),
                    ((col_leaf_invalid_selected.blue() * .7F) + (col_leaf_invalid.blue() * .3F)).toFloat()
                )
            }
            else -> {
                throw InvalidColorException("$key is not a calculated color")
            }
        }
    }

    operator fun set(key: Palette, value: Int) {
        this._palette[key] = value

        when (key) {
            ColorMap.Palette.ChannelEven,
            ColorMap.Palette.Leaf -> {
                this._palette[ColorMap.Palette.Spill] = this.calculate_color(ColorMap.Palette.Spill)
            }
            ColorMap.Palette.Link,
            ColorMap.Palette.LinkEmpty -> {
                this._palette[ColorMap.Palette.SpillLink] = this.calculate_color(ColorMap.Palette.SpillLink)
            }
            ColorMap.Palette.Selection,
            ColorMap.Palette.LeafSelected -> {
                this._palette[ColorMap.Palette.SecondarySelection] = this.calculate_color(ColorMap.Palette.SecondarySelection)
            }
            ColorMap.Palette.LinkSelected,
            ColorMap.Palette.LinkEmptySelected -> {
                this._palette[ColorMap.Palette.SecondarySelectionLinkActive] = this.calculate_color(ColorMap.Palette.SecondarySelectionLinkActive)
            }
            ColorMap.Palette.LeafInvalidSelected,
            ColorMap.Palette.LeafInvalid -> {
                this._palette[ColorMap.Palette.SecondarySelectionInvalid] = this.calculate_color(ColorMap.Palette.SecondarySelectionInvalid)
            }
            else -> {}
        }
    }

    fun unset(key: Palette) {
        this._palette.remove(key)
    }
}
