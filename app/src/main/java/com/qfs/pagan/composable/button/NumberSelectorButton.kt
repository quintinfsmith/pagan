package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun NumberSelectorButton(modifier: Modifier = Modifier, index: Int, alternate: Boolean, selected: Boolean, highlighted: Boolean, callback: (Int) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val (background, foreground) = if (selected) {
        Pair(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
    } else if (alternate) {
        Pair(MaterialTheme.colorScheme.primaryFixed, MaterialTheme.colorScheme.onPrimaryFixed)
    } else {
        Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(dimensionResource(R.dimen.number_selector_button_height))
            .padding(1.dp)
            .background(background, shape)
            .padding(0.dp)
            .combinedClickable(
                onClick = {
                    callback(index)
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(2.dp)
                .fillMaxSize()
                .then(
                    if (highlighted) Modifier.border(.5.dp, foreground, RoundedCornerShape(10.dp))
                    else Modifier
                )
        ) {
            Text(
                "$index",
                maxLines = 1,
                color = foreground
            )
        }
    }
}
@Composable
fun NumberSelectorInner(modifier: Modifier, size: Int, selected: Int?, highlighted: Int?, alternate: Boolean, callback: (Int) -> Unit) {
    for (i in 0 until size) {
        NumberSelectorButton(
            modifier = modifier,
            index = i,
            selected = selected == i,
            highlighted = highlighted == i,
            alternate = alternate,
            callback = callback
        )
    }
}

@Composable
fun RowScope.NumberSelector(size: Int, selected: Int?, highlighted: Int?, alternate: Boolean, callback: (Int) -> Unit) {
    NumberSelectorInner(Modifier.weight(1F), size, selected, highlighted, alternate, callback)
}
@Composable
fun ColumnScope.NumberSelector(size: Int, selected: Int?, highlighted: Int?, alternate: Boolean, callback: (Int) -> Unit) {
    NumberSelectorInner(Modifier.fillMaxWidth().weight(1F), size, selected, highlighted, alternate, callback)
}
