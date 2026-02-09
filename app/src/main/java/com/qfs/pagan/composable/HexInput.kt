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
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.delete
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.ui.theme.Dimensions
import kotlin.math.max
import kotlin.math.min

@Composable
fun HexInput(
    value: MutableState<Int>,
    maximum: Int? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = Dimensions.NumberInputPadding,
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Int?) -> Unit)? = null,
    callback: (Int) -> Unit
) {
    val minimum = 0x00
    val hex_state = remember { mutableStateOf(value.value.toHexString()) }
    NumberInput(
        hex_state,
        modifier,
        contentPadding,
        text_align,
        prefix,
        label,
        on_focus_enter,
        { on_focus_exit?.invoke(value.value) },
        object : InputTransformation {
            override fun TextFieldBuffer.transformInput() {
                val working_string = this.toString()

                var converted_value = try {
                    if (working_string.isEmpty()) {
                        minimum
                    } else {
                        this.toString().toInt(16) // From Hex
                    }
                } catch (_: Exception) {
                    this.revertAllChanges()
                    return
                }

                converted_value = max(minimum, converted_value)
                maximum?.let {
                    converted_value = min(it, converted_value)
                }

                value.value = converted_value
            }
        },
        {
            val working_string = this.toString()

            var converted_value = try {
                if (working_string.isEmpty()) {
                    minimum
                } else {
                    this.toString().toInt(16) // From Hex
                }
            } catch (_: Exception) {
                return@NumberInput
            }

            if (minimum > converted_value) {
                converted_value = max(minimum, converted_value)
                this.delete(0, this.length)
                this.append(converted_value.toString())
            }

            maximum?.let {
                if (it < converted_value) {
                    converted_value = min(it, converted_value)
                    this.delete(0, this.length)
                    this.append(converted_value.toString())
                }
            }
        },
        callback = {
            callback(value.value)
        }
    )
}