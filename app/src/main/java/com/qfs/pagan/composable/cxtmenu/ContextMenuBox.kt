package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shadows
import com.qfs.pagan.ui.theme.Shapes


@Composable
fun RowScope.CMPadding() {
    Spacer(Modifier.width(Dimensions.ContextMenuPadding))
}

@Composable
fun ColumnScope.CMPadding() {
    Spacer(Modifier.height(Dimensions.ContextMenuPadding))
}

@Composable
fun CMBoxBottom(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.dropShadow(
            Shapes.CMBoxBottom,
            Shadows.ContextMenu
        ),
        shape = Shapes.CMBoxBottom,
        content = {
            Column(
                modifier = modifier.padding(Dimensions.ContextMenuPadding),
                content = content,
                verticalArrangement = Arrangement.Center
            )
        }
    )
}

@Composable
fun CMBoxEnd(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        Modifier.dropShadow(
            Shapes.CMBoxEnd,
            Shadows.ContextMenu
        ),
        shape = Shapes.CMBoxEnd,
        content = {
            Column(
                modifier = modifier.padding(Dimensions.ContextMenuPadding),
                content = content
            )
        }
    )
}

@Composable
fun ContextMenuSecondaryRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun ContextMenuPrimaryRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
