package com.qfs.pagan.composable.button

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.qfs.pagan.R

@Composable
fun NumberSelectorButton(modifier: Modifier = Modifier, index: Int, alternate: Boolean, selected: Boolean, highlighted: Boolean, callback: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1F),
        onClick = callback,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonColors(
            containerColor = if (selected) {
                colorResource(R.color.number_selector_highlight)
            } else if (alternate) {
                colorResource(R.color.ns_alt)
            } else {
                colorResource(R.color.ns_default)
            },
            contentColor = if (selected) {
                colorResource(R.color.ns_selected_text)
            } else if (alternate) {
                colorResource(R.color.ns_alt_text)
            } else {
                colorResource(R.color.ns_default_text)
            },
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        content = {
            Text("$index", maxLines = 1)
        }
    )
}
