package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.MagicInput
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.DelayEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: DelayEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val echo = remember { mutableIntStateOf(event.echo + 1) }
    val numerator = remember { mutableIntStateOf(event.numerator) }
    val denominator = remember { mutableIntStateOf(event.denominator) }
    val fade = remember { mutableFloatStateOf(1F - event.fade) }
    val (channel, line_offset, beat, position) = ui_facade.get_location_ints()

    val default_colors = SliderDefaults.colors()
    val colors = SliderColors(
        thumbColor = default_colors.thumbColor,
        activeTrackColor = default_colors.inactiveTrackColor,
        activeTickColor = default_colors.inactiveTickColor,
        inactiveTrackColor = default_colors.activeTrackColor,
        inactiveTickColor = default_colors.activeTickColor,
        disabledThumbColor = default_colors.disabledThumbColor,
        disabledActiveTrackColor = default_colors.disabledActiveTrackColor,
        disabledActiveTickColor = default_colors.disabledActiveTickColor,
        disabledInactiveTrackColor = default_colors.disabledInactiveTrackColor,
        disabledInactiveTickColor = default_colors.disabledInactiveTickColor
    )

    MagicInput(
        echo,
        background_icon = R.drawable.icon_echo,
        modifier = Modifier
            .fillMaxHeight()
            .width(54.dp)
    ) {
        event.echo = (it - 1)
        if (beat != null) {
            dispatcher.set_effect(EffectType.Delay, event, channel, line_offset, beat, position!!)
        } else {
            dispatcher.set_initial_effect(EffectType.Delay, event, channel, line_offset)
        }
    }
    Spacer(Modifier.width(2.dp))
    MagicInput(
        numerator,
        background_icon = R.drawable.icon_hz,
        modifier = Modifier
            .fillMaxHeight()
            .width(54.dp)
    ) {
        event.numerator = it
        if (beat != null) {
            dispatcher.set_effect(EffectType.Delay, event, channel, line_offset, beat, position!!)
        } else {
            dispatcher.set_initial_effect(EffectType.Delay, event, channel, line_offset)
        }
    }
    Spacer(Modifier.width(2.dp))
    MagicInput(
        denominator,
        background_icon = R.drawable.icon_hz,
        modifier = Modifier
            .fillMaxHeight()
            .width(54.dp)
    ) {
        event.denominator = it
        if (beat != null) {
            dispatcher.set_effect(EffectType.Delay, event, channel, line_offset, beat, position!!)
        } else {
            dispatcher.set_initial_effect(EffectType.Delay, event, channel, line_offset)
        }
    }
    Spacer(Modifier.width(2.dp))
    Slider(
        value = fade.floatValue,
        steps = 100,
        colors = colors,
        valueRange = 0F .. 1F,
        onValueChange = {
            event.fade = 1F - it
            fade.floatValue = it
            if (beat != null) {
                dispatcher.set_effect(EffectType.Delay, event, channel, line_offset, beat, position!!)
            } else {
                dispatcher.set_initial_effect(EffectType.Delay, event, channel, line_offset)
            }
        }
    )

    EffectTransitionButton(event, dispatcher, is_initial)
}

