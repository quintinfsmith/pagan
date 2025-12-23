package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.composable.CMBoxBottomShape
import com.qfs.pagan.composable.CMBoxEndShape


@Composable
fun RowScope.CMPadding() {
    Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))
}

@Composable
fun ColumnScope.CMPadding() {
    Spacer(Modifier.height(dimensionResource(R.dimen.contextmenu_padding)))
}

@Composable
fun CMBoxBottom(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .background(
                MaterialTheme.colorScheme.surface,
                CMBoxBottomShape()
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(
                top = dimensionResource(R.dimen.contextmenu_padding),
                start = dimensionResource(R.dimen.contextmenu_padding),
                end = dimensionResource(R.dimen.contextmenu_padding),
                bottom = 0.dp
            ),
            content = content
        )
    }
}

@Composable
fun CMBoxEnd(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .background(
                MaterialTheme.colorScheme.surface,
                CMBoxEndShape()
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(dimensionResource(R.dimen.contextmenu_padding)),
            content = content
        )
    }
}

@Composable
fun ContextMenuSecondaryRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .padding(bottom = dimensionResource(R.dimen.contextmenu_padding))
            .height(dimensionResource(R.dimen.contextmenu_button_height))
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun ContextMenuPrimaryRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .height(dimensionResource(R.dimen.contextmenu_button_height))
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
