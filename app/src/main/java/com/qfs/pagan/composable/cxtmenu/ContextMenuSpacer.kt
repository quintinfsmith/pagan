package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun ContextMenuSpacer() {
    Spacer(
        Modifier.Companion
            .height(Dimensions.ContextMenuSpacing)
            .width(Dimensions.ContextMenuSpacing)
    )
}