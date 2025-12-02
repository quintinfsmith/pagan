package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R

@Composable
fun ConfigDrawerButton(modifier: Modifier = Modifier, icon: Int, description: Int, onClick: () -> Unit) {
    Button(
        modifier = modifier.height(dimensionResource(R.dimen.config_icon_size)),
        onClick = onClick,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description)
            )
        }
    )
}
