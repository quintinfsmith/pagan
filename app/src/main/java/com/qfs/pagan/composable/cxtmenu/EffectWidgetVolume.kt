package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
        onClick = {
            dispatcher.dialog_number_input(R.string.dlg_set_volume, 0, default = (event.value * 100).toInt()) {
                event.value = it.toFloat() / 100F
                dispatcher.set_effect_at_cursor(event)
            }
        },
        onLongClick = {
            event.value = 1F
            dispatcher.set_effect_at_cursor(event)
        }
    )
    CMPadding()
    Slider(
        modifier = Modifier
            .weight(1F)
            .fillMaxHeight(),
        value = event.value,
        valueRange = 0F .. 1.27F,
        onValueChange = {
            event.value = it
            dispatcher.set_effect_at_cursor(event)
        },
    )

    EffectTransitionButton(event.transition, dispatcher, is_initial)
}