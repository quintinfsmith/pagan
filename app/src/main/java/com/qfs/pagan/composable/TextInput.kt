/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography

@Composable
fun TextInput(
    modifier: Modifier = Modifier,
    input: MutableState<String>,
    textAlign: TextAlign = TextAlign.End,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((String) -> Unit)? = null,
    shape: Shape = Shapes.Container,
    callback_on_return: Boolean = false,
    callback: (String) -> Unit
) {
    val state = rememberTextFieldState(input.value)
    val trigger_select_all = remember { mutableStateOf<Boolean?>(null) }
    val focus_manager = LocalFocusManager.current
    val was_focused = remember { mutableStateOf(false) }
    val focus_change_callback = { focus_state: FocusState ->
        if (focus_state.isFocused) {
            trigger_select_all.value = trigger_select_all.value?.let { !it } ?: true
            was_focused.value = true
            on_focus_enter?.let { it() }
        } else if (was_focused.value) {
            was_focused.value = false
            on_focus_exit?.let { it(input.value) }
        }
    }
    OutlinedTextField(
        state = state,
        lineLimits = lineLimits,
        label = label,
        placeholder = placeholder,
        textStyle = Typography.TextField.copy(textAlign = textAlign),
        modifier = modifier.onFocusChanged { focus_change_callback(it) },
        shape = shape,
        scrollState = rememberScrollState(),
        onKeyboardAction = {
            callback(input.value)
            focus_manager.clearFocus()
        },
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = if (callback_on_return) {
                ImeAction.Done
            } else {
                KeyboardOptions.Default.imeAction
            },
            keyboardType = KeyboardType.Text
        ),
        inputTransformation = InputTransformation { input.value = this.toString() }
    )
}