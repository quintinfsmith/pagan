package com.qfs.pagan.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.window.Dialog

@Composable
fun PaganDialog(visibility: MutableState<Boolean>, alignment: Alignment.Horizontal = Alignment.CenterHorizontally, content: @Composable ColumnScope.(MutableState<Boolean>) -> Unit) {
    if (!visibility.value) return
    PaganTheme(MaterialTheme.colorScheme) {
        Dialog(onDismissRequest = { visibility.value = false }) {
            val keyboard_controller = LocalSoftwareKeyboardController.current
            val focus_manager = LocalFocusManager.current

            // Extra Box prevents weird dialog jumping on focus
            Box(
                Modifier
                    .clickable { visibility.value = false }
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DialogCard(
                    modifier = Modifier
                        // Allow click-away from text fields
                        .wrapContentSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                keyboard_controller?.hide()
                                focus_manager.clearFocus()
                            }
                        },
                    alignment = alignment,
                    content = {
                        content(visibility)
                    }
                )
            }
        }
    }
}