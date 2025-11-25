package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.FloatInput
import com.qfs.pagan.composable.SText
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun TempoEventMenu (ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusTempoEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val working_value = remember { mutableFloatStateOf(event.value) }
    Row(horizontalArrangement = Arrangement.Center) {
        Icon(
            painter = painterResource(R.drawable.icon_tempo),
            contentDescription = "",
            tint = colorResource(R.color.context_menu_background_icon)
        )
        FloatInput(value = working_value, precision = 3) { new_value ->
            event.value = new_value
            dispatcher.set_effect_at_cursor(event)
        }
        SText(R.string.bpm)
    }

    if (!is_initial) {
        EffectTransitionButton(event.transition, dispatcher)
    }
}