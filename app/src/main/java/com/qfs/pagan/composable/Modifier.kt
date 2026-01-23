package com.qfs.pagan.composable

import android.view.ViewConfiguration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
fun Modifier.conditional_drag(
    is_dragging: MutableState<Boolean>,
    on_drag_start: (Float) -> Unit = {},
    on_drag_stop: () -> Unit = {},
    on_drag: (Float) -> Unit = {},
    scroll_state: ScrollState? = null,
    orientation: Orientation = Orientation.Vertical,
): Modifier {
    val was_dragging = remember { mutableStateOf(false) }
    val working_position = remember { mutableStateOf(0F) }
    if (is_dragging.value) {
        was_dragging.value = true
    } else {
        if (was_dragging.value) {
            on_drag_stop()
        }
        was_dragging.value = false
    }

    return this then Modifier
        .onPlaced { coordinates ->
            working_position.value = if (orientation == Orientation.Vertical) {
                coordinates.positionInParent().y
            } else {
                coordinates.positionInParent().x
            }
        }
        .pointerInput(is_dragging.value) {
            if (is_dragging.value) {
                awaitPointerEventScope {
                    var relative_start_position = 0F
                    var absolute_start_position = 0F
                    var absolute_latest_position = 0F
                    // get drag start
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            val change = event.changes.first()
                            relative_start_position = if (orientation == Orientation.Vertical) {
                                change.position.y
                            } else {
                                change.position.x
                            }

                            absolute_latest_position = relative_start_position + working_position.value

                            on_drag_start(relative_start_position)

                            break
                        }
                    }

                    // Main drag loop
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move -> {
                                if (is_dragging.value) {
                                    val change = event.changes.first()
                                    val absolute_current_position = if (orientation == Orientation.Vertical) {
                                        change.position.y
                                    } else {
                                        change.position.x
                                    } + working_position.value

                                    val delta = absolute_current_position - absolute_latest_position
                                    absolute_latest_position = absolute_current_position
                                    on_drag(delta)
                                } else {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
            }
        }
}

@Composable
fun Modifier.dragging_scroll(
    is_dragging: Boolean,
    scroll_state: ScrollState,
    orientation: Orientation = Orientation.Vertical,
): Modifier {
    val drag_offset = remember { mutableStateOf(0F) }
    val scope = rememberCoroutineScope()

    return this then Modifier
        .then(
            if (orientation == Orientation.Vertical) {
                Modifier.verticalScroll(
                    scroll_state,
                    enabled = !is_dragging,
                    overscrollEffect = null
                )
            } else {
                Modifier.horizontalScroll(
                    scroll_state,
                    enabled = !is_dragging,
                    overscrollEffect = null
                )
            }
        )
        .pointerInput(is_dragging) {
            if (is_dragging) {
                awaitPointerEventScope {
                    // get drag start
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            event.changes.forEach { change ->
                                drag_offset.value = if (orientation == Orientation.Vertical) {
                                    change.position.y
                                } else {
                                    change.position.x
                                } - scroll_state.value
                            }
                            break
                        }
                    }

                    // Main drag loop
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move -> {
                                if (is_dragging) {
                                    event.changes.forEach { change ->
                                        val delta = change.position - change.previousPosition
                                        drag_offset.value += if (orientation == Orientation.Vertical) {
                                            delta.y
                                        } else {
                                            delta.x
                                        }

                                        val viewport_height = scroll_state.viewportSize.toFloat()
                                        val div = 3F
                                        val active_zone_height = viewport_height / div
                                        val max_scroll_speed = 40
                                        val relative_y = drag_offset.value
                                        val downscroll_y_position = active_zone_height * (div - 1F)
                                        val factor: Float = if (relative_y < active_zone_height) {
                                            -1F * ((active_zone_height - relative_y) / active_zone_height).pow(2F)
                                        } else if (relative_y > downscroll_y_position) {
                                            ((relative_y - downscroll_y_position) / active_zone_height).pow(2F)
                                        } else {
                                            return@forEach
                                        }

                                        scope.launch {
                                            scroll_state.scrollBy(max_scroll_speed * factor)
                                        }

                                    }
                                } else {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
            }
        }
}


@Composable
fun Modifier.long_press(
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {}
): Modifier {
    val timeout: Long = ViewConfiguration.getLongPressTimeout().toLong()
    val move_threshold = 3
    var is_pressed = false
    return this then Modifier
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Press -> {
                            is_pressed = true
                            if (withTimeoutOrNull(timeout) {
                                var move_action_count = 0 // allow slight finger movement
                                while (is_pressed) {
                                    val next_event = awaitPointerEvent()
                                    when (next_event.type) {
                                        PointerEventType.Move -> {
                                            if (++move_action_count == move_threshold) {
                                                is_pressed = false
                                            }
                                        }
                                        PointerEventType.Release -> {
                                            is_pressed = false
                                        }
                                    }
                                }
                                false
                            } ?: true) {
                                onPress()
                            }
                        }
                        PointerEventType.Release -> onRelease()
                    }
                }
            }
        }
}

@Composable
fun Modifier.dashed_border(
    color: Color,
    shape: Shape,
    width: Dp = 2.dp,
    dash: Dp = 4.dp,
    gap: Dp = 4.dp,
    cap: StrokeCap = StrokeCap.Round
) = this.drawWithContent {
    drawContent()
    drawOutline(
        style = Stroke(
            cap = cap,
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dash.toPx(), gap.toPx())
            )
        ),
        outline = shape.createOutline(
            size = this.size,
            layoutDirection = this.layoutDirection,
            density = this
        ),
        brush = SolidColor(color)
    )
}

@Composable
fun Modifier.pressable(is_pressed: MutableState<Boolean>): Modifier {
    return this then Modifier
        .pointerInput(Unit) {
            awaitPointerEventScope {
                // get drag start
                while (true) {
                    val event = awaitPointerEvent()
                    when (event.type) {
                        PointerEventType.Press -> {
                            is_pressed.value = true
                        }
                        PointerEventType.Release -> {
                            is_pressed.value = false
                        }
                    }
                    event.changes.forEach { it.consume() }
                }
            }
        }
}