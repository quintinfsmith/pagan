package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.foundation.text.input.setTextAndSelectAll
import androidx.compose.foundation.text.input.toTextFieldBuffer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.max
import kotlin.math.min

@Composable
fun IntegerInput(value: MutableState<Int>, minimum: Int = 0, maximum: Int? = null, modifier: Modifier = Modifier, callback: (Int) -> Unit) {
    val state = rememberTextFieldState("${value.value}")
    OutlinedTextField(
        state = state,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
        modifier = modifier.onFocusChanged { focus_state ->
            println("${focus_state.hasFocus}, ${focus_state.isFocused} ${focus_state.isCaptured}")
            if (focus_state.hasFocus) {
                state.edit {
                    this.selection = TextRange(0, this.length)
                }
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                if (this.length > 0) {
                    val last_char = this.charAt(this.length - 1)
                    if (last_char == '\n' && this.length > 1) {
                        callback(value.value)
                        this.revertAllChanges()
                        return
                    }
                } else {
                    return
                }

                var int_value = try {
                    this.toString().toInt()
                } catch (_: Exception) {
                    this.revertAllChanges()
                    return
                }

                int_value = max(minimum, int_value)
                maximum?.let {
                    int_value = min(maximum, int_value)
                }

                value.value = int_value
            }
        }
    )
}

