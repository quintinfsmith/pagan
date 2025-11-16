package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun IntegerInput(minimum: Int = 0, maximum: Int? = null, default: Int? = null, callback: (Int) -> Unit) {
    val working_value = remember { mutableIntStateOf(default ?: minimum) }
    OutlinedTextField(
        state = rememberTextFieldState(initialText = "${working_value.intValue}"),
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
        label = { Text("Label") },
        inputTransformation = object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                var invalid = false
                val int_value = try {
                    this.toString().toInt()
                } catch (e: Exception) {
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

