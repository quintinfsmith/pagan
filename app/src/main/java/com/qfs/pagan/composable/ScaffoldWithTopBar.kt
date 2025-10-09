package com.qfs.pagan.composable

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import com.qfs.pagan.ComponentActivity.ActivityComposerSettings
import com.qfs.pagan.find_activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaffoldWithTopBar(title: String, force_night_mode: MutableState<Int>, content: @Composable (PaddingValues) -> Unit) {
    val is_night_mode = when (force_night_mode.value) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }

    PaganTheme(is_night_mode) {
        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = title)
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            //Icon(Icons.Filled.ArrowBack, "backIcon")
                        }
                    },
                    //backgroundColor = MaterialTheme.colors.primary,
                    //contentColor = Color.White,
                    //elevation = 10.dp
                )
            },
            content = content
        )
    }
}
