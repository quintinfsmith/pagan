package com.qfs.radixulous.opusmanager

import com.qfs.radixulous.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

data class OpusEvent(var note: Int, var radix: Int, var channel: Int, var relative: Boolean)
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int)

class OpusChannel() {
    private var lines: MutableList<MutableList<OpusTree<OpusEvent>>> = mutableListOf()
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

    fun new_line(index: Int? = null) {
        if (index == null) {
            this.lines.add(mutableListOf())
            while (this.lines.last().size < this.beat_count) {
                this.lines.last().add(OpusTree<OpusEvent>())
            }
        }  else {
            this.lines.add(index, mutableListOf())
            while (this.lines[index].size < this.beat_count) {
                this.lines[index].add(OpusTree<OpusEvent>())
            }
        }
        this.size += 1
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
            lines.removeLast()
        } else {
            lines.removeAt(index)
        }

        this.size -= 1
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

    fun move_line(old_index: Int, new_index: Int) {
        // Adjust the new_index so it doesn't get confused
        // when we pop() the old_index
        var adj_new_index: Int = if (new_index < 0) {
            this.lines.size + new_index
        } else {
            new_index
        }

        if (adj_new_index < 0) {
            throw Exception("INDEXERROR")
        }
        if (old_index >= this.lines.size) {
            throw Exception("INDEXERROR")
        }

        var line = this.lines.removeAt(old_index)
        this.lines.add(adj_new_index, line)

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

}