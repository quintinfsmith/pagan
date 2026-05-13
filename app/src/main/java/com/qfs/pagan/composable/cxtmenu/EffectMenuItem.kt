package com.qfs.pagan.composable.cxtmenu

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.EffectResourceMap
import com.qfs.pagan.composable.wrappers.Text
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.ui.theme.Dimensions

@Composable
fun RowScope.EffectMenuItem(ctl_type: EffectType) {
    Icon(
        modifier = Modifier.width(Dimensions.EffectDialogIconWidth),
        painter = painterResource(EffectResourceMap[ctl_type].icon),
        contentDescription = stringResource(EffectResourceMap[ctl_type].name)
    )
    Text(EffectResourceMap[ctl_type].name, Modifier.weight(1F))
}