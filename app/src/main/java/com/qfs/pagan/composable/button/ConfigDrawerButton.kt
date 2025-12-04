package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.ActionTracker
import com.qfs.pagan.R

@Composable
fun ConfigDrawerTopButton(modifier: Modifier = Modifier, content: (@Composable RowScope.() -> Unit), onClick: () -> Unit) {
    BetterButton(
        modifier = modifier.height(dimensionResource(R.dimen.config_icon_size)),
        contentPadding = PaddingValues(dimensionResource(R.dimen.config_icon_padding)),
        onClick = onClick,
        content = content,
    )
}

@Composable
fun ConfigDrawerBottomButton(modifier: Modifier = Modifier, icon: Int, description: Int, enabled: Boolean = true, onClick: () -> Unit) {
    BetterButton(
        modifier = modifier.height(dimensionResource(R.dimen.config_icon_size)),
        contentPadding = PaddingValues(dimensionResource(R.dimen.config_icon_padding)),
        onClick = onClick,
        enabled = enabled,
        content = {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description)
            )
        }
    )
}

@Composable
fun ConfigDrawerChannelLeftButton(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit, onClick: () -> Unit) {
    BetterButton(
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(50, 0, 0, 50),
        modifier = modifier,
        onClick = onClick,
        content = content
    )
}
@Composable
fun ConfigDrawerChannelRightButton(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit, onClick: () -> Unit) {
    BetterButton(
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(0, 50, 50, 0),
        modifier = modifier,
        onClick = onClick,
        content = content
    )
}
