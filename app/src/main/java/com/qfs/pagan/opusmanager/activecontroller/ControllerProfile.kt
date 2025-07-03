package com.qfs.pagan.opusmanager.activecontroller

import com.qfs.pagan.opusmanager.ControlTransition

class ControllerProfile() {
    val values = mutableListOf<Triple<Pair<Float, Float>, Pair<Float, Float>, ControlTransition>>()
    fun add(start: Float, end: Float, start_value: Float, end_value: Float, transition: ControlTransition) {
        this.values.add(
            Triple(
                Pair(start, end),
                Pair(start_value, end_value),
                transition
            )
        )
    }
}

