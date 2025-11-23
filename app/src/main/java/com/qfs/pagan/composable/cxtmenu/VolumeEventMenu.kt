package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.composable.button.TextCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.uibill.UIFacade

@Composable
fun VolumeEventMenu (ui_facade: UIFacade, dispatcher: ActionTracker, event: OpusVolumeEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    // Assume cursor is either Line or Single
    val active_line = ui_facade.line_data[cursor.ints[0]]
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
                dispatcher.set_volume_at_cursor(it)
                slider_position = it
                slider_option_index = (it * (options_playback.size - 1).toFloat()).toInt()
                view_model.configuration.sample_rate = options_playback[slider_option_index]
            },
            modifier = Modifier.fillMaxWidth().weight(2F)
        )
    }
}