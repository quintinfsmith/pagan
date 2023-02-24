package com.qfs.radixulous.opusmanager

import android.util.Log
import com.qfs.radixulous.structure.OpusTree
import kotlinx.serialization.Serializable
import java.lang.Integer.max
import java.lang.Integer.min

@Serializable
data class OpusEvent(var note: Int, var radix: Int, var channel: Int, var relative: Boolean)
@Serializable
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int)

class OpusChannel() {
    var lines: MutableList<MutableList<OpusTree<OpusEvent>>> = mutableListOf()
    var midi_instrument: Int = 1
    var midi_channel: Int = 0
    var beat_count: Int = 0
    var size: Int = 0
    private var line_map: HashMap<Int, Int>? = null

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
        var new_line: MutableList<OpusTree<OpusEvent>> = mutableListOf()
        for (i in 0 until this.beat_count) {
            new_line.add(OpusTree())
        }
        if (index == null) {
            this.lines.add(new_line)
        }  else {
            this.lines.add(index, new_line)
        }
        this.size += 1

        return new_line
    }

    fun insert_line(index: Int, line: MutableList<OpusTree<OpusEvent>>) {
        if (line.size != this.beat_count) {
            throw Exception("Line's beat count doesn't match Channel's")
        }

        this.lines.add(index, line)
        this.size += 1
    }

    fun remove_line(index: Int? = null): MutableList<OpusTree<OpusEvent>> {
        return if (index == null) {
            this.size -= 1
            lines.removeLast()
        } else if (index < lines.size) {
            this.size -= 1
            lines.removeAt(index)
        } else {
            throw Exception("Index Error $index / ${lines.size}")
        }
    }

    fun replace_tree(line: Int, beat: Int, position: List<Int>, tree: OpusTree<OpusEvent>) {
        var old_tree = this.get_tree(line, beat, position)
        old_tree.replace_with(tree)

        if (position.isEmpty()) {
            this.lines[line][beat] = tree
        }
    }

    fun get_tree(line: Int, beat: Int, position: List<Int>? = null): OpusTree<OpusEvent> {
        var tree = this.lines[line][beat]
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
                while (line.size < new_beat_count) {
                    line.add(OpusTree<OpusEvent>())
                }
            }
        } else {
            for (line in this.lines) {
                while (line.size > new_beat_count) {
                    line.removeLast()
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

    fun get_line(index: Int): MutableList<OpusTree<OpusEvent>> {
        return this.lines[index]
    }

    fun remove_beat(index: Int) {
        for (line in this.lines) {
            line.removeAt(index)
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
            line.add(index, OpusTree<OpusEvent>())
        }
    }

}