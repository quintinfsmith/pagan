package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun DelayEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: DelayEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val echo = remember { mutableIntStateOf(event.echo) }
    val numerator = remember { mutableIntStateOf(event.numerator) }
    val denominator = remember { mutableIntStateOf(event.denominator) }
    val fade = remember { mutableFloatStateOf(event.fade) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IntegerInput(
            value = echo,
            minimum = 1,
        ) {
            event.echo = it
            dispatcher.set_effect_at_cursor(event)
        }

        IntegerInput(
            value = numerator,
            minimum = 1,
        ) {
            event.numerator = it
            dispatcher.set_effect_at_cursor(event)
        }

        IntegerInput(
            value = denominator,
            minimum = 1,
        ) {
            event.denominator = it
            dispatcher.set_effect_at_cursor(event)
        }
        Slider(
            value = fade.floatValue,
            steps = 100,
            valueRange = 0F .. 1F,
            onValueChange = {
                event.fade = 1F - it
                dispatcher.set_effect_at_cursor(event)
            }
        )

        if (!is_initial) {
            EffectTransitionButton(event.transition, dispatcher)
        }
    }
}

