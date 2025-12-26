package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.MagicInput
import com.qfs.pagan.composable.SText
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun RowScope.TempoEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusTempoEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    val working_value = remember { mutableFloatStateOf(event.value) }
    val (channel, line_offset) = if (cursor.type == CursorMode.Line || cursor.type == CursorMode.Single) {
        val line_info = ui_facade.line_data[cursor.ints[0]]
        Pair(line_info.channel.value, line_info.line_offset.value)
    } else {
        Pair(null, null)
    }

    val (beat, position) = if (cursor.type == CursorMode.Single) {
        Pair(cursor.ints[1], cursor.ints.subList(2, cursor.ints.size))
    } else {
        Pair(null, null)
    }

    Spacer(Modifier.weight(1F))

    MagicInput(
        value = working_value,
        precision = 3,
        modifier = Modifier
            .fillMaxHeight()
            .width(dimensionResource(R.dimen.effect_tempo_input_width)),
        minimum = 0F,
        background_icon = R.drawable.icon_tempo,
        callback = { new_value: Float ->
            event.value = new_value
            if (beat != null) {
                dispatcher.set_effect(EffectType.Tempo, event, channel, line_offset, beat, position!!, lock_cursor = true)
            } else {
                dispatcher.set_initial_effect(EffectType.Tempo, event, channel, line_offset, lock_cursor = true)
            }
        }
    )

    ProvideContentColorTextStyle(
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = {
            SText(R.string.bpm, Modifier.padding(start = 4.dp))
        }
    )

    Spacer(Modifier.weight(1F))

    EffectTransitionButton(event.transition, dispatcher, is_initial)
}