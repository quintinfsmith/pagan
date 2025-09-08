package com.qfs.pagan.structure.opusmanager.base.effectcontrol

import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.DelayController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.PanController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.ReverbController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.TempoController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VelocityController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.VolumeController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent

class EffectControlSet(var beat_count: Int, default_enabled: Set<EffectType>? = null) {
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

    fun new_controller(type: EffectType, controller: EffectController<*>? = null) {
        if (controller == null) {
            this.controllers[type] = when (type) {
                EffectType.Tempo -> TempoController(this.beat_count)
                EffectType.Volume -> VolumeController(this.beat_count)
                EffectType.Reverb -> ReverbController(this.beat_count)
                EffectType.Pan -> PanController(this.beat_count)
                EffectType.Velocity -> VelocityController(this.beat_count)
                EffectType.Delay -> DelayController(this.beat_count)
            }
        } else {
            this.controllers[type] = controller
        }
        this.controllers[type]!!.set_beat_count(this.beat_count)
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

    operator fun <T: EffectEvent> get(type: EffectType): EffectController<T> {
        if (!this.controllers.containsKey(type)) {
            this.new_controller(type)
        }
        return this.controllers[type]!! as EffectController<T>
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
}