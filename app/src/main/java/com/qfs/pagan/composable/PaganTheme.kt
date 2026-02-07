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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.qfs.pagan.Color.Background
import com.qfs.pagan.Color.BackgroundDark
import com.qfs.pagan.Color.Error
import com.qfs.pagan.Color.ErrorContainer
import com.qfs.pagan.Color.ErrorContainerDark
import com.qfs.pagan.Color.ErrorDark
import com.qfs.pagan.Color.OnBackground
import com.qfs.pagan.Color.OnBackgroundDark
import com.qfs.pagan.Color.OnError
import com.qfs.pagan.Color.OnErrorContainer
import com.qfs.pagan.Color.OnErrorContainerDark
import com.qfs.pagan.Color.OnErrorDark
import com.qfs.pagan.Color.OnPrimary
import com.qfs.pagan.Color.OnPrimaryContainer
import com.qfs.pagan.Color.OnPrimaryContainerDark
import com.qfs.pagan.Color.OnPrimaryDark
import com.qfs.pagan.Color.OnSecondary
import com.qfs.pagan.Color.OnSecondaryContainer
import com.qfs.pagan.Color.OnSecondaryContainerDark
import com.qfs.pagan.Color.OnSecondaryDark
import com.qfs.pagan.Color.OnSurface
import com.qfs.pagan.Color.OnSurfaceDark
import com.qfs.pagan.Color.OnSurfaceVariant
import com.qfs.pagan.Color.OnSurfaceVariantDark
import com.qfs.pagan.Color.OnTertiary
import com.qfs.pagan.Color.OnTertiaryContainer
import com.qfs.pagan.Color.OnTertiaryContainerDark
import com.qfs.pagan.Color.OnTertiaryDark
import com.qfs.pagan.Color.Outline
import com.qfs.pagan.Color.OutlineDark
import com.qfs.pagan.Color.Primary
import com.qfs.pagan.Color.PrimaryContainer
import com.qfs.pagan.Color.PrimaryContainerDark
import com.qfs.pagan.Color.PrimaryDark
import com.qfs.pagan.Color.Secondary
import com.qfs.pagan.Color.SecondaryContainer
import com.qfs.pagan.Color.SecondaryContainerDark
import com.qfs.pagan.Color.SecondaryDark
import com.qfs.pagan.Color.Surface
import com.qfs.pagan.Color.SurfaceDark
import com.qfs.pagan.Color.SurfaceVariant
import com.qfs.pagan.Color.SurfaceVariantDark
import com.qfs.pagan.Color.Tertiary
import com.qfs.pagan.Color.TertiaryContainer
import com.qfs.pagan.Color.TertiaryContainerDark
import com.qfs.pagan.Color.TertiaryDark
import com.qfs.pagan.R

@Composable
fun PaganTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val light_color_scheme = lightColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,
        secondary = Secondary,
        onSecondary = OnSecondary,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,
        tertiary = Tertiary,
        onTertiary = OnTertiary,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = OnTertiaryContainer,
        error = Error,
        onError = OnError,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,
        background = Background,
        onBackground = OnBackground,
        surface = Surface,
        onSurface = OnSurface,
        surfaceVariant = SurfaceVariant,
        onSurfaceVariant = OnSurfaceVariant,
        outline = Outline,
    )

    val dark_color_scheme = darkColorScheme(
        primary = PrimaryDark,
        onPrimary = OnPrimaryDark,
        primaryContainer = PrimaryContainerDark,
        onPrimaryContainer = OnPrimaryContainerDark,
        secondary = SecondaryDark,
        onSecondary = OnSecondaryDark,
        secondaryContainer = SecondaryContainerDark,
        onSecondaryContainer = OnSecondaryContainerDark,
        tertiary = TertiaryDark,
        onTertiary = OnTertiaryDark,
        tertiaryContainer = TertiaryContainerDark,
        onTertiaryContainer = OnTertiaryContainerDark,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        background = BackgroundDark,
        onBackground = OnBackgroundDark,
        surface = SurfaceDark,
        onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = OnSurfaceVariantDark,
        outline = OutlineDark
    )


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
        colorScheme = if (darkTheme) dark_color_scheme else light_color_scheme
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
