package com.qfs.pagan.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun FloatInput(
    value: MutableState<Float>,
    precision: Int? = null,
    minimum: Float? = null,
    maximum: Float? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Float?) -> Unit)? = null,
    callback: (Float) -> Unit
) {
    NumberInput(
        value,
        modifier,
        contentPadding,
        text_align,
        prefix,
        label,
        on_focus_enter,
        on_focus_exit,
        object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                val working_string = this.toString()
                if (working_string == "-" && minimum != null && minimum < 0) return

                var converted_value = try {
                    if (working_string.isEmpty()) {
                        0F
                    } else {
                        this.toString().toFloat()
                    }
                } catch (_: Exception) {
                    this.revertAllChanges()
                    return
                }

                precision?.let {
                    val p = 10F.pow(it)
                    converted_value = (converted_value * p).roundToInt().toFloat() / p
                }
                minimum?.let {
                    converted_value = max(it, converted_value)
                }
                maximum?.let {
                    converted_value = min(it, converted_value)
                }

                value.value = converted_value
            }
        },
        {
            val working_string = this.toString()
            if (working_string == "-" && minimum != null && minimum < 0) return@NumberInput

            var converted_value = try {
                if (working_string.isEmpty()) {
                    0F
                } else {
                    this.toString().toFloat()
                }
            } catch (_: Exception) {
                return@NumberInput
            }

            minimum?.let {
                if (it > converted_value) {
                    converted_value = max(it, converted_value)
                    val text = this.originalText
                    this.replace(0, text.length, converted_value.toString())
                }
            }
            maximum?.let {
                if (it < converted_value) {
                    converted_value = min(it, converted_value)
                    val text = this.originalText
                    this.replace(0, text.length, converted_value.toString())
                }
            }
        },
        callback
    )
}