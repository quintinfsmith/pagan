package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.max
import kotlin.math.min

@Composable
fun IntegerInput(value: MutableState<Int>, minimum: Int? = null, maximum: Int? = null, modifier: Modifier = Modifier, callback: (Int) -> Unit) {
    val state = rememberTextFieldState("${value.value}")
    OutlinedTextField(
        state = state,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        modifier = modifier.onFocusChanged { focus_state ->
            if (focus_state.hasFocus) {
                state.edit {
                    this.selection = TextRange(0, this.length)
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                var working_string = this.toString()
                val enter_pressed = this.length > 0 && this.charAt(this.length -1) == '\n'

                if (enter_pressed) {
                    working_string = working_string.substring(0, this.length - 1)
                }

                if (working_string == "-" && minimum != null && minimum < 0) return

                if (enter_pressed && working_string.isNotEmpty()) {
                    callback(value.value)
                    this.revertAllChanges()
                    return
                }

                var int_value = try {
                    if (working_string.isEmpty()) {
                        0
                    } else {
                        this.toString().toInt()
                    }
                } catch (_: Exception) {
                    this.revertAllChanges()
                    return
                }

                minimum?.let {
                    int_value = max(it, int_value)
                }
                maximum?.let {
                    int_value = min(it, int_value)
                }

                value.value = int_value
            }
        }
    )
}

@Composable
fun FloatInput(value: MutableState<Float>, minimum: Float? = null, maximum: Float? = null, modifier: Modifier = Modifier, callback: (Float) -> Unit) {
    val state = rememberTextFieldState("${value.value}")
    OutlinedTextField(
        state = state,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        modifier = modifier.onFocusChanged { focus_state ->
            if (focus_state.hasFocus) {
                state.edit {
                    this.selection = TextRange(0, this.length)
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                var working_string = this.toString()
                val enter_pressed = this.length > 0 && this.charAt(this.length -1) == '\n'

                if (enter_pressed) {
                    working_string = working_string.substring(0, this.length - 1)
                }

                if (working_string.last() == '.') {
                    working_string = working_string.substring(0, this.length - 1)
                }

                if (working_string == "-" && minimum != null && minimum < 0F) {
                    return
                }

                if (enter_pressed && working_string.isNotEmpty()) {
                    callback(value.value)
                    this.revertAllChanges()
                    return
                }

                var float_value = try {
                    if (working_string.isEmpty()) {
                        0F
                    } else {
                        this.toString().toFloat()
                    }
                } catch (_: Exception) {
                    this.revertAllChanges()
                    return
                }

                minimum?.let {
                    float_value = max(it, float_value)
                }
                maximum?.let {
                    float_value = min(it, float_value)
                }

                value.value = float_value
            }
        }
    )
}
