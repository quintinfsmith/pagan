package com.qfs.pagan.composable.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun ColumnScope.TableLine(color: Color) {
    Spacer(
        Modifier.Companion
            .background(color = color)
            .fillMaxWidth()
            .height(Dimensions.TableLineStroke)
    )
}

@Composable
fun RowScope.TableLine(color: Color) {
    Spacer(
        Modifier
            .background(color = color)
            .fillMaxHeight()
            .width(Dimensions.TableLineStroke)
    )
}

@Composable
fun BoxScope.TableLine(color: Color) {
    Spacer(
        Modifier.Companion
            .background(color = color)
            .align(Alignment.Companion.CenterEnd)
            .fillMaxHeight()
            .width(Dimensions.TableLineStroke)
    )
}