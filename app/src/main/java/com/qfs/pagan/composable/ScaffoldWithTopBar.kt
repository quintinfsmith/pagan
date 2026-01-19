package com.qfs.pagan.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle

@Composable
fun ScaffoldWithTopBar(
    modifier: Modifier = Modifier,
    top_app_bar: (@Composable RowScope.() -> Unit)?,
    content: @Composable (PaddingValues) -> Unit,
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    drawerContent: @Composable () -> Unit
) {
    val keyboard_controller = LocalSoftwareKeyboardController.current
    val focus_manager = LocalFocusManager.current
    ModalNavigationDrawer(
        modifier = modifier
            // Allow click-away from text fields
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    keyboard_controller?.hide()
                    focus_manager.clearFocus()
                }
            },
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = drawerContent,
    ) {
        Scaffold(
            topBar = {
                val background = MaterialTheme.colorScheme.top_bar_container_color()
                val foreground = MaterialTheme.colorScheme.top_bar_content_color()
                top_app_bar?.let {
                    println("TOP BAR EXIST? $it")
                    ProvideContentColorTextStyle(contentColor = foreground) {
                        Row(
                            modifier = Modifier
                                .height(dimensionResource(R.dimen.topbar_height))
                                .background(color = background),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            content = top_app_bar
                        )
                    }
                }
            },
            content = content,
            modifier = Modifier.fillMaxSize()
        )
    }
}
