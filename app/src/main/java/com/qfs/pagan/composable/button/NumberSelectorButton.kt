package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun NumberSelectorButton(modifier: Modifier = Modifier, index: Int, alternate: Boolean, selected: Boolean, highlighted: Boolean, callback: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (selected) colorResource(R.color.number_selector_highlight)
                else if (alternate) colorResource(R.color.ns_alt)
                else colorResource(R.color.ns_default),
                RoundedCornerShape(12.dp)
            )
            .then(
                if (highlighted) {
                    modifier.border(2.dp, colorResource(R.color.number_selector_highlight))
                } else {
                    modifier
                }
            )
            .padding(0.dp)
            .combinedClickable(
                onClick = callback
            ),
    ) {
        Text(
            "$index",
            maxLines = 1,
            color = if (selected) colorResource(R.color.ns_selected_text)
                else if (alternate) colorResource(R.color.ns_alt_text)
                else colorResource(R.color.ns_default_text)
        )
    }
}
