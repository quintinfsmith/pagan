package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.PanEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusPanEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val default_colors = SliderDefaults.colors()
    val colors = SliderColors(
        thumbColor = default_colors.thumbColor,
        activeTrackColor = default_colors.activeTrackColor,
        activeTickColor = default_colors.activeTickColor,
        inactiveTrackColor = default_colors.activeTrackColor,
        inactiveTickColor = default_colors.activeTickColor,
        disabledThumbColor = default_colors.disabledThumbColor,
        disabledActiveTrackColor = default_colors.disabledActiveTrackColor,
        disabledActiveTickColor = default_colors.disabledActiveTickColor,
        disabledInactiveTrackColor = default_colors.disabledInactiveTrackColor,
        disabledInactiveTickColor = default_colors.disabledInactiveTickColor
    )

    key(ui_facade.active_event.value.hashCode()) {
        val working_value = remember { mutableFloatStateOf(event.value) }
        Box(modifier = Modifier.weight(1F)) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .background(colors.thumbColor, CircleShape)
                    .height(12.dp)
                    .width(12.dp)
            )
            Slider(
                value = working_value.floatValue,
                onValueChange = {
                    working_value.floatValue = it
                },
                onValueChangeFinished = {
                    event.value = working_value.floatValue
                    dispatcher.set_effect_at_cursor(event)
                },
                valueRange = -1F..1F,
                steps = 21,
                colors = colors,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
            )
        }
    }

    key(ui_facade.active_event.value.hashCode()) {
        EffectTransitionButton(event, dispatcher, is_initial)
    }
}
