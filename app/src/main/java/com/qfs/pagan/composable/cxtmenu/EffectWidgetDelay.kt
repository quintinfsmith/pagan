package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.MagicInput
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

    MagicInput(echo, background_icon = R.drawable.icon_echo, modifier = Modifier.width(64.dp)) {
        event.echo = it
        dispatcher.set_effect_at_cursor(event)
    }
    Spacer(Modifier.width(2.dp))
    MagicInput(numerator, background_icon = R.drawable.icon_hz, modifier = Modifier.width(64.dp)) {
        event.numerator = it
        dispatcher.set_effect_at_cursor(event)
    }
    Spacer(Modifier.width(2.dp))
    MagicInput(denominator, background_icon = R.drawable.icon_hz, modifier = Modifier.width(64.dp)) {
        event.denominator = it
        dispatcher.set_effect_at_cursor(event)
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

