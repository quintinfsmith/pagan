/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun RelativeOptionButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    ProvideContentColorTextStyle(colorResource(R.color.button_text)) {
        Box(
            modifier
                .padding(vertical = 0.dp, horizontal = Dimensions.Space.Small)
                .background(colorResource(R.color.button), shape = RoundedCornerShape(50F))
                .combinedClickable(onClick = onClick),
            contentAlignment = Alignment.Center,
            content = {
                Box(
                    Modifier.padding(vertical = Dimensions.Space.Medium, horizontal = 0.dp),
                    content = content
                )
            }
        )
    }
}