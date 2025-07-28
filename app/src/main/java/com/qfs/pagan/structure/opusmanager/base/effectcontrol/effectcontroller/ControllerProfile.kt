package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

class ControllerProfile() {
    val values = mutableListOf<Triple<Pair<Float, Float>, Pair<FloatArray, FloatArray>, EffectTransition>>()
    fun add(start: Float, end: Float, start_values: FloatArray, end_values: FloatArray, transition: EffectTransition) {
        this.values.add(
            Triple(
                Pair(start, end),
                Pair(start_values, end_values),
                transition
            )
        )
    }
}

