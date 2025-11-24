package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.uibill.UIFacade

@Composable
fun VelocityEventMenu (ui_facade: UIFacade, dispatcher: ActionTracker, event: OpusVelocityEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line

    Row {
        TextCMenuButton(
            text = "${event.value}",
            onClick = {},
            onLongClick = {}
        )

        Slider(
            value = event.value,
            onValueChange = {
                event.value = it
                dispatcher.set_effect_at_cursor(event)
            },
            modifier = Modifier.fillMaxWidth().weight(2F)
        )

        if (!is_initial) {
            EffectTransitionButton(event.transition, dispatcher)
        }
    }
}