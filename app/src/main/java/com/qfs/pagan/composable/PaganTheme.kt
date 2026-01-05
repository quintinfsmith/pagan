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
import androidx.compose.ui.platform.LocalContext
import com.qfs.pagan.Color.*

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


    MaterialTheme(
        content = content,
        // typography = Typography(
        //     bodyMedium = TextStyle(
        //         fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp/*...*/
        //     ),
        //     bodyLarge = TextStyle(
        //         fontFamily = fontFamily,
        //         fontWeight = FontWeight.Bold,
        //         letterSpacing = 2.sp,
        //         /*...*/
        //     ),
        //     headlineMedium = TextStyle(
        //         fontFamily = fontFamily, fontWeight = FontWeight.SemiBold/*...*/
        //     )
        // ),
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
