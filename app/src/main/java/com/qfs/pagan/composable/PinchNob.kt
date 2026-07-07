package com.qfs.pagan.composable

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import com.qfs.pagan.R
import com.qfs.pagan.process_zoom

@Composable
fun PinchNob(
    modifier: Modifier = Modifier,
    value: MutableFloatState,
    minimum: Float = 0F,
    maximum: Float = 1F,
) {
    Icon(
        modifier = Modifier
            .rotate(value.floatValue * 360F)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    // Main drag loop
                    while (true) {
                        process_zoom(awaitPointerEvent()) { zoom, _, _ ->
                            value.floatValue = (value.floatValue + zoom).coerceIn(minimum, maximum)
                        }
                    }
                }
            },
        painter = painterResource(R.drawable.icon_ctl),
        contentDescription = null
    )
}