package com.qfs.pagan.composable

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun SText(
    string_id: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(stringResource(string_id), modifier, color)
}

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
        keyboardOptions = KeyboardOptions.Companion.Default.copy(keyboardType = KeyboardType.Companion.Number),
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                var working_string = this.toString()
                val enter_pressed = this.length > 0 && this.charAt(this.length - 1) == '\n'

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
fun FloatInput(value: MutableState<Float>, minimum: Float? = null, maximum: Float? = null, modifier: Modifier = Modifier, precision: Int? = null, outlined: Boolean = true, callback: (Float) -> Unit) {
    val state = rememberTextFieldState("${value.value}")
    val textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
    val modifier = modifier.onFocusChanged { focus_state ->
        if (focus_state.hasFocus) {
            state.edit {
                this.selection = TextRange(0, this.length)
            }
        }
    }
    val keyboardOptions = KeyboardOptions.Companion.Default.copy(keyboardType = KeyboardType.Companion.Number)
    val inputTransformation = object : InputTransformation {
        override fun TextFieldBuffer.transformInput() {
            var working_string = this.toString()
            val enter_pressed = this.length > 0 && this.charAt(this.length - 1) == '\n'

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

            precision?.let {
                val p = 10F.pow(it)
                float_value = (float_value * p).roundToInt().toFloat() / p
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

    if (outlined) {
        OutlinedTextField(
            state = state,
            textStyle = textStyle,
            modifier = modifier,
            keyboardOptions = keyboardOptions,
            inputTransformation = inputTransformation
        )
    } else {
        TextField(
            state = state,
            textStyle = textStyle,
            modifier = modifier,
            keyboardOptions = keyboardOptions,
            inputTransformation = inputTransformation
        )
    }
}