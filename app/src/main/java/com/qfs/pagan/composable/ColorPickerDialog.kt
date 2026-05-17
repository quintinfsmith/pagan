package com.qfs.pagan.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.qfs.pagan.R

@Composable
fun ColorPickerDialog(initial_color: Color, visibility: MutableState<Boolean>, callback: (Color?) -> Unit) {
    val color_state = remember { mutableStateOf(initial_color) }
    PaganDialog(visibility) {
        Row(horizontalArrangement = Arrangement.Center) {
            ColorPicker(
                modifier = Modifier.fillMaxWidth(),
                color = color_state
            )
        }
        DialogBar(
            negative = {
                visibility.value = false
                callback(null)
            },
            negative_label = R.string.use_default_color,
            neutral = { visibility.value = false },
            positive = {
                visibility.value = false
                callback(color_state.value)
            }

        )
    }
}