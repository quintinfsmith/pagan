package com.qfs.pagan

import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.pow
import kotlin.math.sqrt
// TODO: Find this function a better home
fun process_zoom(event: PointerEvent, callback: (Float, Float, Float) -> Unit) {
    if (event.type != PointerEventType.Move) return
    if (event.changes.size < 2) return

    val (lesser, larger) = if (event.changes[0].position.x < event.changes[1].position.x) {
        Pair(event.changes[0], event.changes[1])
    } else {
        Pair(event.changes[1], event.changes[0])
    }

    val diff_x = larger.position.x - lesser.position.x
    val diff_y = larger.position.y - lesser.position.y
    val current_diff = sqrt(diff_x.pow(2F) + diff_y.pow(2F))
    val previous_diff = sqrt(
        ((larger.position.x - larger.positionChange().x) - (lesser.position.x - lesser.positionChange().x)).pow(
            2F
        )
                + ((larger.position.y - larger.positionChange().y) - (lesser.position.y - lesser.positionChange().y)).pow(
            2F
        )
    )

    callback(
        current_diff - previous_diff,
        lesser.position.x + (diff_x / 2F),
        lesser.position.y + (diff_y / 2F),
    )
}