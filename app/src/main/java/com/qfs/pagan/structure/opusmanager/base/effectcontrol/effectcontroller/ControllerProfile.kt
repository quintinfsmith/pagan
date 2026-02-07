/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller

import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition

class ControllerProfile(val initial_value: FloatArray) {
    class ProfileEffectEvent(
        val start_position: Rational,
        val end_position: Rational,
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
                    start_position = Rational(0,1),
                    end_position = Rational(0,1),
                    start_value = FloatArray(this.initial_value.size),
                    end_value= this.initial_value,
                    EffectTransition.Instant
                )
            )
        }
    }


}

