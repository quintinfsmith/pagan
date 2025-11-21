package com.qfs.pagan.composable

import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun PaganTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Light color palette
    val light_color_palette = lightColorScheme(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC5),
        background = Color.Companion.White,
        surface = Color.Companion.White,
        onPrimary = Color.Companion.White,
        onSecondary = Color.Companion.Black,
        onBackground = Color.Companion.Black,
        onSurface = Color.Companion.Black,
    )

    val dark_color_scheme = darkColorScheme(
        primary = Color(0xFFBB86FC),
        secondary = Color(0xFF03DAC5),
        background = Color.Companion.Black,
        surface = Color.Companion.Black,
        onPrimary = Color.Companion.Black,
        onSecondary = Color.Companion.White,
        onBackground = Color.Companion.White,
        onSurface = Color.Companion.White,
    )

    MaterialTheme(content = content, colorScheme = if (darkTheme) dark_color_scheme else light_color_palette)
}