package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.DropdownMenu
import com.qfs.pagan.composable.IntegerInput
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.composable.button.Button
import com.qfs.pagan.composable.button.ContextMenuButtonPadding
import com.qfs.pagan.composable.button.ContextMenuButtonShape
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

@Composable
fun RowScope.DelayEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: DelayEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val fade = remember { mutableFloatStateOf(event.fade) }
    val (channel, line_offset, beat, position) = ui_facade.get_location_ints()

    val default_colors = SliderDefaults.colors()
    val colors = default_colors.copy(
        activeTickColor = default_colors.inactiveTickColor,
        inactiveTickColor = default_colors.activeTickColor
    )

    val echo_label = remember { mutableStateOf(event.echo + 1) }
    val numerator_label = remember { mutableStateOf(event.numerator) }
    val denominator_label = remember { mutableStateOf(event.denominator) }

    IntegerInput(
        value = echo_label,
        on_focus_exit = {
            it?.let { echo_label.value = it }
        },
        //background_icon = R.drawable.icon_echo,
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
    Spacer(Modifier.width(2.dp).weight(1F))
    IntegerInput(
        numerator_label,
        //background_icon = R.drawable.icon_hz,
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
    IntegerInput(
        denominator_label,
        //background_icon = R.drawable.icon_hz,
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
            contentPadding = ContextMenuButtonPadding(),
            shape = ContextMenuButtonShape(),
            modifier = Modifier
                .height(dimensionResource(R.dimen.contextmenu_button_height))
                .width(dimensionResource(R.dimen.contextmenu_button_width)),
            content = {
                if (fade_expanded.value) {
                    Text("${(fade.floatValue * 100).roundToInt()}%")
                } else {
                    Icon(
                        painter = painterResource(R.drawable.icon_volume),
                        contentDescription = stringResource(R.string.cd_fade)
                    )
                }
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
                        rotationZ = 270f
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
                    event.fade = it
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

