package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.ui.theme.Dimensions


@Composable
fun TopBarIcon(icon: Int, description: Int, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color = Color.Transparent, shape = CircleShape)
            .size(
                Dimensions.TopBarIconSize,
                Dimensions.TopBarIconSize
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick ?: {}
            ),
        contentAlignment = Alignment.Center,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
            )
        }
    )
}


@Composable
fun TopBarNoIcon() {
    Box(
        modifier = Modifier
            .size(Dimensions.TopBarIconSize, Dimensions.TopBarIconSize)
    )
}
