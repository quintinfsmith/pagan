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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
fun Modifier.conditional_drag(
    is_dragging: MutableState<Boolean>,
    on_drag_start: (Float) -> Unit = {},
    on_drag_stop: () -> Unit = {},
    on_drag: (Float) -> Unit = {},
    orientation: Orientation = Orientation.Vertical,
): Modifier {
    var drag_offset = 0F
    var was_dragging = false
    return this then Modifier
        .pointerInput(is_dragging.value) {
            if (is_dragging.value) {
                println("CAN DRAG!")
                was_dragging = true
                awaitPointerEventScope {
                    // get drag start
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            event.changes.forEach { change ->
                                drag_offset = if (orientation == Orientation.Vertical) {
                                    change.position.y
                                } else {
                                    change.position.x
                                }

                                on_drag_start(drag_offset)
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
                                        drag_offset += if (orientation == Orientation.Vertical) {
                                            delta.y
                                        } else {
                                            delta.x
                                        }

                                        on_drag(drag_offset)
                                    }
                                } else {
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
            } else if (was_dragging) {
                was_dragging = false
                on_drag_stop()
            }
        }
}

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
    val timeout: Long = getLongPressTimeout().toLong()
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