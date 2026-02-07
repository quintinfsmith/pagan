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

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shadows
import com.qfs.pagan.ui.theme.Shapes

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
                    ProvideContentColorTextStyle(contentColor = foreground) {
                        Row(
                            modifier = Modifier
                                .height(Dimensions.TopBarHeight)
                                .dropShadow(
                                    shadow = Shadows.TopBar,
                                    shape = Shapes.TopBar
                                )
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
