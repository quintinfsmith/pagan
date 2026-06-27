package com.qfs.pagan.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.ceil

fun DrawScope.AppBackground() {
    val gap_width = Dimensions.Background.Gap.toPx()
    val bar_width = Dimensions.Background.BarWidth.toPx()
    val f = (this.size.width + gap_width + (bar_width / 2)) / (gap_width + bar_width)
    val bar_height_small = Dimensions.Background.BarSmallHeight.toPx()
    val bar_height_large = Dimensions.Background.BarLargeHeight.toPx()
    clipRect {
        for (x in 0 until ceil(f).toInt()) {
            var y_offset = if (x % 2 == 1) {
                bar_height_large
            } else {
                bar_height_small
            } / -2F
            var y = 0
            while (y_offset < this.size.height) {
                val bar_height = if (x % 2 == 1) {
                    if (y % 2 == 0) {
                        bar_height_large
                    } else {
                        bar_height_small
                    }
                } else if (y % 2 == 0) {
                    bar_height_small
                } else {
                    bar_height_large
                }
                drawRoundRect(
                    color = Color(0x10888888),
                    topLeft = Offset(
                        x = (x * (bar_width + gap_width)) - (bar_width / 2),
                        y = y_offset
                    ),
                    size = Size(
                        width = bar_width,
                        height = bar_height
                    ),
                    cornerRadius = CornerRadius(Dimensions.Background.Radius.toPx())
                )

                y_offset += bar_height + gap_width
                y++
            }
        }
    }
}