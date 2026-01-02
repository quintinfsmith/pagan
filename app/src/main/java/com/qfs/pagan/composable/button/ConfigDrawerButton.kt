package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R

@Composable
fun ConfigDrawerTopButton(modifier: Modifier = Modifier, content: (@Composable RowScope.() -> Unit), onClick: () -> Unit) {
    Button(
        modifier = modifier.height(dimensionResource(R.dimen.config_icon_size)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.config_icon_padding_horizontal)),
        onClick = onClick,
        content = content,
    )
}

@Composable
fun ConfigDrawerBottomButton(modifier: Modifier = Modifier, icon: Int, description: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        modifier = modifier
            .widthIn(dimensionResource(R.dimen.config_button_width))
            .height(dimensionResource(R.dimen.config_icon_size)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.config_icon_padding_horizontal),
            vertical = dimensionResource(R.dimen.config_icon_padding_vertical)
        ),
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
    val corner_radius = dimensionResource(R.dimen.channel_option_corner_radius)
    Button(
        contentPadding = PaddingValues(4.dp),
        shape = RoundedCornerShape(corner_radius, 0.dp, 0.dp, corner_radius),
        modifier = modifier.height(dimensionResource(R.dimen.config_channel_button_height)),
        onClick = onClick,
        content = content
    )
}
@Composable
fun ConfigDrawerChannelRightButton(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit, onClick: () -> Unit) {
    val corner_radius = dimensionResource(R.dimen.channel_option_corner_radius)
    Button(
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(0.dp, corner_radius, corner_radius, 0.dp),
        modifier = modifier.height(dimensionResource(R.dimen.config_channel_button_height)),
        onClick = onClick,
        content = content
    )
}
