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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.selectAll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ui.theme.Dimensions.Unpadded
import com.qfs.pagan.ui.theme.Typography

@Composable
fun <T> NumberInput(
    value: T,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = Unpadded,
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((T?) -> Unit)? = null,
    revert_on_exit: Boolean = false,
    string_validate: (CharSequence) -> Pair<T?, CharSequence>,
    callback: (T) -> Unit
) {

    val trigger_select_all = remember { mutableStateOf<Boolean?>(null) }
    val state = rememberTextFieldState(value.toString())
    // Prevent weird focusing behavior causing on_focus_exit to be called without any initial focus
    val was_focused = remember { mutableStateOf(false) }
    var backup_value = remember { mutableStateOf(value) }
    val focus_change_callback = { focus_state: FocusState ->
        val (validated_value, validated_string) = string_validate(state.text)
        if (focus_state.isFocused) {
            trigger_select_all.value = trigger_select_all.value?.let { it -> !it } ?: true
            was_focused.value = true
            on_focus_enter?.let { it() }
        } else if (was_focused.value) {
            was_focused.value = false
            on_focus_exit?.let { validated_value?.let { v -> it(v) } }
            if (revert_on_exit) {
                val text = state.text
                state.edit {
                    replace(0, text.length, backup_value.value.toString())
                }
            }
        }
    }
    val focus_manager = LocalFocusManager.current
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
                        val char_sequence = state.text
                        val (validated_value, validated_string) = string_validate(char_sequence)
                        validated_value?.let {
                            callback(it)
                            backup_value.value = it
                            focus_manager.clearFocus()
                        }
                        true
                    }

                    else -> false
                }
            }
            .heightIn(1.dp)
            .onFocusChanged {
                focus_change_callback(it)
            },
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number
        ),
        inputTransformation = InputTransformation {
            val char_sequence = this.asCharSequence()
            val (validated_value, validated_string) = string_validate(char_sequence)
            this.replace(0, char_sequence.count(), validated_string)
        },
        lineLimits = TextFieldLineLimits.SingleLine,
        onKeyboardAction = { action ->
            val (validated_value, validated_string) = string_validate(state.text)
            validated_value?.let {
                backup_value.value = it
                callback(it)
            }
            action()
            focus_manager.clearFocus()
        }
    )

    trigger_select_all.value?.let {
        LaunchedEffect(trigger_select_all.value) {
            state.edit { selectAll() }
        }
    }
}