package com.qfs.pagan.ComponentActivity

import android.content.Context
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Context.toDp(float: Float): Dp {
    val pixel_density = this.resources.displayMetrics.density
    return (float / pixel_density).dp
}

fun Context.toPx(density_pixel: Dp): Float {
    val pixel_density = this.resources.displayMetrics.density
    return density_pixel.value * pixel_density
}
