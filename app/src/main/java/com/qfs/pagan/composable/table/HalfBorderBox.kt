package com.qfs.pagan.composable.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun HalfBorderBox(
    modifier: Modifier = Modifier,
    border_width: Dp = Dimensions.TableLineStroke,
    border_color: Color,
    content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier,
        contentAlignment = Alignment.BottomEnd,
        content = {
            Box(
                modifier = Modifier.fillMaxSize(),
                content = content,
                contentAlignment = Alignment.Center
            )
            Spacer(
                Modifier
                    .width(border_width)
                    .background(border_color)
                    .fillMaxHeight()
            )
        }
    )
}