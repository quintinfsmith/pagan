package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun NumberSelectorButton(modifier: Modifier = Modifier, index: Int, alternate: Boolean, selected: Boolean, highlighted: Boolean, callback: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(dimensionResource(R.dimen.number_selector_button_height))
            .padding(1.dp)
            .background(
                if (alternate) colorResource(R.color.ns_alt)
                else colorResource(R.color.ns_default),
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
                onClick = callback
            )
    ) {
        Text(
            "$index",
            maxLines = 1,
            color = if (alternate) colorResource(R.color.ns_alt_text)
                else colorResource(R.color.ns_default_text)
        )
    }
}
