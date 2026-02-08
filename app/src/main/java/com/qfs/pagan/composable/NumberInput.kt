package com.qfs.pagan.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ui.theme.Typography

@Composable
fun <T> NumberInput(
    value: MutableState<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((T?) -> Unit)? = null,
    input_transformation: InputTransformation,
    output_transformation: OutputTransformation,
    callback: (T) -> Unit
) {
    val trigger_select_all = remember { mutableStateOf<Boolean?>(null) }

    val state = rememberTextFieldState(value.value.toString())
    // Prevent weird focusing behavior causing on_focus_exit to be called without any initial focus
    val was_focused = remember { mutableStateOf(false) }
    val focus_change_callback = { focus_state: FocusState ->
        if (focus_state.isFocused) {
            trigger_select_all.value = trigger_select_all.value?.let { it -> !it } ?: true
            was_focused.value = true
            on_focus_enter?.let { it() }
        } else if (was_focused.value) {
            was_focused.value = false
            on_focus_exit?.let { it(value.value) }
        }
    }

    OutlinedTextField(
        state = state,
        label = label,
        contentPadding = contentPadding,
        textStyle = Typography.TextField.copy(textAlign = text_align),
        prefix = prefix,
        modifier = modifier
            .onKeyEvent { event ->
                when (event.key) {
                    Key.Enter -> {
                        callback(value.value)
                        false
                    }

                    else -> true
                }
            }
            .heightIn(1.dp)
            .onFocusChanged { focus_change_callback(it) },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number
        ),
        inputTransformation = input_transformation,
        outputTransformation = output_transformation,
        lineLimits = TextFieldLineLimits.SingleLine,
        onKeyboardAction = { action ->
            callback(value.value)
            action()
        }
    )

    trigger_select_all.value?.let {
        LaunchedEffect(trigger_select_all.value) {
            state.edit { selectAll() }
        }
    }
}