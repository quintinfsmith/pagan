package com.qfs.pagan.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun <T> DialogMenu(
    visibility: MutableState<Boolean>,
    title: Int,
    options: () -> List<Pair<T, @Composable RowScope.() -> Unit>>,
    default: T? = null,
    on_dismiss_request: (() -> Unit)? = null,
    long_click_callback: ((T) -> Unit)? = null,
    callback: (value: T) -> Unit
) {
    val mutable_options = remember { mutableStateOf<List<Pair<T, @Composable RowScope.() -> Unit>>>(listOf()) }
    val options_ready = remember { mutableStateOf(false) }
    PaganDialog(
        visibility = visibility,
        on_dismiss_request = on_dismiss_request,
    ) { visibility ->
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

@Composable
fun <T> DialogSortableMenu(
    visibility: MutableState<Boolean>,
    title: Int,
    options: () -> List<Pair<T, @Composable RowScope.() -> Unit>>,
    sort_options: List<Pair<Int, (Int, Int) -> Int>>,
    active_sort_option: Int? = null,
    default: T? = null,
    refresher: MutableState<Boolean>? = null,
    extra_content: (@Composable RowScope.(() -> Unit, Int?) -> Unit)? = null,
    on_dismiss_request: (() -> Unit)? = null,
    long_click_callback: ((T) -> Unit)? = null,
    callback: (value: T) -> Unit
) {
    val mutable_options = remember { mutableStateOf<List<Pair<T, @Composable RowScope.() -> Unit>>>(listOf()) }
    val options_ready = remember { mutableStateOf(false) }
    val selected_sort = remember { mutableStateOf(active_sort_option) }
    PaganDialog(
        visibility = visibility,
        on_dismiss_request = on_dismiss_request,
    ) { visibility ->
        if (options_ready.value) {
            SortableMenu(
                modifier = Modifier
                    .weight(1F, fill = false)
                    .fillMaxWidth(),
                title_content = {
                    DialogSTitle(title)
                    extra_content?.let {
                        Row {
                            it( { visibility.value = false }, selected_sort.value)
                        }
                    }
                },
                default_menu = mutable_options.value,
                sort_row_padding = PaddingValues(
                    bottom = Dimensions.DialogBarPaddingVertical,
                ),
                sort_options = sort_options,
                active_sort_option = selected_sort,
                onLongClick = { value: T ->
                    long_click_callback?.let {
                        visibility.value = false
                        it(value)
                    }
                },
                default_value = default,
                onClick = { value ->
                    visibility.value = false
                    callback(value)
                }
            )
        } else {
            CircularProgressIndicator()
        }

        DialogBar(neutral = { visibility.value = false })

        LaunchedEffect(visibility.value, refresher?.value) {
            if (visibility.value) {
                println("K? ${System.currentTimeMillis()}")
                mutable_options.value = options()
                options_ready.value = true
            }
        }
    }
}
