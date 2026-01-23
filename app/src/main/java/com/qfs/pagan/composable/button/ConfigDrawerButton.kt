package com.qfs.pagan.composable.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qfs.pagan.R
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shapes

@Composable
fun ConfigDrawerTopButton(modifier: Modifier = Modifier, content: (@Composable RowScope.() -> Unit), onClick: () -> Unit) {
    Button(
        modifier = modifier.height(Dimensions.ConfigChannelButtonHeight),
        shape = RoundedCornerShape(12.dp),
        contentPadding = Dimensions.ConfigButtonPadding,
        onClick = onClick,
        content = content,
    )
}

@Composable
fun ConfigDrawerBottomButton(modifier: Modifier = Modifier, icon: Int, description: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        modifier = modifier
            .height(Dimensions.ConfigChannelBottomButtonHeight)
            .widthIn(dimensionResource(R.dimen.config_button_width)),
        shape = RoundedCornerShape(12.dp),
        contentPadding = Dimensions.ConfigButtonPadding,
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
fun ConfigDrawerChannelLeftButton(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit
) {
    Button(
        modifier = modifier.height(Dimensions.ConfigChannelButtonHeight),
        contentPadding = PaddingValues(4.dp),
        shape = Shapes.SectionButtonStart,
        colors = colors,
        onClick = onClick,
        content = content
    )
}
@Composable
fun ConfigDrawerChannelRightButton(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit)
{
    Button(
        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
        shape = Shapes.SectionButtonEnd,
        modifier = modifier.height(Dimensions.ConfigChannelButtonHeight),
        colors = colors,
        onClick = onClick,
        content = content
    )
}
