package com.qfs.pagan.composable

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle

@Composable
fun ScaffoldWithTopBar(
    modifier: Modifier = Modifier,
    top_app_bar: @Composable RowScope.() -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    drawerContent: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = drawerContent,
    ) {
        Scaffold(
            topBar = {
                val background = MaterialTheme.colorScheme.top_bar_container_color()
                val foreground = MaterialTheme.colorScheme.top_bar_content_color()
                ProvideContentColorTextStyle(
                    contentColor = foreground
                ) {
                    Row(
                        modifier = Modifier.background(color = background),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        content = top_app_bar
                    )
                }
            },
            //bottomBar = { Text("")},
            content = content,
            modifier = Modifier.fillMaxSize()
        )
    }
}
