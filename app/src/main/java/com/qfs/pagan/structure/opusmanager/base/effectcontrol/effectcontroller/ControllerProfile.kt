package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

class ControllerProfile(val initial_value: FloatArray) {
    class ProfileEffectEvent(
        val start_position: Float,
        val end_position: Float,
        val start_value: FloatArray,
        val end_value: FloatArray,
        val transition: EffectTransition
    ) {
        fun is_trivial(): Boolean {
            return this.start_value.contentEquals(this.end_value)
        }
    }

    val events = mutableListOf<ProfileEffectEvent>()

    fun add(event: ProfileEffectEvent) {
        this.events.add(event)
    }

    fun get_events(): List<ProfileEffectEvent> {
        return this.events.ifEmpty {
            listOf(
                ProfileEffectEvent(
                    start_position = 0F,
                    end_position = 0F,
                    start_value = FloatArray(this.initial_value.size),
                    end_value= this.initial_value,
                    EffectTransition.Instant
                )
            )
        }
    }


}

