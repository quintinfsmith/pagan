/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.qfs.pagan.R

@Composable
fun PaganTheme(color_scheme: ColorScheme, content: @Composable () -> Unit) {
    val default = Typography()
    val font = FontFamily(
        //Font(R.font.fira_sans, FontWeight.Light),
        Font(R.font.fira_sans, FontWeight.Normal),
        //Font(R.font.fira_sans, FontWeight.Normal, FontStyle.Italic),
        //Font(R.font.fira_sans, FontWeight.Medium),
        //Font(R.font.fira_sans, FontWeight.Bold),
    )
    MaterialTheme(
        content = content,
        typography = default.copy(
            displayLarge = default.displayLarge.copy(fontFamily = font),
            displayMedium = default.displayMedium.copy(fontFamily = font),
            displaySmall = default.displaySmall.copy(fontFamily = font),
            headlineLarge = default.headlineLarge.copy(fontFamily = font),
            headlineMedium = default.headlineMedium.copy(fontFamily = font),
            headlineSmall = default.headlineSmall.copy(fontFamily = font),
            titleLarge = default.titleLarge.copy(fontFamily = font),
            titleMedium = default.titleMedium.copy(fontFamily = font),
            titleSmall = default.titleSmall.copy(fontFamily = font),
            bodyLarge = default.bodyLarge.copy(fontFamily = font),
            bodyMedium = default.bodyMedium.copy(fontFamily = font),
            bodySmall = default.bodySmall.copy(fontFamily = font),
            labelLarge = default.labelLarge.copy(
                fontFamily = font,
                letterSpacing = 1.sp
            ),
            labelMedium = default.labelMedium.copy(fontFamily = font),
            labelSmall = default.labelSmall.copy(fontFamily = font)
        ),
        colorScheme = color_scheme
    )
}

@Composable
fun ColorScheme.is_light(): Boolean {
    return this.background.luminance() > 0.5
}

@Composable
fun ColorScheme.top_bar_container_color(): Color {
    return if (this.is_light()) {
        this.primary
    } else {
        this.surface
    }
}

@Composable
fun ColorScheme.top_bar_content_color(): Color {
    return if (this.is_light()) {
        this.onPrimary
    } else {
        this.onSurface
    }
}
