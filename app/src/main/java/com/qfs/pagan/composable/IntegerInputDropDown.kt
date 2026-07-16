/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2026  Quintin Foster Smith
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.wrappers.DropdownMenu
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes

@Composable
fun IntegerInputDropDown(
    title_string_id: Int,
    visibility: MutableState<Boolean>,
    value: MutableIntState,
    min_value: Int,
    max_value: Int? = null,
    callback: (value: Int) -> Unit
) {
    val focus_requester = remember { FocusRequester() }
    DropdownMenu(
        expanded = visibility.value,
        onDismissRequest = { visibility.value = false },
        shape = Shapes.NumberInputDialog

    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IntegerInput(
                value = value,
                label = { Text(title_string_id) },
                modifier = Modifier
                    .testTag(TestTag.DialogNumberInput)
                    .width(Dimensions.NumberInputDialogWidth)
                    .padding(Dimensions.NumberInputDialogPadding)
                    .focusRequester(focus_requester),
                contentPadding = Dimensions.NumberInputDialogPadding,
                text_align = TextAlign.Center,
                on_focus_exit = { dialog_value ->
                    dialog_value?.let { value.intValue = it }
                },
                minimum = min_value,
                maximum = max_value
            ) { new_value ->
                visibility.value = false
                callback(new_value)
            }
            Button(
                modifier = Modifier
                    .testTag(TestTag.DialogPositive)
                    .padding(Dimensions.NumberInputDialogPadding)
                    .size(42.dp),
                onClick = {
                    visibility.value = false
                    callback(value.intValue)
                },
                contentPadding = PaddingValues(0.dp),
                content = {
                    Icon(
                        modifier = Modifier.padding(4.dp),
                        painter = painterResource(R.drawable.icon_check),
                        contentDescription = stringResource(android.R.string.ok)
                    )
                }
            )
        }

        LaunchedEffect(Unit) {
            focus_requester.requestFocus()
        }
    }
}