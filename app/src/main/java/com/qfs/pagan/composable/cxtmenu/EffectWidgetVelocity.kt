package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.Slider
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState
import kotlin.math.roundToInt

@Composable
fun VelocityEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusVelocityEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line

    val height = dimensionResource(R.dimen.contextmenu_secondary_button_height)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextCMenuButton(
            contentPadding = PaddingValues(4.dp),
            text = "%02d".format((event.value * 100).roundToInt()),
            onClick = {},
            onLongClick = {}
        )

        Slider(
            valueRange = 0F .. 1.27F,
            value = event.value,
            onValueChange = {
                event.value = it
                dispatcher.set_effect_at_cursor(event)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
                .height(height)
        )

        if (!is_initial) {
            EffectTransitionButton(event.transition, dispatcher)
        }
    }
}