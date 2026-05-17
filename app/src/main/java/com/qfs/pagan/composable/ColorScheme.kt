package com.qfs.pagan.composable

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

object ColorScheme {
    val Light = lightColorScheme(
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

    val Dark = darkColorScheme(
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
}