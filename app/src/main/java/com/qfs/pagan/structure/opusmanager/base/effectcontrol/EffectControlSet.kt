/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.base.effectcontrol

import com.qfs.json.JSONHashMap
import com.qfs.json.JSONList
import com.qfs.json.JSONCompliant
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.LowPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.HighPassEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.PitchEvent

class EffectControlSet(var beat_count: Int, default_enabled: Set<EffectType>? = null): JSONCompliant {
    val controllers = HashMap<EffectType, EffectController<*>>()

    init {
        for (type in default_enabled ?: setOf()) {
            this.new_controller(type)
        }
    }

    fun clear() {
        this.beat_count = 0
        this.controllers.clear()
    }

    fun size(): Int {
        return this.controllers.size
    }

    fun get_all(): Array<Pair<EffectType, EffectController<out EffectEvent>>> {
        val items = this.controllers.entries.sortedByDescending { it.key }
        return Array(items.size) { i: Int ->
            Pair(items[i].key, items[i].value)
        }
    }

    fun new_controller(type: EffectType) {
        this.controllers[type] = EffectController(
            this.beat_count,
            when (type) {
                EffectType.Tempo -> OpusTempoEvent()
                EffectType.Volume -> OpusVolumeEvent()
                EffectType.Reverb -> OpusReverbEvent()
                EffectType.Pan -> OpusPanEvent()
                EffectType.Velocity -> OpusVelocityEvent()
                EffectType.Delay -> DelayEvent()
                EffectType.LowPass -> LowPassEvent()
                EffectType.HighPass -> HighPassEvent()
                EffectType.Pitch -> PitchEvent()
            }
        )
    }

    fun <T: EffectEvent> new_controller(controller: EffectController<T>) {
        controller.set_beat_count(this.beat_count)
        this.controllers[controller.initial_event.event_type] = controller
    }

    fun remove_controller(type: EffectType) {
        this.controllers.remove(type)
    }

    fun insert_beat(n: Int) {
        this.beat_count += 1
        for (controller in this.controllers.values) {
            controller.insert_beat(n)
        }
    }

    fun remove_beat(n: Int) {
        this.beat_count -= 1
        for (controller in this.controllers.values) {
            controller.remove_beat(n)
        }
    }

    operator fun get(type: EffectType): EffectController<*> {
        if (!this.controllers.containsKey(type)) {
            this.new_controller(type)
        }
        return this.controllers[type]!!
    }

    fun has_controller(type: EffectType): Boolean {
        return this.controllers.containsKey(type)
    }

    fun set_beat_count(new_count: Int) {
        this.beat_count = new_count
        for ((_, controller) in this.get_all()) {
            controller.set_beat_count(new_count)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is EffectControlSet) return false
        if (other.controllers.keys != this.controllers.keys) return false
        for ((key, controller) in this.controllers.entries) {
            if (other.controllers[key] != controller) {
                return false
            }
        }
        return true
    }

    override fun to_json(): JSONHashMap {
        val output = JSONHashMap()
        output["beat_count"] = this.beat_count

        val controllers = this.controllers.values.toList()
        output["controllers"] = JSONList(controllers.size) {
            controllers[it].to_json()
        }

        return output
    }
}
