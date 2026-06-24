package com.qfs.pagan.composable

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.qfs.pagan.composable.wrappers.CircularProgressIndicator
import com.qfs.pagan.ui.theme.Colors

@Composable
fun SoundfontLoadingIndicator(color: Color = Colors.active_color_scheme.loading_indicator) {
    CircularProgressIndicator(color = color)
}