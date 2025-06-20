package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import kotlinx.serialization.Serializable

class InvalidBeatKey(channel: Int, line_offset: Int, beat: Int): Exception("Can't have negative values: BeatKey($channel, $line_offset, $beat)")

@Serializable
data class BeatKey(var channel: Int, var line_offset: Int, var beat: Int) {
    init {
        if (this.channel < 0 || this.line_offset < 0) {
            throw InvalidBeatKey(this.channel, this.line_offset, this.beat)
        }
    }

    fun toList(): List<Int> {
        return listOf(this.channel, this.line_offset, this.beat)
    }

    override fun toString(): String {
        return "BeatKey(${this.channel}, ${this.line_offset}, ${this.beat})"
    }
}

abstract class OpusChannelAbstract<U: InstrumentEvent, T: OpusLineAbstract<U>>(var uuid: Int) {
    class InvalidChannelUUID(uuid: Int): Exception("No such channel uuid: $uuid")
    class LineSizeMismatch(incoming_size: Int, required_size: Int): Exception("Line is $incoming_size beats but OpusManager is $required_size beats")
    class LastLineException: Exception("Can't remove final line in channel")
    class BlockedTreeException(var line_offset: Int, var e: OpusTreeArray.BlockedTreeException): Exception()
    class BlockedLineCtlTreeException(var line_offset: Int, var e: OpusLineAbstract.BlockedCtlTreeException): Exception()
    class BlockedCtlTreeException(var e: OpusLineAbstract.BlockedCtlTreeException): Exception()

    var lines: MutableList<T> = mutableListOf()
    var controllers = ActiveControlSet(0)
    var midi_program = 0
    private var _beat_count: Int = 0
    var size: Int = 0

    var visible = true // Would rather have this in the Interface Layer but the code is much cleaner with it here
    var muted = false

