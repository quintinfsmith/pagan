package com.qfs.pagan.composable

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun RowScope.MediumSpacer() {
    Spacer(Modifier.width(Dimensions.Space.Medium))
}

@Composable
fun ColumnScope.MediumSpacer() {
    Spacer(Modifier.height(Dimensions.Space.Medium))
}