package com.qfs.pagan.composable

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun <T> DialogMenu(
    visibility: MutableState<Boolean>,
    title: Int,
    options: () -> List<Pair<T, @Composable RowScope.() -> Unit>>,
    default: T? = null,
    long_click_callback: ((T) -> Unit)? = null,
    callback: (value: T) -> Unit
) {
    val mutable_options = remember { mutableStateOf<List<Pair<T, @Composable RowScope.() -> Unit>>>(listOf()) }
    val options_ready = remember { mutableStateOf(false) }
    PaganDialog(visibility) { visibility ->
        DialogSTitle(title)
        if (options_ready.value) {
            UnSortableMenu(
                Modifier,
                mutable_options.value,
                default,
                long_click_callback = { value ->
                    long_click_callback?.let {
                        visibility.value = false
                        it(value)
                    }
                },
                callback = { value ->
                    visibility.value = false
                    callback(value)
                }
            )
        } else {
            CircularProgressIndicator()
        }
        DialogBar(neutral = { visibility.value = false })

        LaunchedEffect(visibility.value) {
            if (visibility.value) {
                mutable_options.value = options()
                options_ready.value = true
            }
        }
    }
}