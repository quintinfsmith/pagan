package com.qfs.pagan.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.structure.pow
import com.qfs.pagan.ui.theme.Colors
import com.qfs.pagan.ui.theme.Dimensions
import kotlin.math.roundToInt

@Composable
fun Knob(
    modifier: Modifier = Modifier,
    value: MutableFloatState = remember { mutableFloatStateOf(0F) },
    minimum: Float = 0F,
    maximum: Float = 1F,
    rotations: Int = 1,
    precision: Int? = null,
    color: Color = Colors.active_color_scheme.button,
    padding: PaddingValues = Dimensions.KnobPadding,
    callback: (value: Float) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier
                .padding(padding)
                .rotate(((value.floatValue - minimum) / (maximum - minimum)) * (rotations * 360F))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        // Main drag loop
                        while (true) {
                            val event = awaitPointerEvent()
                            when (event.type) {
                                PointerEventType.Move -> {
                                    val diff = (event.changes[0].previousPosition.x - event.changes[0].position.x) * (maximum - minimum) / (360F * rotations)
                                    value.floatValue = (value.floatValue - diff).coerceIn(minimum, maximum)
                                    precision?.let {
                                        val e = 10.pow(precision)
                                        value.floatValue = (value.floatValue * e).roundToInt() / e.toFloat()
                                    }
                                }
                                PointerEventType.Release -> {
                                    if (event.changes.size == 1) {
                                        callback(value.floatValue)
                                    }
                                }
                            }
                        }
                    }
                },
            painter = painterResource(R.drawable.icon_knob_full),
            contentDescription = null,
            tint = color
        )
    }
}