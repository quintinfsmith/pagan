package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree

abstract class OpusLineAbstract<T: InstrumentEvent>(var beats: MutableList<OpusTree<T>>) {
    var controllers = ActiveControlSet(this.beats.size, setOf(ControlEventType.Volume))

    fun squish(factor: Int) {
        val new_beats = mutableListOf<OpusTree<T>>()
        for (b in 0 until this.beats.size) {
            if (b % factor == 0) {
                new_beats.add(OpusTree<T>())
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
        if (other !is OpusLineAbstract<*>) {
            return false
        }

        for (i in 0 until this.beats.size) {
            if (this.beats[i] != other.beats[i]) {
                return false
            }
        }

        return true
    }

    fun insert_beat(index: Int) {
        this.beats.add(index, OpusTree())
        for ((_, controller) in this.controllers.get_all()) {
            controller.insert_beat(index)
        }
    }

    fun set_beat_count(new_beat_count: Int) {
        val original_size = this.beats.size
        if (new_beat_count > original_size) {
            for (i in original_size until new_beat_count) {
                this.beats.add(OpusTree())
            }
        } else if (new_beat_count < original_size) {
            for (i in new_beat_count until original_size) {
                this.beats.removeLast()
            }
        }
        this.controllers.set_beat_count(new_beat_count)
    }

    fun get_controller(type: ControlEventType): ActiveController {
        return this.controllers.get_controller(type)
    }

    fun remove_beat(index: Int) {
        this.beats.removeAt(index)
        for ((_, controller) in this.controllers.get_all()) {
            controller.remove_beat(index)
        }
    }

    fun get_tree(beat: Int, position: List<Int>? = null): OpusTree<T> {
        var tree = this.beats[beat]
        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun replace_tree(beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        val old_tree = this.get_tree(beat, position)
        if (old_tree == tree) {
            return // Don't waste the cycles
        }

        if (old_tree.parent != null) {
            old_tree.replace_with(tree)
        } else {
            tree.parent = null
        }

        if (position?.isEmpty() ?: true) {
            this.beats[beat] = tree
        }
    }
}

