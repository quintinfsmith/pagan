package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

class ControllerProfile(val initial_value: FloatArray) {
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
    fun get_values(): List<Triple<Pair<Float, Float>, Pair<FloatArray, FloatArray>, EffectTransition>> {
        return this.values.ifEmpty {
            listOf(Triple(Pair(0F, 0F), Pair(FloatArray(this.initial_value.size), this.initial_value), EffectTransition.Instant))
        }
    }
}

