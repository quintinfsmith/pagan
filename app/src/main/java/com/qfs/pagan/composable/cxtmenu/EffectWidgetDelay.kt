package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.DropdownMenu
import com.qfs.pagan.composable.MagicInput
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.composable.button.Button
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
        activeTickColor = default_colors.activeTickColor,
        inactiveTrackColor = default_colors.activeTrackColor,
        inactiveTickColor = default_colors.inactiveTickColor,
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
    Text("/")
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
    Spacer(Modifier.weight(1F))

    val fade_expanded = remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center) {
        Button(
            content = {
                Icon(
                    painter = painterResource(R.drawable.icon_volume),
                    contentDescription = stringResource(R.string.cd_fade)
                )
            },
            onClick = {
                fade_expanded.value = !fade_expanded.value
            }
        )
        DropdownMenu(
            fade_expanded.value,
            onDismissRequest = { fade_expanded.value = false },
            modifier = Modifier
                .height(250.dp)
                .width(50.dp)
        ) {
            Slider(
                value = fade.floatValue,
                steps = 100,
                colors = colors,
                valueRange = 0F..1F,
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = 90f
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .weight(1F)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints(
                                minWidth = constraints.minHeight,
                                maxWidth = constraints.maxHeight,
                                minHeight = constraints.minWidth,
                                maxHeight = constraints.maxHeight,
                            )
                        )
                        layout(placeable.height, placeable.width) {
                            placeable.place(-placeable.width, 0)
                        }
                    },

                onValueChange = {
                    event.fade = (1F - it)
                    fade.floatValue = it
                    if (beat != null) {
                        dispatcher.set_effect(EffectType.Delay, event, channel, line_offset, beat, position!!)
                    } else {
                        dispatcher.set_initial_effect(EffectType.Delay, event, channel, line_offset)
                    }
                }
            )
        }
    }

    EffectTransitionButton(event, dispatcher, is_initial)
}

