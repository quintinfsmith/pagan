package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
fun RowScope.VelocityEventMenu(ui_facade: ViewModelEditorState, dispatcher: ActionTracker, event: OpusVelocityEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line
    TextCMenuButton(
        modifier = Modifier
            .width(dimensionResource(R.dimen.contextmenu_button_width))
            .fillMaxHeight(),
        contentPadding = PaddingValues(4.dp),
        text = "%02d".format((event.value * 100).roundToInt()),
        onClick = {},
        onLongClick = {
            event.value = 1F
            dispatcher.set_effect_at_cursor(event)
        }
    )

    Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))

    Slider(
        valueRange = 0F .. 1.27F,
        value = event.value,
        onValueChange = {
            event.value = it
            dispatcher.set_effect_at_cursor(event)
        },
        modifier = Modifier
            .weight(1F)
            .fillMaxHeight()
    )

    EffectTransitionButton(event.transition, dispatcher, is_initial)
}