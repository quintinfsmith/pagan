package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun IntegerInput(value: MutableState<Int>, minimum: Int = 0, maximum: Int? = null, callback: (Int) -> Unit) {
    OutlinedTextField(
        state = rememberTextFieldState(initialText = "${value.value}"),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        label = { Text("Label") },
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                var invalid = false
                val int_value = try {
                    this.toString().toInt()
                } catch (_: Exception) {
                    invalid = true
                    0
                }

                if (invalid || minimum > int_value || (maximum != null && maximum < int_value)) {
                    this.revertAllChanges()
                }
            }
        }
    )
}

