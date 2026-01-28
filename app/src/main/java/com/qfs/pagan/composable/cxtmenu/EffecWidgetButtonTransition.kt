package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R
import com.qfs.pagan.composable.button.IconCMenuButton
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun EffectTransitionButton(event: EffectEvent, dispatcher: ActionTracker, is_initial: Boolean, modifier: Modifier = Modifier) {
    val transition = event.transition
    if (!is_initial) {
        Spacer(Modifier.width(Dimensions.ContextMenuPadding))
        IconCMenuButton(
            modifier = modifier
                .height(Dimensions.ContextMenuButtonHeight)
                .width(Dimensions.ContextMenuButtonWidth),
            onClick = {
                dispatcher.set_effect_transition(event)
            },
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