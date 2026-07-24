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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.structure.pow
import com.qfs.pagan.ui.theme.MasterTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun FloatInput(
    value: MutableState<Float>,
    precision: Int? = null,
    minimum: Float? = null,
    maximum: Float? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = MasterTheme.dimensions.NumberInputPadding,
    text_align: TextAlign = TextAlign.End,
    prefix: @Composable (() -> Unit)? = null,
    label: (@Composable TextFieldLabelScope.() -> Unit)? = null,
    on_focus_enter: (() -> Unit)? = null,
    on_focus_exit: ((Float?) -> Unit)? = null,
    revert_on_exit: Boolean = false,
    callback: (Float) -> Unit
) {

    NumberInput(
        value,
        modifier,
        contentPadding,
        text_align,
        prefix,
        label,
        on_focus_enter,
        on_focus_exit,
        revert_on_exit,
        { char_sequence ->
            val working_string = mutableListOf<Char>()
            var dot_count = 0
            val current_text = char_sequence.toList().withIndex()
            var bad_char_found = false
            for ((i, c) in current_text) {
                if (c == '.') dot_count++
                if ((i == 0 && c == '-') || (c == '.' && dot_count == 1) || c.isDigit()) {
                    working_string.add(c)
                } else {
                    bad_char_found = true
                }
            }
            val output_string = working_string.joinToString("")
            try {
                var tmp = output_string.toFloat()
                maximum?.let { tmp = min(tmp, it) }
                minimum?.let { tmp = max(tmp, it) }
                precision?.let {
                    val factor = 10.pow(it)
                    tmp = ((tmp * factor).roundToInt()).toFloat() / factor.toFloat()
                }
                Triple(
                    tmp,
                    if (dot_count == 0) {
                        tmp.toInt().toString()
                    } else {
                        tmp.toString()
                    },
                    tmp != output_string.toFloat() || bad_char_found
                )
            } catch (_: Exception) {
                Triple(null, output_string, bad_char_found)
            }
        },
        callback
    )
}