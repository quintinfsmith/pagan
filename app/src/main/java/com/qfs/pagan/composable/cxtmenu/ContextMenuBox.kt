package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun CMBoxBottom(content: @Composable ColumnScope.() -> Unit) {
    Box(
        Modifier.Companion
            .background(
                colorResource(R.color.surface),
                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
            )
    ) {
        Column(
            Modifier.Companion.padding(4.dp),
            content = content
        )
    }
}