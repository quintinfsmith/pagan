package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import kotlinx.serialization.Serializable

class OpusLine(var beats: MutableList<OpusTree<OpusEventSTD>>) {
    constructor(beat_count: Int) : this(Array<OpusTree<OpusEventSTD>>(beat_count) { OpusTree() }.toMutableList())
    var volume = 64
    var static_value: Int? = null
    var controllers = ActiveControlSet(this.beats.size)

    fun squish(factor: Int) {
        val new_beats = mutableListOf<OpusTree<OpusEventSTD>>()
        for (b in 0 until this.beats.size) {
            if (b % factor == 0) {
                new_beats.add(OpusTree<OpusEventSTD>())
            }
            val working_beat = new_beats.last()
            working_beat.insert(b % factor, this.beats[b])
        }

        if (this.beats.size % factor != 0) {
            while (new_beats.last().size < factor) {
                new_beats.last().insert(
                    new_beats.last().size,
                    OpusTree()
                )
            }
        }

        for (beat in new_beats) {
            var is_empty = true

            for (i in 0 until beat.size) {
                if (!(beat[i].is_leaf() && beat[i].is_eventless())) {
                    is_empty = false
                    break
                }
            }

            if (is_empty) {
                beat.set_size(0)
            }
        }
        this.beats = new_beats
    }

    override fun equals(other: Any?): Boolean {
        if (other !is OpusLine) {
            return false
        }

        if (this.volume != other.volume) {
            return false
        }

        if (this.static_value != other.static_value) {
            return false
        }

        for (i in 0 until this.beats.size) {
            if (this.beats[i] != other.beats[i]) {
                return false
            }
        }

        return true
    }

    fun set_beat_count(new_beat_count: Int) {
        val original_size = this.beats.size
        if (new_beat_count > original_size) {
            for (i in original_size until new_beat_count) {
                this.beats.add(OpusTree())
                for (controller in this.controllers.controllers.values) {
                    controller.insert_beat(i)
                }
            }
        } else {
            for (i in new_beat_count until original_size) {
                this.beats.removeLast()
                for (controller in this.controllers.controllers.values) {
                    controller.remove_beat(new_beat_count)
                }
            }
        }
    }

    fun get_controller(type: ControlEventType): ActiveControlSet.ActiveController {
        return this.controllers.get_controller(type)
    }
}
