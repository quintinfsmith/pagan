package com.qfs.pagan.composable

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.find_activity


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
            ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                SText(
                    string_id = R.string.warning_nosoundfont_a,
                    textAlign = TextAlign.Center
                )
            }

            Column(Modifier.padding(top = 8.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleSmall) {
                    SText(R.string.warning_nosoundfont_b)
                }

                ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                    Text(
                        text = url,
                        textAlign = TextAlign.Center,
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
                Row(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        SText(R.string.warning_nosoundfont_c)
                    }
                }
            }
        }
    }
}