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
import androidx.compose.material3.TextFieldLabelScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.ui.theme.Dimensions
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
    revert_on_exit: Boolean = false,
    callback: (Int) -> Unit
) {
    TODO()
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
        revert_on_exit,
        { char_sequence ->
            val working_string = mutableListOf<Char>()
            val current_text = char_sequence.toList()
            for (c in current_text) {
                if (c.isDigit() || "abcdef".contains(c.lowercase())) {
                    working_string.add(c)
                }
            }
            val output_string = working_string.joinToString("")
            val output_value = try {
                var tmp = output_string.toInt()
                maximum?.let { tmp = min(tmp, it) }
                tmp.toHexString()
            } catch (_: Exception) {
                null
            }
            Pair(output_value, output_string)
        },
        callback = {
            callback(value.value)
        }
    )
}