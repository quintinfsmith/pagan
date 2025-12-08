package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

@Composable
fun VolumeEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusVolumeEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextCMenuButton(
            modifier = Modifier.fillMaxHeight(),
            text = "%02d".format((event.value * 100).roundToInt()),
            onClick = {},
            onLongClick = {}
        )

        Slider(
            modifier = Modifier.weight(1F).fillMaxHeight(),
            value = event.value,
            valueRange = 0F .. 1.27F,
            steps = 128,
            onValueChange = {
                event.value = it
                dispatcher.set_effect_at_cursor(event)
            },
        )

        if (!is_initial) {
            EffectTransitionButton(event.transition, dispatcher, Modifier.fillMaxHeight())
        }
    }
}