package com.qfs.pagan.composable

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.qfs.pagan.R
import com.qfs.pagan.find_activity

@Composable
fun SoundFontWarning() {
    val url = stringResource(R.string.url_fluid)
    val context = LocalContext.current.find_activity() ?: return

    Card(Modifier.padding(12.dp)) {
        Row {
            Text(stringResource(R.string.warning_nosoundfont))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            SelectionContainer {
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
            Text(stringResource(R.string.warning_nosoundfont_2))
        }
    }
}