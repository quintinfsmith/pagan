package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.NumberSelector
import com.qfs.pagan.R

@Composable
fun NumberSelectorButton(modifier: Modifier = Modifier, index: Int, alternate: Boolean, selected: Boolean, highlighted: Boolean, callback: (Int) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(dimensionResource(R.dimen.number_selector_button_height))
            .padding(1.dp)
            .background(
                if (alternate) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary,
                shape
            )
            .then(
                if (selected) {
                    modifier.border(3.dp, colorResource(R.color.selected_primary), shape)
                } else if (highlighted) {
                    modifier.border(1.dp, colorResource(R.color.selected_secondary), shape)
                } else {
                    modifier
                }
            )
            .padding(0.dp)
            .combinedClickable(
                onClick = {
                    callback(index)
                }
            )
    ) {
        Text(
            "$index",
            maxLines = 1,
            color = if (alternate) MaterialTheme.colorScheme.onSecondary
                else MaterialTheme.colorScheme.onPrimary
        )
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
