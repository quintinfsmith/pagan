package com.qfs.pagan.composable.cxtmenu

import androidx.compose.runtime.Composable
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.uibill.UIFacade

@Composable
fun ReverbEventMenu (ui_facade: UIFacade, dispatcher: ActionTracker, event: OpusReverbEvent) {
    val cursor = ui_facade.active_cursor.value ?: return
    val is_initial = cursor.type == CursorMode.Line

    // Row {
    //     TextCMenuButton(
    //         text = "${event.value}",
    //         onClick = {},
    //         onLongClick = {}
    //     )

    //     Slider(
    //         value = event.value,
    //         onValueChange = { dispatcher.set_volume_at_cursor(it) },
    //         modifier = Modifier.fillMaxWidth().weight(2F)
    //     )

    //     if (!is_initial) {
    //         IconCMenuButton(
    //             onClick = { dispatcher.set_ctl_transition() },
    //             icon = when (event.transition) {
    //                 EffectTransition.Instant -> R.drawable.icon_transition_immediate
    //                 EffectTransition.Linear -> R.drawable.icon_transition_linear
    //                 EffectTransition.RInstant -> R.drawable.icon_transition_rimmediate
    //                 EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
    //             },
    //             description = R.string.cd_show_effect_controls
    //         )
    //     }
    // }
}