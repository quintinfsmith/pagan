package com.qfs.pagan.composable.cxtmenu

import android.view.ViewConfiguration.getLongPressTimeout
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Composable
fun Modifier.dragging_scroll(
    is_dragging: MutableState<Boolean>,
    scroll_state: ScrollState,
    interaction_source: MutableInteractionSource,
    orientation: Orientation = Orientation.Vertical
): Modifier {
    val drag_offset = remember { mutableStateOf(0F) }
    val scope = rememberCoroutineScope()

    return this then Modifier
        .then(
            if (orientation == Orientation.Vertical) {
                Modifier.verticalScroll(scroll_state, enabled = !is_dragging.value)
            } else {
                Modifier.horizontalScroll(scroll_state, enabled = !is_dragging.value)
            }
        )
        .pointerInput(is_dragging.value) {
            if (is_dragging.value) {
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
                                if (is_dragging.value) {
                                    event.changes.forEach { change ->
                                        val delta = change.position - change.previousPosition
                                        drag_offset.value += if (orientation == Orientation.Vertical) {
                                            delta.y
                                        } else {
                                            delta.x
                                        }

                                        val viewport_height = scroll_state.viewportSize.toFloat()
                                        val div = 4F
                                        val active_zone_height = viewport_height / div
                                        val max_scroll_speed = 40
                                        val relative_y = drag_offset.value
                                        val downscroll_y_position = active_zone_height * (div - 1F)
                                        val factor: Float = if (relative_y < active_zone_height) {
                                            -1F * (relative_y / active_zone_height).pow(2F)
                                        } else if (relative_y > downscroll_y_position) {
                                            ((relative_y - downscroll_y_position) / active_zone_height).pow(2F)
                                        } else {
                                            return@forEach
                                        }

                                        val scroll_diff = max(
                                            0F,
                                            min(
                                                scroll_state.maxValue.toFloat(),
                                                (max_scroll_speed * factor) + scroll_state.value
                                            )
                                        ) - max(0F, min(scroll_state.maxValue, scroll_state.value).toFloat())

                                        if (scroll_diff != 0F) {
                                            drag_offset.value += scroll_diff

                                            scope.launch {
                                                scroll_state.scrollBy(scroll_diff)
                                            }
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
    val timeout: Long = getLongPressTimeout().toLong()
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
                                while (is_pressed) {
                                    when (awaitPointerEvent().type) {
                                        PointerEventType.Release -> { is_pressed = false }
                                        else -> event.changes.forEach { it.consume() }
                                    }
                                }
                                false
                            } ?: true) {
                                onPress()
                            }
                        }
                        PointerEventType.Release -> onRelease()
                        else -> event.changes.forEach { it.consume() }
                    }
                }
            }
        }
}



@Composable
fun Modifier.long_press_draggable(
    onDragStarted: (Offset) -> Unit,
    onDragStopped: () -> Unit,
    onDrag: (Float) -> Unit,
    orientation: Orientation = Orientation.Vertical
): Modifier {
    val is_really_dragging = remember { mutableStateOf(false) }
    val initial_drag = remember { mutableStateOf(Offset(0F,0F))}
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

// Composable...

    return this then Modifier
        // .pointerInput(Unit) {
        //     detectDragGesturesAfterLongPress { _, i ->
        //         println(" --$i")
        //     }
        // }
        // .pointerInput(Unit) {
        //     awaitPointerEventScope {
        //         while (true) {
        //             val event = awaitPointerEvent()
        //             when (event.type) {
        //                 PointerEventType.Press -> {
        //                     if (!is_really_dragging.value) {
        //                         is_really_dragging.value = withTimeoutOrNull(500) {
        //                             while (awaitPointerEvent().type != PointerEventType.Release) { }
        //                             false
        //                         } ?: true
        //                         if (is_really_dragging.value) {
        //                             println("Start Drag")
        //                             onDragStarted(initial_drag.value)
        //                         }
        //                     }
        //                 }

        //                 PointerEventType.Release -> {
        //                     println("Release")
        //                     is_really_dragging.value = false
        //                     onDragStopped()
        //                 }
        //                 else -> { }
        //             }
        //         }
        //     }
        // }
        // .pointerInput( Unit ) {
        //     detectDragGestures(
        //         onDragStart = { },
        //         onDragEnd = { is_really_dragging.value = false },
        //         onDrag = { _, offset ->
        //             initial_drag.value += offset
        //             if (is_really_dragging.value) {
        //                 println("is reqally dragging $offset:")
        //                 onDrag(offset.y)
        //             }
        //         }
        //     )
        // }
}