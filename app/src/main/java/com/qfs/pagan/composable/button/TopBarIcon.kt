package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.R

@Composable
fun TopBarIcon(icon: Int, description: Int, callback: () -> Unit) {
    val v_padding = dimensionResource(R.dimen.topbar_icon_padding_v)
    IconButton(
        onClick = callback,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .padding(v_padding)
                    .height(dimensionResource(R.dimen.topbar_icon_height))
            )
        }
    )
}

