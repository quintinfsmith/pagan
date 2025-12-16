package com.qfs.pagan.composable

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ScaffoldWithTopBar(top_app_bar: @Composable RowScope.() -> Unit, force_night_mode: MutableState<Int>, content: @Composable (PaddingValues) -> Unit) {
    val is_night_mode = when (force_night_mode.value) {
        AppCompatDelegate.MODE_NIGHT_YES -> true
        AppCompatDelegate.MODE_NIGHT_NO -> false
        else -> isSystemInDarkTheme()
    }
    PaganTheme(is_night_mode) {
        Scaffold(
            topBar = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    content = top_app_bar
                )
            },
            //bottomBar = { Text("")},
            content = content,
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
        )
    }
}
