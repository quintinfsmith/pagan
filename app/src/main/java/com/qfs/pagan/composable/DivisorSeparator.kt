package com.qfs.pagan.composable

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun DivisorSeparator() {
    Text("/", Modifier.padding(horizontal= Dimensions.DivisorPadding))
}