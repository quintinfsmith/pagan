/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan.structure.opusmanager.base

import androidx.compose.ui.graphics.Color
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.ColorPalettable
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectControlSet
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.Effectable
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.effectcontroller.EffectController
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree

abstract class OpusLineAbstract<T: InstrumentEvent>(beats: MutableList<ReducibleTree<T>>): ReducibleTreeArray<T>(beats), ColorPalettable, Effectable {
    class BlockedCtlTreeException(var type: EffectType, var e: BlockedTreeException): Exception(e.message)
    override var palette = OpusColorPalette()
    var controllers = EffectControlSet(this.beats.size, setOf(EffectType.Volume))
    var muted = false

    init {
        // Default volume to hidden
        for ((_, controller) in this.controllers.get_all()) {
            controller.visible = false
        }
    }

    fun toggle_mute() {
        this.muted = !this.muted
    }

    fun mute() {
        this.muted = true
    }

    fun unmute() {
        this.muted = false
    }

    override fun insert_beat(index: Int) {
        super.insert_beat(index)
        this.controllers.insert_beat(index)
    }

    override fun set_beat_count(new_beat_count: Int) {
        super.set_beat_count(new_beat_count)
        this.controllers.set_beat_count(new_beat_count)
    }

    override fun remove_beat(index: Int, count: Int) {
        super.remove_beat(index, count)
        for ((type, controller) in this.controllers.get_all()) {
            try {
                controller.remove_beat(index, count)
            } catch (e: BlockedTreeException) {
                throw BlockedCtlTreeException(type, e)
            }
        }
    }

    fun remove_control_leaf(type: EffectType, beat: Int, position: List<Int>) {
        try {
            this.get_controller<EffectEvent>(type).remove_node(beat, position)
        } catch (e: BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun insert_control_leaf(type: EffectType, beat: Int, position: List<Int>) {
        try {
            this.get_controller<EffectEvent>(type).insert(beat, position)
        } catch (e: BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun insert_control_leaf_after(type: EffectType, beat: Int, position: List<Int>) {
        try {
            this.get_controller<EffectEvent>(type).insert_after(beat, position)
        } catch (e: BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun <U: EffectEvent> replace_control_leaf(type: EffectType, beat: Int, position: List<Int>, tree: ReducibleTree<U>) {
        try {
            this.get_controller<U>(type).replace_tree(beat, position, tree)
        } catch (e: BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    fun <U: EffectEvent> set_controller_event(type: EffectType, beat: Int, position: List<Int>, event: U) {
        try {
            this.get_controller<U>(type).set_event(beat, position, event)
        } catch (e: BlockedTreeException) {
            throw BlockedCtlTreeException(type, e)
        }
    }

    override fun <U: EffectEvent> get_controller(type: EffectType): EffectController<U> {
        return this.controllers.get<U>(type)
    }


    override fun equals(other: Any?): Boolean {
        if (other !is OpusLineAbstract<T>) return false
        if (this.controllers != other.controllers) return false
        return super.equals(other)
    }
}

