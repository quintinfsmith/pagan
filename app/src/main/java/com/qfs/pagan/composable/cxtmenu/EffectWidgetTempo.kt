package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.FloatInput
import com.qfs.pagan.composable.IntegerInput
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
    val (channel, line_offset, beat, position) = ui_facade.get_location_ints()

    Spacer(Modifier.weight(1F))

    val tempo_key = remember { mutableStateOf(false) }
    val tempo_label = remember { mutableFloatStateOf(event.value) }
    key(tempo_key.value) {
        FloatInput(
            tempo_label,
            precision = 3,
            on_focus_exit = {
                tempo_label.floatValue = event.value
                tempo_key.value = !tempo_key.value
            },
            prefix = {
                Icon(
                    modifier = Modifier.width(32.dp),
                    painter = painterResource(R.drawable.icon_tempo),
                    contentDescription = null
                )
            },
            minimum = 1F,
            contentPadding = PaddingValues(0.dp),
            text_align = TextAlign.Center,
            modifier = Modifier
                .height(41.dp)
                .width(IntrinsicSize.Min)
        ) {
            event.value = it
            if (beat != null) {
                dispatcher.set_effect(EffectType.Tempo, event, channel, line_offset, beat, position!!, lock_cursor = true)
            } else {
                dispatcher.set_initial_effect(EffectType.Tempo, event, channel, line_offset, lock_cursor = true)
            }
        }
    }

    ProvideContentColorTextStyle(
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = {
            SText(R.string.bpm, Modifier.padding(start = 4.dp))
        }
    )

    Spacer(Modifier.weight(1F))

    EffectTransitionButton(event, dispatcher, is_initial)
}