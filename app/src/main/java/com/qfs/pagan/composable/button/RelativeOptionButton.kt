package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.RelativeInputMode

@Composable
fun RelativeOptionButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    ProvideContentColorTextStyle(colorResource(R.color.button_text)) {
        Box(
            modifier
                .padding(vertical = 0.dp, horizontal = 2.dp)
                .background(colorResource(R.color.button), shape = RoundedCornerShape(50F))
                .combinedClickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = {
                Box(
                    Modifier.padding(vertical = 6.dp, horizontal = 0.dp),
                    content = content
                )
            }
        )
    }
}