    abstract fun gen_line(): T
    open fun clear() {
        this.lines.clear()
        this.midi_program = 0
        this._beat_count = 0
        this.size = 0
        this.controllers.clear()
    }

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
            this.lines.removeAt(this.lines.size - 1)
        } else if (index < this.lines.size) {
            this.size -= 1
            lines.removeAt(index)
        } else {
            throw IndexOutOfBoundsException()
        }
    }

    fun replace_tree(line_offset: Int, beat: Int, position: List<Int>?, tree: OpusTree<U>) {
        this.catch_blocked_tree_exception(line_offset) {
            this.lines[line_offset].replace_tree(beat, position, tree)
        }
    }

    fun get_tree(line: Int, beat: Int, position: List<Int>? = null): OpusTree<U> {
        return this.lines[line].get_tree(beat, position)
    }

    fun <T: OpusControlEvent> get_ctl_tree(line: Int, type: ControlEventType, beat: Int, position: List<Int>? = null): OpusTree<T> {
        var tree = this.lines[line].controllers.get_controller<T>(type).get_tree(beat)

        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun <T: OpusControlEvent> get_ctl_tree(type: ControlEventType, beat: Int, position: List<Int>? = null): OpusTree<T> {
        var tree = this.controllers.get_controller<T>(type).get_tree(beat)

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

    fun set_midi_program(program: Int) {
        this.midi_program = program
    }

    fun get_instrument(): Pair<Int, Int> {
        return Pair(this.get_midi_bank(), this.midi_program)
    }

    fun get_line(index: Int): T {
        return this.lines[index]
    }

    fun blocked_check_remove_beat(index: Int, count: Int = 1) {
        for (line_offset in 0 until this.lines.size) {
            this.catch_blocked_tree_exception(line_offset) {
                this.lines[line_offset].blocked_check_remove_beat_throw(index, count)
            }
        }


        for ((type, controller) in this.controllers.get_all()) {
            this.catch_blocked_tree_exception_channel_controller(type) {
                controller.blocked_check_remove_beat_throw(index, count)
            }
        }
    }

    fun remove_beat(index: Int, count: Int = 1) {
        //this.blocked_check_remove_beat(index, count)

        for (line_offset in 0 until this.lines.size) {
            this.lines[line_offset].remove_beat(index, count)
        }

        for ((type, controller) in this.controllers.get_all()) {
            controller.remove_beat(index, count)
        }

        this._beat_count -= 1
    }

    /* Catch Blocked tree exceptions and upgrade them to exceptions with more context */
    fun <T> catch_blocked_tree_exception_channel_controller(type: ControlEventType, callback: () -> T): T {
        return try {
            callback()
        } catch (e: OpusTreeArray.BlockedTreeException) {
            throw OpusChannelAbstract.BlockedCtlTreeException(OpusLineAbstract.BlockedCtlTreeException(type, e))
        }
    }
    /* Catch Blocked tree exceptions and upgrade them to exceptions with more context */
    fun <T> catch_blocked_tree_exception(line_offset: Int, callback: () -> T): T {
        return try {
            callback()
        } catch (e: OpusTreeArray.BlockedTreeException) {
            throw OpusChannelAbstract.BlockedTreeException(line_offset, e)
        } catch (e: OpusLineAbstract.BlockedCtlTreeException) {
            throw OpusChannelAbstract.BlockedLineCtlTreeException(line_offset, e)
        }
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

    fun set_line_volume(line_offset: Int, volume: Float) {
        this.lines[line_offset].controllers.get_controller<OpusVolumeEvent>(ControlEventType.Volume).initial_event = OpusVolumeEvent(volume)
    }

    fun squish(factor: Int) {
        for (line in this.lines) {
            line.squish(factor)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is OpusChannelAbstract<*, *>) {
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
    abstract fun get_midi_channel(): Int
    abstract fun get_midi_bank(): Int
    fun get_midi_program(): Int {
        return this.midi_program
    }

    fun set_event(line_offset: Int, index: Int, position: List<Int>, event: U) {
        this.catch_blocked_tree_exception(line_offset) {
            this.lines[line_offset].set_event(index, position, event)
        }
    }

    fun <K: OpusControlEvent> set_line_controller_event(type: ControlEventType, line_offset: Int, index: Int, position: List<Int>, event: K) {
        this.catch_blocked_tree_exception(line_offset) {
            this.lines[line_offset].set_controller_event(type, index, position, event)
        }
    }

    fun <K: OpusControlEvent> set_controller_event(type: ControlEventType, index: Int, position: List<Int>, event: K) {
        this.catch_blocked_tree_exception_channel_controller(type) {
            this.controllers.get_controller<K>(type).set_event(index, position, event)
        }
    }

    fun remove(line_offset: Int, beat: Int, position: List<Int>) {
        this.catch_blocked_tree_exception(line_offset) {
            this.lines[line_offset].remove_node(beat, position)
        }
    }
    fun controller_line_remove_leaf(type: ControlEventType, line_offset: Int, beat: Int, position: List<Int>) {
        this.catch_blocked_tree_exception(line_offset) {
            this.get_line(line_offset).remove_control_leaf(type, beat, position)
        }
    }
    fun controller_channel_remove_leaf(type: ControlEventType, beat: Int, position: List<Int>) {
        this.catch_blocked_tree_exception_channel_controller(type) {
            this.controllers.get_controller<OpusControlEvent>(type).remove_node(beat, position)
        }
    }

    fun controller_channel_insert_leaf(type: ControlEventType, beat: Int, position: List<Int>){
        this.catch_blocked_tree_exception_channel_controller(type) {
            this.controllers.get_controller<OpusControlEvent>(type).insert(beat, position)
        }
    }

    fun controller_channel_insert_leaf_after(type: ControlEventType, beat: Int, position: List<Int>){
        this.catch_blocked_tree_exception_channel_controller(type) {
            this.controllers.get_controller<OpusControlEvent>(type).insert_after(beat, position)
        }
    }

    fun controller_line_insert_leaf(type: ControlEventType, line_offset: Int, beat: Int, position: List<Int>){
        this.catch_blocked_tree_exception(line_offset) {
            this.get_line(line_offset).insert_control_leaf(type, beat, position)
        }
    }

    fun controller_line_insert_leaf_after(type: ControlEventType, line_offset: Int, beat: Int, position: List<Int>){
        this.catch_blocked_tree_exception(line_offset) {
            this.get_line(line_offset).insert_control_leaf_after(type, beat, position)
        }
    }

    fun <K: OpusControlEvent> replace_line_control_leaf(type: ControlEventType, line_offset: Int, beat: Int, position: List<Int>, tree: OpusTree<K>) {
        this.catch_blocked_tree_exception(line_offset) {
            this.lines[line_offset].replace_control_leaf(type, beat, position, tree)
        }
    }
    fun <K: OpusControlEvent> replace_channel_control_leaf(type: ControlEventType, beat: Int, position: List<Int>, tree: OpusTree<K>) {
        this.catch_blocked_tree_exception_channel_controller(type) {
            this.controllers.get_controller<K>(type).replace_tree(beat, position, tree)
        }
    }

    fun insert_tree(line_offset: Int, beat: Int, position: List<Int>) {
        this.catch_blocked_tree_exception(line_offset) {
            this.lines[line_offset].insert(beat, position)
        }
    }

    fun insert_after(line_offset: Int, beat: Int, position: List<Int>) {
        this.catch_blocked_tree_exception(line_offset) {
            this.lines[line_offset].insert_after(beat, position)
        }
    }

    fun set_line_color(line_offset: Int, color: Int?) {
        this.lines[line_offset].color = color
    }

    override fun hashCode(): Int {
        var result = this.lines.hashCode()
        result = (31 * result) + this.controllers.hashCode()
        result = (31 * result) + this.midi_program
        result = (31 * result) + this._beat_count
        result = (31 * result) + this.size
        result = (31 * result) + this.visible.hashCode()
        return result
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
}

class OpusChannel(uuid: Int): OpusChannelAbstract<TunedInstrumentEvent, OpusLine>(uuid) {
    var midi_channel: Int = 0
    var midi_bank = 0

    fun set_midi_bank(bank: Int) {
        this.midi_bank = bank
    }
    override fun gen_line(): OpusLine {
        return OpusLine(this.get_beat_count())
    }
    override fun get_midi_channel(): Int {
        return this.midi_channel
    }
    override fun get_midi_bank(): Int {
        return this.midi_bank
    }
    override fun clear() {
        super.clear()
        this.midi_channel = 0
        this.midi_bank = 0
    }



}

class OpusPercussionChannel(uuid: Int) : OpusChannelAbstract<PercussionEvent, OpusLinePercussion>(uuid) {
    companion object {
        const val DEFAULT_INSTRUMENT = 0
    }


    override fun gen_line(): OpusLinePercussion {
        return OpusLinePercussion(OpusPercussionChannel.DEFAULT_INSTRUMENT, this.get_beat_count())
    }

    fun set_instrument(line: Int, offset: Int) {
        this.lines[line].instrument = offset
    }

    fun get_instrument(line: Int): Int {
        return this.lines[line].instrument
    }

    override fun get_midi_channel(): Int {
        return 9
    }
    override fun get_midi_bank(): Int {
        return 128
    }
}
