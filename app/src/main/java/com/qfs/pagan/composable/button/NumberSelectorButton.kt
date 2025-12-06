package com.qfs.pagan.composable.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun NumberSelectorButton(modifier: Modifier = Modifier, index: Int, alternate: Boolean, selected: Boolean, highlighted: Boolean, callback: () -> Unit) {
    Button(
        modifier = modifier,
        onClick = callback,
        shape = RoundedCornerShape(12.dp),
        border = if (highlighted) BorderStroke(2.dp, color = colorResource(R.color.number_selector_highlight))
            else null,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonColors(
            containerColor = if (selected) colorResource(R.color.number_selector_highlight)
                else if (alternate) colorResource(R.color.ns_alt)
                else colorResource(R.color.ns_default),
            contentColor = if (selected) colorResource(R.color.ns_selected_text)
                else if (alternate) colorResource(R.color.ns_alt_text)
                else colorResource(R.color.ns_default_text),
            disabledContentColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        content = { Text("$index", maxLines = 1) }
    )
}
