package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.viewmodel.ViewModelEditorState

@Composable
fun VelocityEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusVelocityEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line

    val height = dimensionResource(R.dimen.contextmenu_secondary_button_height)
    Row {
        TextCMenuButton(
            contentPadding = PaddingValues(4.dp),
            text = "%.2${event.value}f",
            onClick = {},
            onLongClick = {}
        )

        Slider(
            value = event.value,
            onValueChange = {
                event.value = it
                dispatcher.set_effect_at_cursor(event)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(2F)
                .height(height)
        )

        if (!is_initial) {
            EffectTransitionButton(event.transition, dispatcher)
        }
    }
}