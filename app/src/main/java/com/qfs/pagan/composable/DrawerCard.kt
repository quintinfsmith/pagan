package com.qfs.pagan.composable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.qfs.pagan.composable.button.ProvideContentColorTextStyle
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shadows
import com.qfs.pagan.ui.theme.Shapes

@Composable
fun DrawerCard(
    modifier: Modifier = Modifier.wrapContentWidth(),
    colors: CardColors = CardColors(
        contentColor = MaterialTheme.colorScheme.onSurface,
        containerColor = MaterialTheme.colorScheme.surface,
        disabledContentColor = Color.Gray,
        disabledContainerColor = Color.Green,
    ),
    elevation: CardElevation = CardDefaults.cardElevation(),
    shape: Shape = Shapes.Drawer,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ProvideContentColorTextStyle(contentColor = colors.contentColor) {
        Surface(
            modifier = modifier
                .dropShadow(
                    shape,
                    Shadows.ContextMenu
                )
                .wrapContentWidth()
                .then(if (border != null) modifier.border(border) else modifier),
            color = colors.containerColor,
            contentColor = colors.contentColor,
            shape = shape
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .padding(Dimensions.DialogPadding),
                horizontalAlignment = Alignment.End,
                content = content
            )
        }
    }
}