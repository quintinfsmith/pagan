package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import kotlinx.serialization.Serializable

@Serializable
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int)

class OpusChannel(var uuid: Int) {
    class InvalidChannelUUID(uuid: Int): Exception("No such channel uuid: $uuid")
    class LineSizeMismatch(incoming_size: Int, required_size: Int): Exception("Line is $incoming_size beats but OpusManager is $required_size beats")

    class OpusLine(var beats: MutableList<OpusTree<OpusEvent>>) {
        constructor(beat_count: Int) : this(Array<OpusTree<OpusEvent>>(beat_count) { OpusTree() }.toMutableList())
        var volume = 96
        var static_value: Int? = null
        fun squish(factor: Int) {
            val new_beats = mutableListOf<OpusTree<OpusEvent>>()
            for (b in 0 until this.beats.size) {
                if (b % factor == 0) {
                    new_beats.add(OpusTree<OpusEvent>())
                }

                val working_beat = new_beats.last()
                working_beat.insert(b % factor, this.beats[b])
            }
            for (b in 0 until (this.beats.size % factor)) {
                this.beats.last().insert(b % factor, OpusTree())
            }
            this.beats = new_beats
        }
    }

    class LastLineException: Exception("Can't remove final line in channel")

    var lines: MutableList<OpusLine> = mutableListOf()
    var midi_bank = 0
    var midi_program = 0
    var midi_channel: Int = 0
    private var _beat_count: Int = 0
    var size: Int = 0

    fun map_line(line: Int, offset: Int) {
        this.lines[line].static_value = offset
    }

    fun get_mapped_line_offset(line: Int): Int? {
        return this.lines[line].static_value
    }

    fun new_line(index: Int? = null): OpusLine {
        val new_line = OpusLine(this._beat_count)
        if (index == null) {
            this.lines.add(new_line)
        } else if (index <= this.lines.size) {
            this.lines.add(index, new_line)
        } else {
            throw IndexOutOfBoundsException()
        }
        this.size += 1

        return new_line
    }

    fun insert_line(index: Int, line: OpusLine) {
        if (line.beats.size != this._beat_count) {
            throw LineSizeMismatch(line.beats.size, this._beat_count)
        }

        this.lines.add(index, line)
        this.size += 1
    }

    fun remove_line(index: Int? = null): OpusLine {
        if (this.lines.size == 1) {
            throw LastLineException()
        }
        return if (index == null) {
            this.size -= 1
            this.lines.removeLast()
        } else if (index < this.lines.size) {
            this.size -= 1
            lines.removeAt(index)
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    fun replace_tree(line: Int, beat: Int, position: List<Int>, tree: OpusTree<OpusEvent>) {
        val old_tree = this.get_tree(line, beat, position)
        if (old_tree.parent != null) {
            old_tree.replace_with(tree)
        } else {
            tree.parent = null
        }
        if (position.isEmpty()) {
            this.lines[line].beats[beat] = tree
        }
    }

    fun get_tree(line: Int, beat: Int, position: List<Int>? = null): OpusTree<OpusEvent> {
        var tree = this.lines[line].beats[beat]
        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun set_beat_count(new_beat_count: Int) {
        if (new_beat_count > this._beat_count) {
            for (line in this.lines) {
                while (line.beats.size < new_beat_count) {
                    line.beats.add(OpusTree())
                }
            }
        } else {
            for (line in this.lines) {
                while (line.beats.size > new_beat_count) {
                    line.beats.removeLast()
                }
            }
        }

        this._beat_count = new_beat_count
    }

    fun set_instrument(instrument: Pair<Int, Int>) {
        this.midi_bank = instrument.first
        this.midi_program = instrument.second
    }

    fun get_instrument(): Pair<Int, Int> {
        return Pair(this.midi_bank, this.midi_program)
    }

    // TODO: Implement
    //fun swap_lines(first_index: Int, second_index: Int) {
    //    // Adjust the new_index so it doesn't get confused
    //    // when we pop() the old_index

    //    if (first_index < 0) {
    //        throw IndexOutOfBoundsException()
    //    }
    //    if (second_index >= this.lines.size) {
    //        throw IndexOutOfBoundsException()
    //    }

    //    val tmp = this.lines[first_index]
    //    this.lines[first_index] = this.lines[second_index]
    //    this.lines[second_index] = tmp
    //}

    fun get_line(index: Int): OpusLine {
        return this.lines[index]
    }

    fun remove_beat(index: Int) {
        for (line in this.lines) {
            line.beats.removeAt(index)
        }
        this._beat_count -= 1
    }

    fun insert_beat(index: Int? = null) {
        if (index == null) {
            this.set_beat_count(this._beat_count + 1)
            return
        }

        this._beat_count += 1
        for (line in this.lines) {
            line.beats.add(index, OpusTree())
        }
    }

    fun line_is_empty(line_offset: Int): Boolean {
        for (tree in this.get_line(line_offset).beats) {
            if (!tree.is_leaf() || tree.is_event()) {
                return false
            }
        }
        return true
    }
    fun is_empty(): Boolean {
        for (i in 0 until this.lines.size) {
            if (!this.line_is_empty(i)) {
                return false
            }
        }

        return true
    }

    fun set_line_volume(line_offset: Int, volume: Int) {
        this.lines[line_offset].volume = volume
    }
    fun get_line_volume(line_offset: Int): Int {
        return this.lines[line_offset].volume
    }

    fun squish(factor: Int) {
        this.lines.forEachIndexed { i: Int, line: OpusLine ->
            line.squish(factor)
        }
    }
}
