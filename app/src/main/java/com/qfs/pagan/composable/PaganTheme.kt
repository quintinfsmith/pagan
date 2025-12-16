package com.qfs.pagan.composable

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.qfs.pagan.R

@Composable
fun PaganTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Light color palette
    val light_color_palette = lightColorScheme(
        primary = colorResource(R.color.primary),
        secondary = colorResource(R.color.primary),
        background = colorResource(R.color.main_background),
        surface = colorResource(R.color.surface),
        onPrimary = colorResource(R.color.primary_text),
        onSecondary = Color.Companion.Black,
        onBackground = colorResource(R.color.main_foreground),
        onSurface = colorResource(R.color.on_surface),
    )
    val dark_color_scheme = darkColorScheme(
        primary = colorResource(R.color.primary),
        secondary = colorResource(R.color.primary),
        background = colorResource(R.color.main_background),
        surface = colorResource(R.color.surface),
        onPrimary = colorResource(R.color.primary_text),
        onSecondary = Color.Companion.Black,
        onBackground = colorResource(R.color.main_foreground),
        onSurface = colorResource(R.color.on_surface),
    )


    MaterialTheme(
        content = content,
        typography = Typography(),
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    )
}