package com.qfs.pagan.composable.button

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun TopBarIcon(icon: Int, description: Int, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick ?: {}
            ),
        contentAlignment = Alignment.Center,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
                modifier = Modifier
                    .padding(vertical = 5.dp, horizontal = 16.dp)
                    .width(dimensionResource(R.dimen.topbar_icon_height))
                    .height(dimensionResource(R.dimen.topbar_icon_height))
            )
        }
    )
}

