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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.composable.button.SmallButton
import com.qfs.pagan.composable.button.SmallOutlinedButton
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun DialogCard(
    modifier: Modifier = Modifier,
    colors: CardColors = CardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContentColor = Color.Gray,
        disabledContainerColor = Color.Green,
    ),
    shape: Shape = RoundedCornerShape(Dimensions.DialogRadius),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideContentColorTextStyle(contentColor = colors.contentColor) {
        Surface(
            modifier = modifier
                .then(if (border != null) modifier.border(border) else Modifier),
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(Dimensions.DialogPadding),
                horizontalAlignment = Alignment.Start,
                content = content
            )
        }
    }
}

@Composable
fun DialogTitle(text: String, modifier: Modifier = Modifier) {
    ProvideTextStyle(MaterialTheme.typography.titleLarge) {
        Text(
            text = text,
            modifier = modifier.padding(Dimensions.DialogTitlePadding)
        )
    }
}

@Composable
fun DialogSTitle(text: Int, modifier: Modifier = Modifier) {
    DialogTitle(text = stringResource(text), modifier = modifier)
}


@Composable
fun ColumnScope.DialogBar(
    modifier: Modifier = Modifier,
    positive: (() -> Unit)? = null,
    negative: (() -> Unit)? = null,
    neutral: (() -> Unit)? = null,
    neutral_label: Int = android.R.string.cancel,
    negative_label: Int = R.string.no,
    positive_label: Int = android.R.string.ok,

    ) {
    Row(
        modifier = modifier
            .padding(
                vertical = Dimensions.DialogBarPaddingVertical
            ),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        negative?.let {
            SmallButton(
                modifier = Modifier
                    .height(Dimensions.DialogBarButtonHeight)
                    .weight(1F),
                onClick = it,
                content = { Text(negative_label) }
            )
        }
        neutral?.let {
            if (negative != null) {
                Spacer(Modifier.width(Dimensions.DialogBarSpacing))
            }
            SmallOutlinedButton(
                modifier = Modifier
                    .height(Dimensions.DialogBarButtonHeight)
                    .weight(1F),
                onClick = it,
                border = BorderStroke(Dimensions.OutlineButtonStrokeWidth, MaterialTheme.colorScheme.onSurface),
                content = { Text(neutral_label, maxLines = 1) }
            )
        }
        positive?.let {
            if (negative != null || neutral != null) {
                Spacer(Modifier.width(Dimensions.DialogBarSpacing))
            }
            SmallButton(
                modifier = Modifier
                    .height(Dimensions.DialogBarButtonHeight)
                    .weight(1F),
                onClick = it,
                content = { Text(positive_label) }
            )
        }
    }
}

