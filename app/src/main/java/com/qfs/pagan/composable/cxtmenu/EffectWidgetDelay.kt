package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.uibill.UIFacade

@Composable
fun DelayEventMenu (ui_facade: UIFacade, dispatcher: ActionTracker, event: DelayEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line

    Row {
        // TextCMenuButton(
        //     text = "${event.value}",
        //     onClick = {},
        //     onLongClick = {}
        // )

        // Slider(
        //     value = event.value,
        //     onValueChange = { dispatcher.set_volume_at_cursor(it) },
        //     modifier = Modifier.fillMaxWidth().weight(2F)
        // )

        if (!is_initial) {
            EffectTransitionButton(event.transition, dispatcher)
        }
    }
}

