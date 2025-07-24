package com.qfs.pagan.structure.opusmanager.base.activecontroller

import com.qfs.pagan.structure.opusmanager.base.ControlTransition

class ControllerProfile() {
    val values = mutableListOf<Triple<Pair<Float, Float>, Pair<FloatArray, FloatArray>, ControlTransition>>()
    fun add(start: Float, end: Float, start_values: FloatArray, end_values: FloatArray, transition: ControlTransition) {
        this.values.add(
            Triple(
                Pair(start, end),
                Pair(start_values, end_values),
                transition
            )
        )
    }
}

