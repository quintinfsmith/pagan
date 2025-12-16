package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

@Composable
fun EffectTransitionButton(transition: EffectTransition, dispatcher: ActionTracker, is_initial: Boolean, modifier: Modifier = Modifier) {
    if (is_initial) {
        Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))
        Spacer(modifier.width(dimensionResource(R.dimen.contextmenu_button_width)))
    } else {
        Spacer(Modifier.width(dimensionResource(R.dimen.contextmenu_padding)))
        IconCMenuButton(
            modifier = modifier
                .width(dimensionResource(R.dimen.contextmenu_button_width))
                .fillMaxHeight(),
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
}