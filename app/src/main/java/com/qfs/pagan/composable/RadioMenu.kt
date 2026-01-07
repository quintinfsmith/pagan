package com.qfs.pagan.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.enumerate

@Composable
fun <T> RadioMenu(
    options: List<Pair<T, @Composable (RowScope.() -> Unit)>>,
    active: MutableState<T?> = mutableStateOf(null),
    gap_size: Dp = 4.dp,
    callback: (T) -> Unit
) {
    Row {
        for ((i, option) in options.enumerate()) {
            val (item, content) = option

            val shape = when (i) {
                0 -> RoundedCornerShape(50F, 0F, 0F, 50F)
                options.size - 1 -> RoundedCornerShape(0F, 50F, 50F, 0F)
                else -> RectangleShape
            }

            if (i != 0) {
                Spacer(Modifier.width(gap_size))
            }

            Button(
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = if (active.value == item) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        ButtonDefaults.buttonColors().containerColor
                    },
                    contentColor = if (active.value == item) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        ButtonDefaults.buttonColors().contentColor
                    }
                ),
                onClick = {
                    active.value = item
                    callback(item)
                },
                shape = shape,
                content = content
            )
        }
    }
}