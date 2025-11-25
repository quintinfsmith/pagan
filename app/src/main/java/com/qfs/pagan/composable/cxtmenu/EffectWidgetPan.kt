package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun PanEventMenu (ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusPanEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val default_colors = SliderDefaults.colors()
    val colors = SliderColors(
        thumbColor = default_colors.thumbColor,
        activeTrackColor = default_colors.activeTrackColor,
        activeTickColor = default_colors.activeTickColor,
        inactiveTrackColor = default_colors.inactiveTrackColor,
        inactiveTickColor = default_colors.inactiveTickColor,
        disabledThumbColor = default_colors.disabledThumbColor,
        disabledActiveTrackColor = default_colors.disabledActiveTrackColor,
        disabledActiveTickColor = default_colors.disabledActiveTickColor,
        disabledInactiveTrackColor = default_colors.disabledInactiveTrackColor,
        disabledInactiveTickColor = default_colors.disabledInactiveTickColor
    )
    val working_value = remember { mutableFloatStateOf(event.value) }
    Row {
        Slider(
            value = working_value.value,
            onValueChange = { },
            onValueChangeFinished = {
                event.value = working_value.value
                dispatcher.set_effect_at_cursor(event)
            },
            valueRange = -1F..1F,
            steps = 20,
            colors = colors,
            modifier = Modifier.fillMaxWidth().weight(1F),
        )

        if (!is_initial) {
            EffectTransitionButton(event.transition, dispatcher)
        }
    }
}
