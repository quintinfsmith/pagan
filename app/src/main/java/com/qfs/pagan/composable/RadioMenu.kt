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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.enumerate

@Composable
fun <T> RadioMenu(
    modifier: Modifier = Modifier,
    options: List<Pair<T, @Composable (RowScope.() -> Unit)>>,
    active: MutableState<T>,
    gap_size: Dp = 4.dp,
    callback: (T) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((i, option) in options.enumerate()) {
            val (item, content) = option

            val shape = when (i) {
                0 -> RoundedCornerShape(50F, 0F, 0F, 50F)
                options.size - 1 -> RoundedCornerShape(0F, 50F, 50F, 0F)
                else -> RectangleShape
            }

            if (i != 0) {
                Spacer(Modifier.width(gap_size))
            }

            Button(
                colors = ButtonDefaults.buttonColors().copy(
                    containerColor = if (active.value == item) {
                        SwitchDefaults.colors().checkedTrackColor
                    } else {
                        SwitchDefaults.colors().uncheckedTrackColor
                    },
                    contentColor = if (active.value == item) {
                        SwitchDefaults.colors().checkedThumbColor
                    } else {
                        SwitchDefaults.colors().uncheckedThumbColor
                    }
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = if (active.value == item) {
                        SwitchDefaults.colors().checkedBorderColor
                    } else {
                        SwitchDefaults.colors().uncheckedBorderColor
                    }
                ),
                onClick = {
                    active.value = item
                    callback(item)
                },
                shape = shape,
                content = content
            )
        }
    }
}