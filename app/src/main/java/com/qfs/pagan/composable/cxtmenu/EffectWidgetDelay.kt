package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.DelayEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: DelayEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val echo = remember { mutableIntStateOf(event.echo) }
    val numerator = remember { mutableIntStateOf(event.numerator) }
    val denominator = remember { mutableIntStateOf(event.denominator) }
    val fade = remember { mutableFloatStateOf(event.fade) }

    val echo_expanded = remember { mutableStateOf(false) }
    if (echo_expanded.value) {
        val requester = remember { FocusRequester() }
        IntegerInput(
            value = echo,
            text_align = TextAlign.Center,
            on_focus_enter = {},
            on_focus_exit = { value ->
                println("$value FUCK==================")
                value?.let { event.echo = it }
                dispatcher.set_effect_at_cursor(event)
                echo_expanded.value = false
            },
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .focusRequester(requester)
                .width(64.dp),
            minimum = 1,
        ) {
            event.echo = it
            dispatcher.set_effect_at_cursor(event)
            echo_expanded.value = false
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
    } else {
        Button(
            onClick = { echo_expanded.value = !echo_expanded.value },
            content = { Text("${echo.value}") }
        )
    }
    Spacer(Modifier.width(2.dp))
    val numerator_expanded = remember { mutableStateOf(false) }
    if (numerator_expanded.value) {
        val requester = remember { FocusRequester() }
        IntegerInput(
            value = numerator,
            text_align = TextAlign.Center,
            on_focus_enter = {},
            on_focus_exit = { value ->
                value?.let { event.numerator = it }
                dispatcher.set_effect_at_cursor(event)
                numerator_expanded.value = false
            },
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .focusRequester(requester)
                .width(64.dp),
            minimum = 1,
        ) {
            event.numerator = it
            dispatcher.set_effect_at_cursor(event)
            numerator_expanded.value = false
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
    } else {
        Button(
            onClick = { numerator_expanded.value = !numerator_expanded.value },
            content = { Text("${numerator.value}") }
        )
    }

    Spacer(Modifier.width(2.dp))

    val denominator_expanded = remember { mutableStateOf(false) }
    if (denominator_expanded.value) {
        val requester = remember { FocusRequester() }
        IntegerInput(
            value = denominator,
            text_align = TextAlign.Center,
            on_focus_enter = {},
            on_focus_exit = { value ->
                value?.let { event.denominator = it }
                dispatcher.set_effect_at_cursor(event)
                denominator_expanded.value = false

            },
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .focusRequester(requester)
                .width(64.dp),
            minimum = 1,
        ) {
            event.denominator = it
            dispatcher.set_effect_at_cursor(event)
            denominator_expanded.value = false
        }

        LaunchedEffect(Unit) {
            requester.requestFocus()
        }
    } else {
        Button(
            onClick = { denominator_expanded.value = !denominator_expanded.value },
            content = { Text("${denominator.value}") }
        )
    }

    Spacer(Modifier.width(2.dp))

    Slider(
        value = fade.floatValue,
        steps = 100,
        valueRange = 0F .. 1F,
        onValueChange = {
            event.fade = 1F - it
            dispatcher.set_effect_at_cursor(event)
        }
    )

    EffectTransitionButton(event.transition, dispatcher, is_initial)
}

