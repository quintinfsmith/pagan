package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
fun CMBoxBottom(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .background(
                colorResource(R.color.surface),
                CMBoxBottomShape()
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
fun CMBoxEnd(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier
            .background(
                colorResource(R.color.surface),
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
