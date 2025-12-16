package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

@Composable
fun RowScope.VolumeEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusVolumeEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    TextCMenuButton(
        modifier = Modifier
            .width(dimensionResource(R.dimen.contextmenu_button_width))
            .fillMaxHeight(),
        text = "%02d".format((event.value * 100).roundToInt()),
        onClick = {},
        onLongClick = {}
    )

    Slider(
        modifier = Modifier
            .weight(1F)
            .fillMaxHeight(),
        value = event.value,
        valueRange = 0F .. 1.27F,
        steps = 128,
        onValueChange = {
            event.value = it
            dispatcher.set_effect_at_cursor(event)
        },
    )

    EffectTransitionButton(event.transition, dispatcher, is_initial)
}