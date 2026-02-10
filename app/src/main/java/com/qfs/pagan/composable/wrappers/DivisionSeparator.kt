package com.qfs.pagan.composable.wrappers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun DivisorSeparator(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(horizontal = Dimensions.DivisorPadding)
) {
    Text("/", modifier.padding(padding))
}
