package com.qfs.pagan.ui.theme

import androidx.compose.ui.unit.Dp
import com.qfs.pagan.LayoutSize

class Sized<T>(vararg dimensions: Pair<LayoutSize, T>) {
    constructor(dim: T): this(LayoutSize.SmallPortrait to dim)
    val mapped_values = HashMap<LayoutSize, T>()

    init {
        for ((layout, dim) in dimensions) {
            this.mapped_values[layout] = dim
        }
    }

    operator fun get(active_size: LayoutSize): T {
        // If the value is mapped, use it
        this.mapped_values[active_size]?.let { return it }

        // If the value is not mapped, but the other orientation of the same size is, then use it
        when (active_size) {
            LayoutSize.SmallPortrait -> this.mapped_values[LayoutSize.SmallLandscape]?.let { return it }
            LayoutSize.SmallLandscape -> this.mapped_values[LayoutSize.SmallPortrait]?.let { return it }
            LayoutSize.MediumPortrait -> this.mapped_values[LayoutSize.MediumLandscape]?.let { return it }
            LayoutSize.MediumLandscape -> this.mapped_values[LayoutSize.MediumPortrait]?.let { return it }
            LayoutSize.LargePortrait -> this.mapped_values[LayoutSize.LargeLandscape]?.let { return it }
            LayoutSize.LargeLandscape -> this.mapped_values[LayoutSize.LargePortrait]?.let { return it }
            LayoutSize.XLargePortrait -> this.mapped_values[LayoutSize.XLargeLandscape]?.let { return it }
            LayoutSize.XLargeLandscape -> this.mapped_values[LayoutSize.XLargePortrait]?.let { return it }
        }

        // Find the next-smallest mapped value
        var actual_layout_passed = false
        var working_value: T? = null
        for (layout_size in listOf(
            LayoutSize.SmallPortrait,
            LayoutSize.SmallLandscape,
            LayoutSize.MediumPortrait,
            LayoutSize.MediumLandscape,
            LayoutSize.LargePortrait,
            LayoutSize.LargeLandscape,
            LayoutSize.XLargePortrait,
            LayoutSize.XLargeLandscape
        )) {
            if (actual_layout_passed && working_value != null) break
            working_value = this.mapped_values[layout_size] ?: working_value
            actual_layout_passed = actual_layout_passed || (layout_size == active_size)
        }
        working_value?.let { return it }

        throw Exception("Value Not Set") // TODO: Specify
    }
}

operator fun Sized<Dp>.times(value: Int): Sized<Dp> {
    val new_values = mutableListOf<Pair<LayoutSize, Dp>>()

    for ((layout, original_value) in this.mapped_values) {
        new_values.add(layout to original_value * value)
    }

    return Sized(*new_values.toTypedArray())
}

operator fun Sized<Dp>.times(value: Float): Sized<Dp> {
    val new_values = mutableListOf<Pair<LayoutSize, Dp>>()

    for ((layout, original_value) in this.mapped_values) {
        new_values.add(layout to original_value * value)
    }

    return Sized(*new_values.toTypedArray())
}




