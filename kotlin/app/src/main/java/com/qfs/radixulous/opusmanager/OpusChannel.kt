package com.qfs.radixulous.opusmanager

import com.qfs.radixulous.structure.OpusTree
import kotlinx.serialization.Serializable

@Serializable
data class OpusEvent(var note: Int, var radix: Int, var channel: Int, var relative: Boolean)
@Serializable
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int)

class OpusChannel(var uuid: Int) {
    class OpusLine(var beats: MutableList<OpusTree<OpusEvent>>) {
        constructor(beat_count: Int) : this(Array<OpusTree<OpusEvent>>(beat_count) { _ -> OpusTree() }.toMutableList())
        var volume = 64
    }

    var lines: MutableList<OpusLine> = mutableListOf()
    var midi_instrument: Int = 0
    var midi_channel: Int = 0
    var beat_count: Int = 0
    var size: Int = 0
    internal var line_map: HashMap<Int, Int>? = null

    fun is_mapped(): Boolean {
        return this.line_map != null
    }

    fun map_line(line: Int, offset: Int) {
        if (this.line_map == null) {
            this.set_mapped()
        }
        this.line_map!![line] = offset
    }

    fun unmap() {
        this.line_map = null
    }

    fun set_mapped() {
        this.line_map = HashMap<Int, Int>()
    }

    fun get_mapped_line_offset(line: Int): Int? {
        if (this.line_map == null) {
            return null
        }
        return this.line_map!![line]
    }

    fun new_line(index: Int? = null): List<OpusTree<OpusEvent>> {
        var new_line = OpusLine(this.beat_count)
        if (index == null) {
            this.lines.add(new_line)
        }  else {
            this.lines.add(index, new_line)
        }
        this.size += 1

        return new_line.beats
    }

    fun insert_line(index: Int, line: MutableList<OpusTree<OpusEvent>>) {
        if (line.size != this.beat_count) {
            throw Exception("Line's beat count doesn't match Channel's")
        }
        var new_line = OpusLine(line)
        this.lines.add(index, new_line)
        this.size += 1
    }

    fun remove_line(index: Int? = null): MutableList<OpusTree<OpusEvent>> {

        return if (index == null) {
            this.size -= 1
            lines.removeLast().beats
        } else if (index < lines.size) {
            if (this.line_map != null) {
                for (i in index until this.size - 1) {
                    var next = this.line_map!![i + 1]
                    if (next != null) {
                        this.line_map!![i] = next
                    } else {
                        this.line_map!!.remove(i)
                    }
                }
            }
            this.size -= 1
            lines.removeAt(index).beats
        } else {
            throw Exception("Index Error $index / ${lines.size}")
        }

    }

    fun replace_tree(line: Int, beat: Int, position: List<Int>, tree: OpusTree<OpusEvent>) {
        var old_tree = this.get_tree(line, beat, position)
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
                tree = tree.get(i)
            }
        }

        return tree
    }

    fun set_beat_count(new_beat_count: Int) {
        if (new_beat_count > this.beat_count) {
            for (line in this.lines) {
                while (line.beats.size < new_beat_count) {
                    line.beats.add(OpusTree<OpusEvent>())
                }
            }
        } else {
            for (line in this.lines) {
                while (line.beats.size > new_beat_count) {
                    line.beats.removeLast()
                }
            }
        }

        this.beat_count = new_beat_count
    }

    fun set_instrument(instrument: Int) {
        this.midi_instrument = instrument
    }

    fun get_instrument(): Int {
        return this.midi_instrument
    }

    fun swap_lines(first_index: Int, second_index: Int) {
        // Adjust the new_index so it doesn't get confused
        // when we pop() the old_index

        if (first_index < 0) {
            throw Exception("INDEXERROR")
        }
        if (second_index >= this.lines.size) {
            throw Exception("INDEXERROR")
        }

        var tmp = this.lines[first_index]
        this.lines[first_index] = this.lines[second_index]
        this.lines[second_index] = tmp
    }

    fun get_line(index: Int): OpusLine {
        return this.lines[index]
    }

    fun remove_beat(index: Int) {
        for (line in this.lines) {
            line.beats.removeAt(index)
        }
        this.beat_count -= 1
    }

    fun insert_beat(index: Int? = null) {
        if (index == null) {
            this.set_beat_count(this.beat_count + 1)
            return
        }

        this.beat_count += 1
        for (line in this.lines) {
            line.beats.add(index, OpusTree<OpusEvent>())
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

    fun set_line_volume(line_offset: Int, volume: Int) {
        this.lines[line_offset].volume = volume
    }
    fun get_line_volume(line_offset: Int): Int {
        return this.lines[line_offset].volume
    }

}