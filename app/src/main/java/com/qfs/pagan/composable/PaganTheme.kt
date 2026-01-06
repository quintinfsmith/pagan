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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.qfs.pagan.Color.*
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
            labelLarge = default.labelLarge.copy(fontFamily = font),
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
