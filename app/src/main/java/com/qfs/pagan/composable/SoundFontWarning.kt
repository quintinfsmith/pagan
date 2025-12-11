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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.find_activity

@Composable
fun SoundFontWarning() {
    val url = stringResource(R.string.url_fluid)
    val context = LocalContext.current.find_activity() ?: return


    ProvideContentColorTextStyle(contentColor = Color.Black) {
        Column(
            Modifier
                .background(colorResource(R.color.blue_sky), shape = RoundedCornerShape(24.dp))
                .border(4.dp, colorResource(R.color.blue_dark), shape = RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row {
                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                    SText(R.string.warning_nosoundfont)
                }
            }
            Row(
                modifier = Modifier
                    .padding(vertical = 2.dp, horizontal = 1.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                    Text(
                        text = url,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = url.toUri()
                            context.startActivity(intent)
                        }
                    )
                }
            }
            Row {
                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                    SText(R.string.warning_nosoundfont_2)
                }
            }
        }
    }
}