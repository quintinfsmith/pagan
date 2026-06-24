package com.qfs.pagan.composable.wrappers

import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ui.theme.Colors
import androidx.compose.material3.CircularProgressIndicator as OriginalIndicator

@Composable
fun CircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Colors.active_color_scheme.loading_indicator,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = Color.Transparent,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
    gapSize: Dp = 4.dp
) {
    OriginalIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = strokeCap,
        gapSize = gapSize
    )
}

@Composable
fun CircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Colors.active_color_scheme.loading_indicator,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = Color.Transparent,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
    gapSize: Dp = 4.dp
) {
    OriginalIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
        trackColor = trackColor,
        strokeCap = strokeCap,
        gapSize = gapSize
    )
}