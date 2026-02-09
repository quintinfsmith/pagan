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

import android.view.ViewTreeObserver
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes


@Composable
fun SettingsBoxWrapper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .border(1.dp, MaterialTheme.colorScheme.onSurface, Shapes.SettingsBox)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = Shapes.SettingsBox
            ),
        contentAlignment = Alignment.Center,
        content = {
            ProvideContentColorTextStyle(contentColor = MaterialTheme.colorScheme.onSurface) {
                content()
            }
        }
    )
}

@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimensions.SettingsBoxPadding),
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsBoxWrapper(modifier) {
        Column(
            modifier.padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = { content() }
        )
    }
}

@Composable
fun SettingsBox(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimensions.SettingsBoxPadding),
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.() -> Unit
) {
    SettingsBoxWrapper {
        Box(
            modifier.padding(contentPadding),
            contentAlignment = contentAlignment,
            content = { content() }
        )
    }
}

@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimensions.SettingsBoxPadding),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    SettingsBoxWrapper {
        Row(
            modifier.padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = { content() }
        )
    }
}

@Composable
fun MenuPadder() {
    Spacer(
        Modifier
            .width(Dimensions.SoundFontMenuPadding)
            .height(Dimensions.SoundFontMenuPadding)
    )
}

// https://stackoverflow.com/questions/68847559/how-can-i-detect-keyboard-opening-and-closing-in-jetpack-compose
@Composable
fun keyboardAsState(): MutableState<Boolean> {
    val keyboardState = remember { mutableStateOf(false) }
    val view = LocalView.current
    val viewTreeObserver = view.viewTreeObserver
    DisposableEffect(viewTreeObserver) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            keyboardState.value = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) ?: true
        }
        viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
    return keyboardState
}
