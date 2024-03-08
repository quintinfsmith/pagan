package com.qfs.pagan

import android.graphics.Color

class ColorMap(initial_palette: HashMap<Palette, Int>? = null) {
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
        TitleBarText
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
    }

    fun set_fallback_palette(palette: HashMap<Palette, Int>) {
        this._palette_fallback = palette
    }

    operator fun get(key: Palette): Int {
        return if (this.use_palette && this._palette.containsKey(key)) {
            this._palette[key]!!
        } else {
            this._palette_fallback.getOrDefault(key, this._default)
        }
    }

    operator fun set(key: Palette, value: Int) {
        this._palette[key] = value
    }

    fun unset(key: Palette) {
        this._palette.remove(key)
    }
}
