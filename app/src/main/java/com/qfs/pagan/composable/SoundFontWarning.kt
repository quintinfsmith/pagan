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

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.find_activity
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Typography


@Composable
fun SoundFontWarning(in_settings: Boolean = false) {
    val url = stringResource(R.string.url_fluid)
    val context = LocalContext.current.find_activity() ?: return

    ProvideContentColorTextStyle(contentColor = MaterialTheme.colorScheme.onTertiary) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(24.dp))
                .border(4.dp, MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(24.dp))
                .padding(16.dp),

        ) {
            ProvideTextStyle(Typography.SoundFontWarning.Title) {
                SText(
                    string_id = R.string.warning_nosoundfont_a,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(Dimensions.SFWarningInnerPadding))

            Column {
                ProvideTextStyle(Typography.SoundFontWarning.Body) {
                    SText(R.string.warning_nosoundfont_b)
                }
                Spacer(Modifier.height(Dimensions.SFWarningInnerPadding))

                ProvideTextStyle(Typography.SoundFontWarning.Url) {
                    Text(
                        text = url,
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = url.toUri()
                                context.startActivity(intent)
                            }
                    )
                }

            }

            if (!in_settings) {
                Spacer(Modifier.height(Dimensions.SFWarningInnerPadding))
                Row(horizontalArrangement = Arrangement.Center) {
                    ProvideTextStyle(Typography.SoundFontWarning.Body) {
                        SText(R.string.warning_nosoundfont_c)
                    }
                }
            }
        }
    }
}