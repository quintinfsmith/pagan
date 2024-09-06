package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree

abstract class ActiveController(beat_count: Int) {
    var events = mutableListOf<OpusTree<OpusControlEvent>?>()
    abstract var initial_event: OpusControlEvent

    init {
        for (i in 0 until beat_count) {
            this.insert_beat(i)
        }
    }

    fun get_proceding_leaf_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        var working_position = position.toMutableList()
        var working_beat = beat

        var working_tree = this.get_tree(working_beat, working_position)

        // Move right/up
        while (true) {
            if (working_tree.parent != null) {
                if (working_tree.parent!!.size - 1 > working_position.last()) {
                    working_position[working_position.size - 1] += 1
                    working_tree = this.get_tree(working_beat, working_position)
                    break
                } else {
                    working_position.removeLast()
                    working_tree = working_tree.parent!!
                }
            } else if (working_beat < this.events.size - 1) {
                working_beat += 1
                working_position = mutableListOf()
                working_tree = this.get_tree(working_beat, working_position)
                break
            } else {
                return null
            }
        }
        // Move left/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(0)
            working_tree = working_tree[0]
        }
        return Pair(working_beat, working_position)
    }

    fun set_initial_event(value: OpusControlEvent) {
        this.initial_event = value
    }

    fun get_latest_event(beat: Int, position: List<Int>): OpusControlEvent {
        val current_tree = this.get_tree(beat, position)
        if (current_tree.is_event()) {
            return current_tree.get_event()!!
        }

        var working_beat = beat
        var working_position = position.toList()
        var output = this.initial_event

        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat, working_position) ?: return output
            working_beat = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat, working_position)
            if (working_tree.is_event()) {
                val working_event = working_tree.get_event()!!
                output = working_event
                break
            }
        }
        return output
    }

    fun get_preceding_leaf_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        val working_position = position.toMutableList()
        var working_beat = beat

        // Move left/up
        while (true) {
            if (working_position.isNotEmpty()) {
                if (working_position.last() > 0) {
                    working_position[working_position.size - 1] -= 1
                    break
                } else {
                    working_position.removeLast()
                }
            } else if (working_beat > 0) {
                working_beat -= 1
                break
            } else {
                return null
            }
        }

        var working_tree = this.get_tree(working_beat, working_position)

        // Move right/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(working_tree.size - 1)
            working_tree = working_tree[working_tree.size - 1]
        }

        return Pair(working_beat, working_position)
    }

    fun beat_count(): Int {
        return this.events.size
    }

    fun insert_beat(n: Int) {
        this.events.add(n, null)
    }

    fun remove_beat(n: Int) {
        this.events.removeAt(n)
    }

    fun get_tree(beat: Int, position: List<Int>? = null): OpusTree<OpusControlEvent> {
        var tree = this.get_beat(beat)
        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun get_beat(beat: Int): OpusTree<OpusControlEvent> {
        if (this.events[beat] == null) {
            this.events[beat] = OpusTree()
        }

        return this.events[beat]!!
    }

    fun replace_tree(beat: Int, position: List<Int>, new_tree: OpusTree<OpusControlEvent>) {
        if (position.isEmpty()) {
            this.events[beat] = new_tree
        } else {
            var tree = this.get_beat(beat)
            for (i in position) {
                tree = tree[i]
            }
            tree.replace_with(new_tree)
        }
    }

    fun set_beat_count(beat_count: Int) {
        val current_beat_count = this.events.size
        if (beat_count > current_beat_count) {
            for (i in current_beat_count until beat_count) {
                this.insert_beat(current_beat_count)
            }
        } else if (beat_count < current_beat_count) {
            for (i in beat_count until current_beat_count) {
                this.remove_beat(this.beat_count() - 1)
            }
        }
    }
}

class VolumeController(beat_count: Int): ActiveController(beat_count) {
    override var initial_event: OpusControlEvent = OpusVolumeEvent(64)
}
class TempoController(beat_count: Int): ActiveController(beat_count) {
    override var initial_event: OpusControlEvent = OpusTempoEvent(120F)
}
class ReverbController(beat_count: Int): ActiveController(beat_count) {
    override var initial_event: OpusControlEvent = OpusReverbEvent(1F)
}
