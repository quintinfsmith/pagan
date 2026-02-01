package com.qfs.pagan.composable.button

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import com.qfs.pagan.composable.pressable
import com.qfs.pagan.ui.theme.Dimensions
import com.qfs.pagan.ui.theme.Shadows
import com.qfs.pagan.ui.theme.Shapes
import com.qfs.pagan.ui.theme.Typography

@Composable
fun NumberSelectorButton(
    modifier: Modifier = Modifier,
    index: Int,
    alternate: Boolean,
    selected: Boolean,
    highlighted: Boolean,
    default: Boolean,
    shape: Shape = Shapes.NumberSelectorButton,
    on_long_click: (Int) -> Unit,
    on_click: (Int) -> Unit,
) {
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
                        shape = shape,
                        shadow = Shadows.Button
                    )
                }
            )
            .background(background, shape)
            .combinedClickable(
                onClick = { on_click(index) },
                onLongClick = { on_long_click(index) }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                "$index",
                maxLines = 1,
                color = foreground,
                style = Typography.NoteSelector.copy(
                    fontWeight = if (selected) {
                        FontWeight.Bold
                    } else {
                        LocalTextStyle.current.fontWeight
                    }
                )
            )
            Canvas(Modifier.fillMaxSize()) {
                if (highlighted) {
                    drawCircle(
                        color = foreground,
                        radius = (size.height * .05F),
                        center = Offset(size.width * .3F, size.height * .2F)
                    )
                }
                if (default) {
                    drawLine(
                        color = foreground,
                        start = Offset(
                            size.width / 5F,
                            size.height * .8F
                        ),
                        end = Offset(
                            size.width * 4F / 5F,
                            size.height * .8F
                        ),
                        strokeWidth = 2.5F
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.NumberSelector(
    progression: IntProgression,
    selected: Int?,
    highlighted: Int?,
    default: Int?,
    alternate: Boolean,
    shape_start: Shape = Shapes.NumberSelectorButton,
    shape_middle: Shape = Shapes.NumberSelectorButton,
    shape_end: Shape = Shapes.NumberSelectorButton,
    on_long_click: (Int) -> Unit,
    on_click: (Int) -> Unit
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
            default = default == i,
            alternate = alternate,
            shape = when (i) {
                progression.first -> shape_start
                progression.last -> shape_end
                else -> shape_middle
            },
            on_long_click = on_long_click,
            on_click = on_click
        )
    }
}

@Composable
fun ColumnScope.NumberSelector(
    progression: IntProgression,
    selected: Int?,
    highlighted: Int?,
    default: Int?,
    alternate: Boolean,
    shape_start: Shape = Shapes.NumberSelectorButton,
    shape_middle: Shape = Shapes.NumberSelectorButton,
    shape_end: Shape = Shapes.NumberSelectorButton,
    on_long_click: (Int) -> Unit,
    on_click: (Int) -> Unit
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
            default = default == i,
            alternate = alternate,
            shape = when (i) {
                progression.first -> shape_start
                progression.last -> shape_end
                else -> shape_middle
            },
            on_long_click = on_long_click,
            on_click = on_click
        )
    }
}
