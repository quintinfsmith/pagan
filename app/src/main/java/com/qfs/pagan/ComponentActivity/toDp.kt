/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
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
