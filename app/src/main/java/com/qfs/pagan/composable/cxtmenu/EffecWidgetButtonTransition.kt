package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

@Composable
fun EffectTransitionButton(transition: EffectTransition, dispatcher: ActionTracker, modifier: Modifier = Modifier) {
    IconCMenuButton(
        modifier = modifier.fillMaxHeight(),
        onClick = { /* dispatcher.set_ctl_transition() */ },
        icon = when (transition) {
            EffectTransition.Instant -> R.drawable.icon_transition_immediate
            EffectTransition.Linear -> R.drawable.icon_transition_linear
            EffectTransition.RInstant -> R.drawable.icon_transition_rimmediate
            EffectTransition.RLinear -> R.drawable.icon_transition_rlinear
        },
        description = R.string.cd_show_effect_controls
    )
}