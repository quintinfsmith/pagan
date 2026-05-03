package com.qfs.pagan.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import com.qfs.pagan.TestTag
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.testTag
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun IntegerInputDialog(visibility: MutableState<Boolean>, title_string_id: Int, min_value: Int, max_value: Int? = null, default: Int? = null, callback: (value: Int) -> Unit) {
    val focus_requester = remember { FocusRequester() }
    //default ?: this@ActionDispatcher.persistent_number_input_values[title_string_id] ?: min_value
    val value = remember { mutableIntStateOf(default ?: min_value) }

    PaganDialog(visibility) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IntegerInput(
                value = value.intValue,
                label = { Text(title_string_id) },
                modifier = Modifier
                    .testTag(TestTag.DialogNumberInput)
                    .focusRequester(focus_requester),
                contentPadding = PaddingValues(Dimensions.NumberInputDialogPadding),
                text_align = TextAlign.Center,
                on_focus_exit = {
                    if (it != null) {
                        value.intValue = it
                    }
                },
                minimum = min_value,
                maximum = max_value
            ) { new_value ->
                visibility.value = false
                // this@ActionDispatcher.persistent_number_input_values[title_string_id] = new_value
                callback(new_value)
            }
        }

        DialogBar(
            neutral = { visibility.value = false },
            positive = {
                // this@ActionDispatcher.persistent_number_input_values[title_string_id] = value.value
                visibility.value = false
                callback(value.intValue)
            }
        )

        LaunchedEffect(Unit) {
            focus_requester.requestFocus()
        }
    }
}
