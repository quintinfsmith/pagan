package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import kotlinx.serialization.Serializable

class InvalidBeatKey(channel: Int, line_offset: Int, beat: Int): Exception("Can't have negative values: BeatKey($channel, $line_offset, $beat)")

@Serializable
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int) {
    init {
        if (channel < 0 || line_offset < 0) {
            throw InvalidBeatKey(channel, line_offset, beat)
        }
    }
}

abstract class OpusChannelAbstract<U: InstrumentEvent, T: OpusLineAbstract<U>>() {
    class LineSizeMismatch(incoming_size: Int, required_size: Int): Exception("Line is $incoming_size beats but OpusManager is $required_size beats")
    class LastLineException: Exception("Can't remove final line in channel")

    var lines: MutableList<T> = mutableListOf()
    var controllers = ActiveControlSet(0)
    var midi_bank = 0
    var midi_program = 0
    var midi_channel: Int = 0
    private var _beat_count: Int = 0
    var size: Int = 0
    init {
        this.controllers.new_controller(ControlEventType.Volume)
    }

    abstract fun gen_line(): T

    fun get_beat_count(): Int {
        return this._beat_count
    }

    fun new_line(index: Int? = null): T {
        val new_line = this.gen_line()
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

    fun insert_line(index: Int, line: T) {
        if (line.beats.size != this._beat_count) {
            throw LineSizeMismatch(line.beats.size, this._beat_count)
        }

        this.lines.add(index, line)
        this.size += 1
    }

    fun remove_line(index: Int? = null): T {
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

    fun replace_tree(line: Int, beat: Int, position: List<Int>?, tree: OpusTree<U>) {
        val old_tree = this.get_tree(line, beat, position)
        if (old_tree == tree) {
            return // Don't waste the cycles
        }

        if (old_tree.parent != null) {
            old_tree.replace_with(tree)
        } else {
            tree.parent = null
        }

        if (position?.isEmpty() ?: true) {
            this.lines[line].beats[beat] = tree
        }
    }

    fun get_tree(line: Int, beat: Int, position: List<Int>? = null): OpusTree<U> {
        var tree = this.lines[line].beats[beat]
        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun get_ctl_tree(line: Int, type: ControlEventType, beat: Int, position: List<Int>? = null): OpusTree<OpusControlEvent> {
        var tree = this.lines[line].controllers.get_controller(type).get_beat(beat)

        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun get_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>? = null): OpusTree<OpusControlEvent> {
        var tree = this.controllers.get_controller(type).get_beat(beat)

        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun set_beat_count(new_beat_count: Int) {
        for (line in this.lines) {
            line.set_beat_count(new_beat_count)
        }
        this.controllers.set_beat_count(new_beat_count)
        this._beat_count = new_beat_count
    }

    fun set_instrument(instrument: Pair<Int, Int>) {
        this.midi_bank = instrument.first
        this.midi_program = instrument.second
    }

    fun get_instrument(): Pair<Int, Int> {
        return Pair(this.midi_bank, this.midi_program)
    }

    fun get_line(index: Int): T {
        return this.lines[index]
    }

    fun remove_beat(index: Int) {
        for (line in this.lines) {
            line.remove_beat(index)
        }

        for ((type, controller) in this.controllers.get_all()) {
            controller.remove_beat(index)
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
            line.insert_beat(index)
        }
        this.controllers.insert_beat(index)
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
        this.lines[line_offset].controllers.get_controller(ControlEventType.Volume).initial_event = OpusVolumeEvent(volume)
    }

    fun squish(factor: Int) {
        for (line in this.lines) {
            line.squish(factor)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is OpusChannel) {
            return false
        }

        if (this.lines.size != other.lines.size) {
            return false
        }

        for (i in 0 until this.lines.size) {
            if (this.lines[i] != other.lines[i]) {
                return false
            }
        }

        return true
    }
}

class OpusChannel(var uuid: Int): OpusChannelAbstract<TunedInstrumentEvent, OpusLine>() {
    class InvalidChannelUUID(uuid: Int): Exception("No such channel uuid: $uuid")
    override fun gen_line(): OpusLine {
        return OpusLine(this.get_beat_count())
    }
}

class OpusPercussionChannel(): OpusChannelAbstract<PercussionEvent, OpusLinePercussion>() {
    companion object {
        val default_instrument = 0
    }
    override fun gen_line(): OpusLinePercussion {
        return OpusLinePercussion(OpusPercussionChannel.default_instrument, this.get_beat_count())
    }

    fun set_instrument(line: Int, offset: Int) {
        this.lines[line].instrument = offset
    }

    fun get_instrument(line: Int): Int {
        return this.lines[line].instrument
    }

}
