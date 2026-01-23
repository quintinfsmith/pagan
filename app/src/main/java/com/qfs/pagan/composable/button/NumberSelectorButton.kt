package com.qfs.pagan.composable.button

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp
import com.qfs.pagan.composable.dashed_border
import com.qfs.pagan.composable.pressable
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shadows
import com.qfs.pagan.ui.theme.Shapes

@Composable
fun NumberSelectorButton(modifier: Modifier = Modifier, index: Int, alternate: Boolean, selected: Boolean, highlighted: Boolean, callback: (Int) -> Unit) {
    val (background, foreground) = if (selected) {
        Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
    } else if (!alternate) {
        Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
    } else {
        Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
    }

    val pressed = remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(Dimensions.NumberSelectorButtonHeight)
            .pressable(pressed)
            .then(
                if (selected || pressed.value) {
                    Modifier
                } else {
                    Modifier.dropShadow(
                        shape = Shapes.NumberSelectorButton,
                        shadow = Shadows.Button
                    )
                }
            )

            .background(background, Shapes.NumberSelectorButton)
            .combinedClickable(
                onClick = {
                    callback(index)
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (highlighted) {
                        Modifier
                            .padding(Dimensions.NumberSelectorHighlightedBorderPadding)
                            .dashed_border(foreground, Shapes.NumberSelectorButton)
                    } else {
                        Modifier
                    }
                )
        ) {
            Text(
                "$index",
                maxLines = 1,
                color = foreground
            )
        }
    }
}

@Composable
fun RowScope.NumberSelector(
    progression: IntProgression,
    selected: Int?,
    highlighted: Int?,
    alternate: Boolean,
    callback: (Int) -> Unit
) {
    for (i in progression) {
        if (i != progression.first) {
            Spacer(Modifier.width(Dimensions.NumberSelectorSpacing))
        }
        NumberSelectorButton(
            modifier = Modifier.weight(1F),
            index = i,
            selected = selected == i,
            highlighted = highlighted == i,
            alternate = alternate,
            callback = callback
        )
    }
}

@Composable
fun ColumnScope.NumberSelector(
    progression: IntProgression,
    selected: Int?,
    highlighted: Int?,
    alternate: Boolean,
    callback: (Int) -> Unit
) {
    for (i in progression) {
        if (i != progression.first) {
            Spacer(Modifier.height(Dimensions.NumberSelectorSpacing))
        }
        NumberSelectorButton(
            modifier = Modifier.weight(1F),
            index = i,
            selected = selected == i,
            highlighted = highlighted == i,
            alternate = alternate,
            callback = callback
        )
    }
}
