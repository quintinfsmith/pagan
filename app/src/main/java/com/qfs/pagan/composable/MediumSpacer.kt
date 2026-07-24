package com.qfs.pagan.composable

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ui.theme.MasterTheme

@Composable
fun RowScope.MediumSpacer() {
    Spacer(Modifier.width(MasterTheme.dimensions.SpaceMedium))
}

@Composable
fun ColumnScope.MediumSpacer() {
    Spacer(Modifier.height(MasterTheme.dimensions.SpaceMedium))
}

@Composable
fun RowScope.LargeSpacer() {
    Spacer(Modifier.width(MasterTheme.dimensions.SpaceLarge))
}

@Composable
fun ColumnScope.LargeSpacer() {
    Spacer(Modifier.height(MasterTheme.dimensions.SpaceLarge))
}
