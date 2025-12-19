package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.DropdownMenu
import com.qfs.pagan.composable.DropdownMenuItem
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

    IntegerInput(
        value = echo,
        text_align = TextAlign.Center,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.width(64.dp),
        minimum = 1,
    ) {
        event.echo = it
        dispatcher.set_effect_at_cursor(event)
    }

    Spacer(Modifier.width(2.dp))

    IntegerInput(
        value = numerator,
        text_align = TextAlign.Center,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.width(64.dp),
        minimum = 1,
    ) {
        event.numerator = it
        dispatcher.set_effect_at_cursor(event)
    }

    Spacer(Modifier.width(2.dp))

    IntegerInput(
        value = denominator,
        text_align = TextAlign.Center,
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = Modifier.width(64.dp),
        minimum = 1,
    ) {
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

