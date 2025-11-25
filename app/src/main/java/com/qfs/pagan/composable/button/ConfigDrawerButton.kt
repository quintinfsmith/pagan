package com.qfs.pagan.composable.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.qfs.pagan.ActionTracker

@Composable
fun ConfigDrawerButton(modifier: Modifier = Modifier, icon: Int, description: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description)
            )
        }
    )
}
