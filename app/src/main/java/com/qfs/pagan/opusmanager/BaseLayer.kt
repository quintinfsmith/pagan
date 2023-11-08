package com.qfs.pagan.opusmanager
import com.qfs.apres.Midi
import com.qfs.apres.event.BankSelect
import com.qfs.apres.event.NoteOff
import com.qfs.apres.event.NoteOn
import com.qfs.apres.event.ProgramChange
import com.qfs.apres.event.SetTempo
import com.qfs.apres.event.SongPositionPointer
import com.qfs.apres.event.TimeSignature
import com.qfs.apres.event2.NoteOff79
import com.qfs.apres.event2.NoteOn79
import com.qfs.pagan.from_string
import com.qfs.pagan.structure.OpusTree
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

/**
 * The logic of the Opus Manager.
 *
 * This is completely separated from user interface or state.
 * @constructor Creates an unusably empty object. new() / load() / import() need to be called still
 */
open class BaseLayer {
    class BadBeatKey(beat_key: BeatKey): Exception("BeatKey $beat_key doesn't exist")
    class NonEventConversion(beat_key: BeatKey, position: List<Int>): Exception("Attempting to convert non-event @ $beat_key:$position")
    class NoteOutOfRange(n: Int): Exception("Attempting to use unsupported note $n")
    class NonPercussionEventSet : Exception("Attempting to set normal event on percussion channel")
    class PercussionEventSet : Exception("Attempting to set percussion event on non-percussion channel")
    class EmptyPath : Exception("Path Required but not given")
    class BadInsertPosition : Exception("Can't insert tree at top level")
    class RemovingLastBeatException : Exception("OpusManager requires at least 1 beat")
    class IncompatibleChannelException(channel_old: Int, channel_new: Int) : Exception("Can't move lines into or out of the percussion channel ($channel_old -> $channel_new)")

    companion object {
        var DEFAULT_PERCUSSION: Int = 0
    }

    private var _channel_uuid_generator: Int = 0x00
    private var _channel_uuid_map = HashMap<Int, OpusChannel>()

    var beat_count: Int = 1
    var channels: MutableList<OpusChannel> = mutableListOf()
    var path: String? = null
    var project_name: String = "New Opus"
    var radix: Int = 12
    var tempo: Float = 120F
    var transpose: Int = 0

    //// RO Functions ////
    /**
     * Calculates the number of channels in use.
     */
    fun get_channel_count(): Int {
        return this.channels.size
    }

    /**
     * Calculates the position of the first leaf in a given tree
     */
    fun get_first_position(beat_key: BeatKey, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_tree(beat_key, output)
        while (! tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    /**
     * Calculates how many lines are in use.
     */
    fun get_total_line_count(): Int {
        var output = 0
        this.channels.forEach { channel: OpusChannel ->
            output += channel.size
        }
        return output
    }

    /**
     * Calculates how many lines down a given row is.
     */
    fun get_abs_offset(channel_index: Int, line_offset: Int): Int {
        // Allows for line_offsets longer than line counts. ie channel=1, line_offset=2 could technically be channel=2
        var count = 0
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            for (j in 0 until channel.size) {
                if (i > channel_index || (i == channel_index && j == line_offset)) {
                    return count
                }
                count += 1
            }
        }
        throw IndexOutOfBoundsException()
    }

    /**
     * Finds all the BeatKeys in a range denoted by [top_left_key] & [bottom_right_key].
     */
    fun get_beatkeys_in_range(top_left_key: BeatKey, bottom_right_key: BeatKey): List<BeatKey> {
        val output = mutableListOf<BeatKey>()
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (i < top_left_key.channel || i > bottom_right_key.channel) {
                return@forEachIndexed // Continues
            }

            val (start, end) = if (top_left_key.channel == bottom_right_key.channel) {
                Pair(top_left_key.line_offset, bottom_right_key.line_offset)
            } else {
                when (i) {
                    top_left_key.channel -> {
                        Pair(top_left_key.line_offset, channel.size - 1)
                    }
                    bottom_right_key.channel -> {
                        Pair(0, bottom_right_key.line_offset)
                    }
                    else -> {
                        Pair(0, channel.size - 1)
                    }
                }
            }
            for (j in start .. end) {
                for (k in top_left_key.beat .. bottom_right_key.beat) {
                    output.add(BeatKey(i,j,k))
                }
            }
        }
        return output
    }

    /**
     * Calculate the x & y difference between two BeatKeys [beata] & [beatb]
     */
    open fun get_abs_difference(beata: BeatKey, beatb: BeatKey): Pair<Int, Int> {
        val beata_y = this.get_abs_offset(beata.channel, beata.line_offset)
        val beatb_y = this.get_abs_offset(beatb.channel, beatb.line_offset)

        return Pair(beatb_y - beata_y, beatb.beat - beata.beat)
    }

    /**
     * Calculate which channel and line offset is denoted by the [absolute]th line
     */
    fun get_std_offset(absolute: Int): Pair<Int, Int> {
        var count = 0
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            for (j in 0 until channel.size) {
                if (count == absolute) {
                    return Pair(i, j)
                }
                count += 1
            }
        }
        throw IndexOutOfBoundsException()
    }

    /**
     * Get the midi instrument current used by Channel [channel]
     */
    fun get_channel_instrument(channel: Int): Pair<Int, Int> {
        return this.channels[channel].get_instrument()
    }

    /**
     * Get the percussion instrument used on the [line_offset]th line of the percussion channel
     */
    open fun get_percussion_instrument(line_offset: Int): Int {
        val channel = this.channels.last()
        return channel.get_mapped_line_offset(line_offset) ?: BaseLayer.DEFAULT_PERCUSSION
    }

    /**
     * Get the tree structure found at the BeatKey [beat_key]
     */
    fun get_beat_tree(beat_key: BeatKey): OpusTree<OpusEvent> {
        if (beat_key.channel >= this.channels.size) {
            throw BadBeatKey(beat_key)
        }

        val line_offset: Int = if (beat_key.line_offset < 0) {
            this.channels[beat_key.channel].size - beat_key.line_offset
        } else {
            beat_key.line_offset
        }

        if (line_offset > this.channels[beat_key.channel].size) {
            throw BadBeatKey(beat_key)
        }

        return this.channels[beat_key.channel].get_tree(line_offset, beat_key.beat)
    }

    /**
     * Get the tree structure found within the BeatKey [beat_key] at [position]
     */
    fun get_tree(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent> {
        try {
            return this.channels[beat_key.channel].get_tree(
                beat_key.line_offset,
                beat_key.beat,
                position
            )
        } catch (e: OpusTree.InvalidGetCall) {
            throw e
        }
    }

    /**
     * Get the leaf immediately after the tree found at [beat_key]/[position], if any
     * *it may not be an immediate sibling, rather an aunt, neice, etc*
     */
    fun get_proceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent>? {
        val pair = this.get_proceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    /**
     * Get the last event before the tree at [beat_key]/[position], if any exist.
     * This may or may not be in the preceding leaf, but will look for the first leaf with an associated event.
     */
    fun get_preceding_event(beat_key: BeatKey, position: List<Int>): OpusEvent? {
        // Gets first preceding event. may skip empty leafs
        var working_position = position.toList()
        var working_beat_key = beat_key
        while (!this.get_tree(working_beat_key, working_position).is_event()) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: return null
            working_beat_key = pair.first
            working_position = pair.second
        }
        return this.get_tree(working_beat_key, working_position).get_event()
    }

    /**
     * Get the leaf immediately before the tree found at [beat_key]/[position], if any
     * *it may not be an immediate sibling, rather an aunt, neice, etc*
     */
    fun get_preceding_leaf(beat_key: BeatKey, position: List<Int>): OpusTree<OpusEvent>? {
        // Gets first preceding leaf, event or not
        val pair = this.get_preceding_leaf_position(beat_key, position) ?: return null
        return this.get_tree(pair.first, pair.second)
    }

    /**
     * Get the location of the leaf immediately before the tree found at [beat_key]/[position].
     * *it may not be an immediate sibling, rather an aunt, neice, etc*
     */
    fun get_preceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        val working_position = position.toMutableList()
        val working_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat)

        // Move left/up
        while (true) {
            if (working_position.isNotEmpty()) {
                if (working_position.last() > 0) {
                    working_position[working_position.size - 1] -= 1
                    break
                } else {
                    working_position.removeLast()
                }
            } else if (working_beat_key.beat > 0) {
                working_beat_key.beat -= 1
                break
            } else {
                return null
            }
        }

        var working_tree = this.get_tree(working_beat_key, working_position)

        // Move right/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(working_tree.size - 1)
            working_tree = working_tree[working_tree.size - 1]
        }

        return Pair(working_beat_key, working_position)
    }

    /**
     * Get the location of the leaf immediately after the tree found at [beat_key]/[position].
     * *it may not be an immediate sibling, rather an aunt, neice, etc*
     */
    fun get_proceding_leaf_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        var working_position = position.toMutableList()
        val working_beat_key = BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat)
        var working_tree = this.get_tree(working_beat_key, working_position)

        // Move right/up
        while (true) {
            if (working_tree.parent != null) {
                if (working_tree.parent!!.size - 1 > working_position.last()) {
                    working_position[working_position.size - 1] += 1
                    working_tree = this.get_tree(working_beat_key, working_position)
                    break
                } else {
                    working_position.removeLast()
                    working_tree = working_tree.parent!!
                }
            } else if (working_beat_key.beat < this.beat_count - 1) {
                working_beat_key.beat += 1
                working_position = mutableListOf()
                working_tree = this.get_tree(working_beat_key, working_position)
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
        return Pair(working_beat_key, working_position)
    }

    /**
     * Get the value of the event at location[beat_key]/[position], if any.
     * if the event is relative, it will look back and add up preceding values
     */
    open fun get_absolute_value(beat_key: BeatKey, position: List<Int>): Int? {
        val tree = this.get_tree(beat_key, position)

        var abs_value: Int
        if (tree.is_event()) {
            val event = tree.get_event()!!
            if (!event.relative) {
                return event.note
            } else {
                abs_value = event.note
            }
        } else {
            return null
        }

        var working_beat_key = beat_key
        var working_position = position.toList()

        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: return abs_value
            working_beat_key = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat_key, working_position)

            if (working_tree.is_event()) {
                val working_event = working_tree.get_event()!!
                abs_value += working_event.note
                if (!working_event.relative) {
                    break
                }
            }
        }
        return abs_value
    }

    /**
     * Get an ordered list the size of the number of channels in use, with values corresponding to the
     * number of lines in each channel
     */
    fun get_channel_line_counts(): List<Int> {
        // TODO: Use Array?
        val output: MutableList<Int> = mutableListOf()
        for (i in 0 until this.channels.size) {
            output.add(this.channels[i].size)
        }
        return output
    }

    /**
     * Check if the tree at location [beat_key]/[position] has any absolute event
     * between it and the beginning of the opus on it's line.
     */
    fun has_preceding_absolute_event(beat_key: BeatKey, position: List<Int>): Boolean {
        var working_beat_key = beat_key
        var working_position = position.toList()

        var output = false
        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            working_beat_key = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat_key, working_position)

            if (working_tree.is_event()) {
                val working_event = working_tree.get_event()!!
                if (!working_event.relative) {
                    output = true
                    break
                }
            }
        }

        return output
    }

    /**
     * Artifact. Checks if the [channel] is assigned to be used as percussion.
     * The last channel is now always used as percussion.
     */
    fun is_percussion(channel: Int): Boolean {
        return channel == this.channels.size - 1
    }
    //// END RO Functions ////

    /**
     * Recalculate the event of the tree @ [beat_key]/[position]
     * to be relative to the events before it, if it isn't already
     *
     * @throws NonEventConversion If no event is present
     */
    fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw NonEventConversion(beat_key, position)
        }

        val event = tree.get_event()!!
        if (event.relative) {
            return
        }

        var working_beat_key: BeatKey = beat_key
        var working_position: List<Int> = position
        var preceding_value: Int? = null
        while (preceding_value == null) {
            val pair = this.get_preceding_leaf_position(working_beat_key, working_position) ?: break
            preceding_value = this.get_absolute_value(pair.first, pair.second)
            working_beat_key = pair.first
            working_position = pair.second
        }

        if (preceding_value == null) {
            this.set_event(
                beat_key, position, OpusEvent(
                    event.note,
                    event.radix,
                    event.channel,
                    true,
                    event.duration
                )
            )
        } else {
            this.set_event(
                beat_key, position, OpusEvent(
                    event.note - preceding_value,
                    event.radix,
                    event.channel,
                    true,
                    event.duration
                )
            )
        }
    }

    /**
     * Recalculate the event of the tree @ [beat_key]/[position]
     * to not depend on preceding events
     *
     * @throws NonEventConversion If no event is present
     */
    fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            throw NonEventConversion(beat_key, position)
        }

        val event = tree.get_event()!!
        if (!event.relative) {
            return
        }

        // The implied first value can be 0
        val value = this.get_absolute_value(beat_key, position) ?: event.note
        if (value < 0 || value > 95) {
            throw NoteOutOfRange(value)
        }
        this.set_event(beat_key, position, OpusEvent(
            value,
            event.radix,
            event.channel,
            false,
            event.duration
        ))
    }

    /**
     * Insert a new tree @ [beat_key]/[position]
     *
     * @throws BadInsertPosition When attempting to insert a new tree next to a top-level tree
     */
    open fun insert(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }
        val parent_position = position.subList(0, position.size - 1)
        val tree = this.get_tree(beat_key, parent_position)

        val index = position.last()
        tree.insert(index, OpusTree())
    }
    /**
     * Insert a new tree after [beat_key]/[position]
     *
     * @throws BadInsertPosition When attempting to insert a new tree next to a top-level tree
     */
    open fun insert_after(beat_key: BeatKey, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val tree = this.get_tree(beat_key, position)
        val parent = tree.get_parent()!!

        val index = position.last()
        parent.insert(index + 1, OpusTree())
    }

    /**
     * Remove tree @ [beat_key]/[position] if it's not a top-level tree
     */
    open fun remove(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        // Can't remove beat
        if (tree.parent == null || position.isEmpty()) {
            return
        }

        val parent_tree = tree.parent!!

        when (parent_tree.size) {
            1 -> {
                val next_position = position.toMutableList()
                next_position.removeLast()
                if (next_position.isNotEmpty()) {
                    this.remove(beat_key, next_position)
                }
                tree.detach()
            }
            2 -> {
                tree.detach()
                val prev_position = position.toMutableList()
                prev_position.removeLast()
                val to_replace = parent_tree[0]
                this.replace_tree(beat_key, prev_position, to_replace)
            }
            else -> {
                tree.detach()
            }
        }
    }

    open fun set_radix(radix: Int, mod_events: Boolean = true) {
        this.radix = radix
        if (!mod_events) {
            return
        }
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (this.is_percussion(i)) {
                return@forEachIndexed
            }

            channel.lines.forEachIndexed { j: Int, line: OpusChannel.OpusLine ->
                line.beats.forEachIndexed { k: Int, beat_tree: OpusTree<OpusEvent> ->
                    beat_tree.traverse { tree: OpusTree<OpusEvent>, event: OpusEvent? ->
                        if (event == null) {
                            return@traverse
                        }

                        var position = tree.get_path()
                        var new_event = event.copy()
                        var octave = (event.note / event.radix)

                        new_event.radix = radix
                        new_event.note = (octave * radix) + ((event.note % event.radix) * radix / event.radix)

                        this.set_event(BeatKey(i, j, k), position, new_event)
                    }
                }
            }

        }
    }

    open fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        if (!this.is_percussion(beat_key.channel)) {
            throw PercussionEventSet()
        }

        val tree = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            tree.unset_event()
        }

        val instrument = this.get_percussion_instrument(beat_key.line_offset)
        tree.set_event(OpusEvent(
            instrument,
            this.radix,
            9,
            false
        ))
    }

    open fun set_percussion_instrument(line_offset: Int, instrument: Int) {
        val channel = this.channels.last()
        channel.map_line(line_offset, instrument)
    }

    open fun set_channel_instrument(channel: Int, instrument: Pair<Int, Int>) {
        this.channels[channel].set_instrument(instrument)
    }
    open fun set_channel_program(channel: Int, program: Int) {
        this.channels[channel].midi_program = program
    }
    open fun set_channel_bank(channel: Int, bank: Int) {
        this.channels[channel].midi_bank = bank
    }

    open fun set_percussion_channel(channel: Int, program: Int = 0) {
        this.channels[channel].midi_program = program
        this.channels[channel].midi_bank = 128
        this.channels[channel].midi_channel = 9
    }

    open fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        if (this.is_percussion(beat_key.channel)) {
            throw NonPercussionEventSet()
        }
        val tree = this.get_tree(beat_key, position)
        tree.set_event(event)
    }

    open fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        var tree: OpusTree<OpusEvent> = this.get_tree(beat_key, position)
        if (tree.is_event()) {
            val event = tree.get_event()!!

            tree.unset_event()
            tree.set_size(splits)

            if (splits > 1) {
                tree = tree[0]
            }

            tree.set_event(event)
        } else {
            tree.set_size(splits)
        }
    }

    open fun unset(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)

        tree.unset_event()
        tree.empty()

        if (tree.parent != null) {
            val index = tree.getIndex()
            tree.parent!!.divisions.remove(index)
        }
    }

    private fun get_next_available_midi_channel(): Int {
        // Find the next available MIDI channel, ignore '9' which needs to be manually set
        // NOTE: This *will* generate past MIDI 1's limit of 16 channels
        val used_channels: MutableSet<Int> = mutableSetOf(9)
        for (channel in this.channels) {
            used_channels.add(channel.midi_channel)
        }
        var new_channel = 0
        while (new_channel in used_channels) {
            new_channel += 1
        }

        return new_channel
    }

    private fun gen_channel_uuid(): Int {
        val output = this._channel_uuid_generator
        this._channel_uuid_generator += 1
        return output
    }

    open fun new_channel(channel: Int? = null, lines: Int = 1, uuid: Int? = null) {
        val new_channel = OpusChannel(uuid ?: this.gen_channel_uuid())
        new_channel.set_beat_count(this.beat_count)
        new_channel.midi_channel = if (this.channels.isNotEmpty()) {
            this.get_next_available_midi_channel()
        } else {
            9
        }
        this._channel_uuid_map[new_channel.uuid] = new_channel
        for (i in 0 until lines) {
            new_channel.new_line(i)
        }

        if (this.channels.isNotEmpty()) {
            // Always insert new channels BEFORE the percussion channel
            if (channel != null) {
                this.channels.add(min(channel, this.channels.size - 1), new_channel)
            } else {
                this.channels.add(this.channels.size - 1, new_channel)
            }
        } else {
            new_channel.midi_bank = 128
            new_channel.midi_program = 0
            this.channels.add(new_channel) // Will be the percussion channel
        }
    }

    open fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        if (this.is_percussion(channel_old) != this.is_percussion(channel_new)) {
            throw IncompatibleChannelException(channel_old, channel_new)
        }

        val line = try {
            this.remove_line(channel_old, line_old)
        } catch (_: OpusChannel.LastLineException) {
            this.new_line(channel_old, 1)
            this.remove_line(channel_old, line_old)
        }

        if (channel_old == channel_new) {
            if (line_old < line_new) {
                this.insert_line(channel_new, line_new - 1, line)
            } else {
                this.insert_line(channel_new, line_new, line)
            }
        } else {
            this.insert_line(channel_new, line_new, line)
        }
    }

    open fun insert_beat(beat_index: Int, count: Int) {
        for (i in 0 until count) {
            this.insert_beat(beat_index + i)
        }
    }

    open fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>? = null) {
        this.beat_count += 1
        for (channel in this.channels) {
            channel.insert_beat(beat_index)
            channel.set_beat_count(this.beat_count)
        }
        if (beats_in_column == null) {
            return
        }
        var y = 0
        this.channels.forEach { channel: OpusChannel ->
            channel.lines.forEach { line: OpusChannel.OpusLine ->
                line.beats[beat_index] = beats_in_column[y]
                y += 1
            }
        }
    }

    open fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        this.channels[channel].insert_line(line_offset, line)
    }

    open fun new_line(channel: Int, line_offset: Int? = null): OpusChannel.OpusLine {
        return this.channels[channel].new_line(line_offset ?: this.channels[channel].lines.size)
    }

    open fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        val new_tree = this.channels[new_beat.channel].get_line(new_beat.line_offset).beats[new_beat.beat].copy()

        this.replace_tree(old_beat, listOf(), new_tree)
    }

    open fun remove_beat(beat_index: Int) {
        if (this.beat_count == 1) {
            throw RemovingLastBeatException()
        }
        for (channel in this.channels) {
            channel.remove_beat(beat_index)
        }
        this.set_beat_count(this.beat_count - 1)
    }

    fun remove_channel_by_uuid(uuid: Int) {
        val channel = this._channel_uuid_map[uuid] ?: throw OpusChannel.InvalidChannelUUID(uuid)
        var channel_index: Int? = null
        for (i in 0 until this.channels.size) {
            if (this.channels[i] == channel) {
                channel_index = i
                break
            }
        }
        if (channel_index != null) {
            this.remove_channel(channel_index)
        }
    }

    open fun remove_channel(channel: Int) {
        val opus_channel = this.channels.removeAt(channel)
        this._channel_uuid_map.remove(opus_channel.uuid)
    }

    open fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        val from_tree = this.get_tree(beatkey_from, position_from).copy()
        this.replace_tree(beatkey_to, position_to, from_tree)
        this.unset(beatkey_from, position_from)
    }

    open fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        return this.channels[channel].remove_line(line_offset)

    }

    private fun copy_func(tree: OpusTree<OpusEvent>): OpusEvent? {
        return if (tree.event == null) {
            null
        } else {
            tree.event!!.copy()
        }
    }

    open fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        val tree_copy = tree.copy(this::copy_func)
        this.channels[beat_key.channel].replace_tree(beat_key.line_offset, beat_key.beat, position, tree_copy)
    }

    open fun replace_beat_tree(beat_key: BeatKey, tree: OpusTree<OpusEvent>) {
        this.replace_tree(beat_key, listOf(), tree)
    }

    open fun set_beat_count(new_count: Int) {
        this.beat_count = new_count
        for (channel in this.channels) {
            channel.set_beat_count(new_count)
        }
    }

    open fun get_midi(start_beat: Int = 0, end_beat_rel: Int? = null): Midi {
        data class StackItem(var tree: OpusTree<OpusEvent>, var divisions: Int, var offset: Int, var size: Int)
        data class PseudoMidiEvent(var channel: Int, var note: Int, var bend: Int, var velocity: Int, var uuid: Int)
        var event_uuid_gen = 0

        val end_beat = if (end_beat_rel == null) {
            this.beat_count
        } else if (end_beat_rel < 0) {
            this.beat_count + end_beat_rel
        } else {
            end_beat_rel
        }

        val midi = Midi()
        midi.insert_event(0,0, SetTempo.from_bpm(this.tempo))

        val pseudo_midi_map = mutableListOf<Triple<Int, PseudoMidiEvent, Boolean>>()
        val max_tick = midi.get_ppqn() * (this.beat_count + 1)

        this.channels.forEachIndexed { c: Int, channel: OpusChannel ->
            midi.insert_event(
                0,
                0,
                BankSelect(channel.midi_channel, channel.midi_bank)
            )
            midi.insert_event(
                0,
                0,
                ProgramChange(channel.midi_channel, channel.midi_program)
            )
            channel.lines.forEachIndexed { l: Int, line: OpusChannel.OpusLine ->
                var current_tick = 0
                var prev_note = 0
                line.beats.forEachIndexed { b: Int, beat: OpusTree<OpusEvent> ->
                    val stack: MutableList<StackItem> = mutableListOf(StackItem(beat, 1, current_tick, midi.ppqn))
                    while (stack.isNotEmpty()) {
                        val current = stack.removeFirst()
                        if (current.tree.is_event()) {
                            val event = current.tree.get_event()!!
                            val (note, bend) = if (this.is_percussion(c)) { // Ignore the event data and use percussion map
                                Pair(this.get_percussion_instrument(l) + 27, 0)
                            } else {
                                val current_note = if (event.relative) {
                                    event.note + prev_note
                                } else {
                                    event.note
                                }

                                val octave = current_note / event.radix
                                val offset = current_note % event.radix
                                val std_offset = (offset.toDouble() * 12.0 / event.radix.toDouble())
                                val bend = ((std_offset - floor(std_offset)) * 512.0).toInt()

                                prev_note = current_note

                                Pair(
                                    (octave * 12) + std_offset.toInt() + 21 + this.transpose,
                                    bend
                                )
                            }

                            if (!(b < start_beat || b >= end_beat)) {
                                val pseudo_event = PseudoMidiEvent(
                                    channel.midi_channel,
                                    note,
                                    bend,
                                    line.volume,
                                    event_uuid_gen++
                                )
                                pseudo_midi_map.add(Triple(
                                    current.offset,
                                    pseudo_event,
                                    true
                                ))
                                pseudo_midi_map.add(Triple(
                                    min(current.offset + (current.size * event.duration), max_tick),
                                    pseudo_event,
                                    false
                                ))
                            }
                        } else if (!current.tree.is_leaf()) {
                            val working_subdiv_size = current.size / current.tree.size
                            for ((i, subtree) in current.tree.divisions) {
                                stack.add(
                                    StackItem(
                                        subtree,
                                        current.tree.size,
                                        current.offset + (working_subdiv_size * i),
                                        working_subdiv_size
                                    )
                                )
                            }
                        }
                    }

                    if (!(b < start_beat || b >= end_beat)) {
                        current_tick += midi.ppqn
                    }
                }
            }
        }

        pseudo_midi_map.sortBy {
            (it.first * 2) + (if (it.third) { 1 } else { 0})
        }

        val index_map = HashMap<PseudoMidiEvent, Int>()
        for ((tick, pseudo_event, is_on) in pseudo_midi_map) {
            midi.insert_event(
                0,
                tick,
                if (is_on) {
                    if (this.radix != 12) {
                        var current_index = 0
                        while (index_map.containsValue(current_index)) {
                            current_index += 1
                        }
                        index_map[pseudo_event] = current_index
                        NoteOn79(
                            index = current_index,
                            note = pseudo_event.note,
                            bend = pseudo_event.bend,
                            channel = pseudo_event.channel,
                            velocity = pseudo_event.velocity shl 8
                        )
                    } else {
                        NoteOn(
                            channel = pseudo_event.channel,
                            note = pseudo_event.note,
                            velocity = pseudo_event.velocity
                        )
                    }
                } else {
                    if (this.radix != 12) {
                        NoteOff79(
                            index = index_map.remove(pseudo_event)!!,
                            note = pseudo_event.note,
                            bend = pseudo_event.bend,
                            channel = pseudo_event.channel,
                            velocity = pseudo_event.velocity shl 8
                        )
                    } else {
                        NoteOff(
                            channel = pseudo_event.channel,
                            note = pseudo_event.note,
                            velocity = pseudo_event.velocity
                        )
                    }
                }
            )
        }

        for (beat in start_beat .. end_beat) {
            midi.insert_event(
                0,
                midi.ppqn * (beat - start_beat),
                SongPositionPointer(beat)
            )
        }

        return midi
    }

    open fun to_json(): LoadedJSONData {
        val channels: MutableList<ChannelJSONData> = mutableListOf()
        for (channel in this.channels) {
            val lines: MutableList<OpusTreeJSON> = mutableListOf()
            val line_volumes: MutableList<Int> = mutableListOf()
            for (i in 0 until channel.size) {
                val line = channel.get_line(i)
                val tree_children = mutableListOf<OpusTreeJSON?>()
                for (beat in line.beats) {
                    if (channel.midi_channel == 9) {
                        beat.traverse { _: OpusTree<OpusEvent>, event: OpusEvent? ->
                            if (event != null) {
                                event.note = channel.get_mapped_line_offset(i) ?: BaseLayer.DEFAULT_PERCUSSION
                            }
                        }
                    }
                    tree_children.add(this.tree_to_json(beat))
                }
                lines.add(
                    OpusTreeJSON( null, tree_children )
                )
                line_volumes.add(line.volume)
            }

            channels.add(
                ChannelJSONData(
                    midi_channel = channel.midi_channel,
                    midi_bank = channel.midi_bank,
                    midi_program = channel.midi_program,
                    lines = lines,
                    line_volumes = line_volumes
                )
            )
        }

        return LoadedJSONData(
            name = this.project_name,
            tempo = this.tempo,
            radix = this.radix,
            channels = channels,
            transpose = this.transpose
        )
    }

    open fun save(path: String? = null) {
        if (path == null && this.path == null) {
            throw EmptyPath()
        }

        if (path != null) {
            this.path = path
        }

        val file_obj = File(this.path!!)
        val json_string = Json.encodeToString(this.to_json())
        file_obj.writeText(json_string)
    }

    // Clear function is used for new projects
    open fun clear() {
        this.beat_count = 0
        for (i in this.channels.size - 1 downTo 0) {
            this.remove_channel(i)
        }
        this.path = null
        this.project_name = "New Opus"
        this.tempo = 120F
        this.radix = 12
        this.transpose = 0
    }

    open fun load(path: String) {
        this.load_json_file(path)
    }

    open fun load(bytes: ByteArray, new_path: String? = null) {
        val json_content = bytes.toString(Charsets.UTF_8)
        val json_data: LoadedJSONData = try {
            Json.decodeFromString<LoadedJSONData>(json_content)
        } catch (e: Exception) {
            val old_data = Json.decodeFromString<LoadedJSONData0>(json_content)
            this.convert_old_fmt(old_data)
        }

        this.load_json(json_data)
        this.path = new_path
    }

    fun convert_old_fmt(old_data: LoadedJSONData0): LoadedJSONData {
        val new_channels = mutableListOf<ChannelJSONData>()
        for (channel in old_data.channels) {
            val new_lines = mutableListOf<OpusTreeJSON>()
            for (line_string in channel.lines) {
                val line_children = mutableListOf<OpusTreeJSON?>()
                line_string.split("|").forEach { beat_string: String ->
                    val beat_tree = from_string(beat_string, old_data.radix, channel.midi_channel)
                    beat_tree.clear_singles()

                    line_children.add(this.tree_to_json(beat_tree))
                }
                new_lines.add(OpusTreeJSON(null, line_children))
            }
            new_channels.add(
                ChannelJSONData(
                    midi_channel = channel.midi_channel,
                    midi_bank = channel.midi_bank,
                    midi_program = channel.midi_program,
                    lines = new_lines,
                    line_volumes = channel.line_volumes
                )
            )
        }
        return LoadedJSONData(
            tempo = old_data.tempo,
            radix = old_data.radix,
            channels = new_channels,
            reflections = old_data.reflections,
            transpose = old_data.transpose,
            name = old_data.name
        )
    }

    open fun new() {
        this.clear()
        this.new_channel()
        this.new_channel()
        this.insert_beat(0, 4)
        this.set_project_name(this.project_name)
    }

    open fun load_json_file(path: String) {
        val json_content = File(path).readText(Charsets.UTF_8)
        val json_data: LoadedJSONData = try {
            Json.decodeFromString<LoadedJSONData>(json_content)
        } catch (e: Exception) {
            val old_data = Json.decodeFromString<LoadedJSONData0>(json_content)
            this.convert_old_fmt(old_data)
        }
        this.load_json(json_data)
        this.path = path
    }

    private fun parse_line_data(json_data: LoadedJSONData): List<List<List<OpusTree<OpusEvent>>>> {
        fun tree_from_json(input_tree: OpusTreeJSON?): OpusTree<OpusEvent> {
            val new_tree = OpusTree<OpusEvent>()
            if (input_tree == null) {
                return new_tree
            }

            if (input_tree.event != null) {
                new_tree.set_event(input_tree.event!!)
                return new_tree
            }

            if (input_tree.children != null) {
                new_tree.set_size(input_tree.children!!.size)
                input_tree.children!!.forEachIndexed { i: Int, child: OpusTreeJSON? ->
                    new_tree.set(i, tree_from_json(child))
                }
            }

            return new_tree
        }

        val output = mutableListOf<MutableList<MutableList<OpusTree<OpusEvent>>>>()
        json_data.channels.forEach { channel_data: ChannelJSONData ->
            val line_list = mutableListOf<MutableList<OpusTree<OpusEvent>>>()
            channel_data.lines.forEach { input_line: OpusTreeJSON ->
                val beat_list = mutableListOf<OpusTree<OpusEvent>>()
                val line_tree = tree_from_json(input_line)
                for (i in 0 until line_tree.size) {
                    beat_list.add(line_tree[i])
                }
                line_list.add(beat_list)
            }
            output.add(line_list)
        }
        return output
    }

    open fun load_json(json_data: LoadedJSONData) {
        // Parse line_data first, if there's an error, it can be caught and ignored so the
        // active project is unaffected
        val parsed = this.parse_line_data(json_data)
        this.clear()

        this.radix = json_data.radix
        this.tempo = json_data.tempo
        this.transpose = json_data.transpose
        this.set_project_name(json_data.name)

        var percussion_channel: Int? = null
        var beat_count = 0
        var y = 0

        // Insert Drum Channel
        this.new_channel()

        json_data.channels.forEachIndexed { i: Int, channel_data ->
            if (channel_data.midi_channel == 9) {
                percussion_channel = i
                for (j in 0 until channel_data.lines.size - 1) {
                    this.new_line(this.channels.size - 1)
                }
            } else {
                this.new_channel(lines = channel_data.lines.size)
                y += 1
            }

            for (j in 0 until channel_data.lines.size) {
                beat_count = max(beat_count, parsed[i][j].size)
            }
        }


        this.insert_beat(0, beat_count)
        y = 0
        json_data.channels.forEachIndexed { i, channel_data ->
            // Separate out percussion channel, just in case it isn't at the end of the channels
            if (channel_data.midi_channel == 9) {
                return@forEachIndexed
            }


            this.channels[y].midi_channel = channel_data.midi_channel
            this.channels[y].midi_bank = channel_data.midi_bank
            this.channels[y].midi_program = channel_data.midi_program

            for (j in 0 until channel_data.lines.size) {
                parsed[i][j].forEachIndexed { b: Int, beat_tree: OpusTree<OpusEvent> ->
                    this.replace_tree(BeatKey(y, j, b), listOf(), beat_tree)
                }
            }

            channel_data.line_volumes.forEachIndexed { j: Int, volume: Int ->
                this.channels[y].lines[j].volume = volume
            }
            y += 1
        }

        if (percussion_channel != null) {
            val i = percussion_channel!!
            val channel_data = json_data.channels[i]
            this.channels[y].midi_channel = channel_data.midi_channel
            this.channels[y].midi_bank = channel_data.midi_bank
            this.channels[y].midi_program = channel_data.midi_program
            channel_data.line_volumes.forEachIndexed { j: Int, volume: Int ->
                this.channels[y].lines[j].volume = volume
            }

            for (j in 0 until channel_data.lines.size) {
                parsed[i][j].forEachIndexed { b: Int, beat_tree: OpusTree<OpusEvent> ->
                    this.replace_tree(BeatKey(y, j, b), listOf(), beat_tree)
                    for ((_, event) in beat_tree.get_events_mapped()) {
                        this.set_percussion_instrument(j, event.note)
                        break
                    }
                }
            }
        }
    }

    open fun import_midi(path: String) {
        val midi = Midi.from_path(path)
        this.import_midi(midi)
    }

    private fun tree_from_midi(midi: Midi): Triple<OpusTree<Set<OpusEvent>>, Float, List<Triple<Int, Int?, Int?>>> {
        var beat_size = midi.get_ppqn()
        var total_beat_offset = 0
        var last_ts_change = 0
        val beat_values: MutableList<OpusTree<Set<OpusEvent>>> = mutableListOf()
        var max_tick = 0
        var tempo = 120F
        val instrument_map = mutableListOf<Triple<Int, Int?, Int?>>()

        val active_event_map = HashMap<Pair<Int,Int>, OpusEvent>()
        val opus_event_duration_map = HashMap<OpusEvent, Float>()

        var denominator = 4F
        for (pair in midi.get_all_events()) {
            val tick = pair.first
            val event = pair.second

            max_tick = kotlin.math.max(tick, max_tick)
            val beat_index = ((tick - last_ts_change) / beat_size) + total_beat_offset
            val inner_beat_offset = (tick - last_ts_change) % beat_size

            if (event is NoteOn && event.get_velocity() > 0) {
                val (channel, note) = Pair(event.channel, event.get_note())

                // Add trees to list of trees
                while (beat_values.size <= beat_index) {
                    val new_tree = OpusTree<Set<OpusEvent>>()
                    new_tree.set_size(beat_size)
                    beat_values.add(new_tree)
                }

                val tree = beat_values[beat_index]
                val eventset = if (tree[inner_beat_offset].is_event()) {
                    tree[inner_beat_offset].get_event()!!.toMutableSet()
                } else {
                    mutableSetOf()
                }

                val opus_event = OpusEvent(
                    if (channel == 9) {
                        note - 27
                    } else {
                        note - 21
                    },
                    12,
                    channel,
                    false,
                    tick
                )
                eventset.add(opus_event)

                tree[inner_beat_offset].set_event(eventset)
                active_event_map[Pair(event.channel, event.get_note())] = opus_event
            } else if ((event is NoteOn && event.get_velocity() == 0) || event is NoteOff) {
                val (channel, note) = if (event is NoteOn) {
                    Pair(event.channel, event.get_note())
                } else {
                    Pair((event as NoteOff).channel, event.get_note())
                }
                val opus_event = active_event_map[Pair(channel, note)] ?: continue
                opus_event_duration_map[opus_event] = (tick - opus_event.duration).toFloat() / beat_size.toFloat()
            } else if (event is TimeSignature) {
                total_beat_offset += (tick - last_ts_change) / beat_size
                last_ts_change = tick
                denominator = 2F.pow(event.get_denominator())
                beat_size = (midi.get_ppqn().toFloat() * (4 / denominator)).toInt()
            } else if (event is SetTempo) {
                if (tick == 0) {
                    tempo = event.get_bpm() * (denominator / 4)
                }
            } else if (event is ProgramChange) {
                instrument_map.add(Triple(event.channel, null, event.get_program()))
            } else if (event is BankSelect) {
                instrument_map.add(Triple(event.channel, event.value, null))
            }
        }

        total_beat_offset += (max_tick - last_ts_change) / beat_size

        val opus = OpusTree<Set<OpusEvent>>()
        opus.set_size(beat_values.size)

        var overflow_events = mutableSetOf<OpusEvent>()
        beat_values.forEachIndexed { i, beat_tree ->
            // Quantize the beat ////////////
            val quantized_tree = OpusTree<Set<OpusEvent>>()
            quantized_tree.set_size(beat_tree.size)

            if (overflow_events.isNotEmpty()) {
                quantized_tree[0].set_event(overflow_events.toSet())
                overflow_events = mutableSetOf()
            }

            // Can easily merge quantized positions since the beats are still flat
            val qmap = beat_tree.get_quantization_map(listOf(2,2,2,3,5,7))
            for ((new_position, old_positions) in qmap) {
                val new_event_set = mutableSetOf<OpusEvent>()
                for (old_position in old_positions) {
                    val next_tree = beat_tree[old_position]
                    for (e in next_tree.get_event()!!) {
                        new_event_set.add(e)
                    }
                }

                if (new_position == quantized_tree.size) {
                    if (i < beat_values.size - 1) {
                        for (e in new_event_set) {
                            overflow_events.add(e)
                        }
                    }
                } else {
                    if (quantized_tree[new_position].is_event()) {
                        for (e in quantized_tree[new_position].get_event()!!) {
                            new_event_set.add(e)
                        }
                    }

                    quantized_tree[new_position].set_event(new_event_set.toSet())
                }
            }
            /////////////////////////////////////

            quantized_tree.reduce()
            quantized_tree.clear_singles()
            quantized_tree.traverse { tree: OpusTree<Set<OpusEvent>>, events: Set<OpusEvent>? ->
                if (events == null) {
                    return@traverse
                }

                var n = 1F
                var climb = tree.parent
                while (climb != null) {
                    n /= (climb.size).toFloat()
                    climb = climb.parent
                }

                for (event in events) {
                    val beat_ratio = opus_event_duration_map[event]
                    event.duration = if (beat_ratio == null) {
                        1
                    } else {
                        ceil(beat_ratio / n).toInt()
                    }
                }
            }
            opus.set(i, quantized_tree)
        }

        return Triple(opus, tempo, instrument_map)
    }

    open fun import_midi(midi: Midi) {
        this.clear()

        val (settree, tempo, instrument_map) = this.tree_from_midi(midi)
        this.tempo = tempo

        val mapped_events = settree.get_events_mapped()
        val midi_channel_map = HashMap<Int, Int>()
        val channel_sizes = mutableListOf<Int>()
        var percussion_channel: Int? = null

        val percussion_map = HashMap<Int, Int>()

        for ((_, event_set) in mapped_events) {
            val tmp_channel_counts = HashMap<Int, Int>()
            event_set.forEachIndexed { _: Int, event: OpusEvent ->
                if (!midi_channel_map.contains(event.channel)) {
                    midi_channel_map[event.channel] = midi_channel_map.size
                }
                val channel_index = midi_channel_map[event.channel]!!

                if (event.channel == 9) {
                    if (!percussion_map.contains(event.note)) {
                        percussion_map[event.note] = percussion_map.size
                    }
                    tmp_channel_counts[channel_index] = percussion_map.size
                } else {
                    if (!tmp_channel_counts.contains(channel_index)) {
                        tmp_channel_counts[channel_index] = 1
                    } else {
                        tmp_channel_counts[channel_index] = tmp_channel_counts[channel_index]!! + 1
                    }
                }
            }

            for ((channel, size) in tmp_channel_counts) {
                while (channel >= channel_sizes.size) {
                    channel_sizes.add(0)
                }
                channel_sizes[channel] = max(channel_sizes[channel], size)
            }
        }

        // Move Percussion to Last Opus Manager Channel
        if (midi_channel_map.containsKey(9)) {
            val channel = midi_channel_map[9]!!
            var new_channel = channel
            for ((mchannel, ochannel) in midi_channel_map) {
                if (mchannel == 9) continue
                if (ochannel > channel) {
                    new_channel = max(new_channel, ochannel)
                    midi_channel_map[mchannel] = ochannel - 1
                }
            }
            val percussion_line_count = channel_sizes.removeAt(channel)
            channel_sizes.add(percussion_line_count)
            midi_channel_map[9] = new_channel
        } else {
            // If no percussion is found, add an empty percussion track
            midi_channel_map[9] = channel_sizes.size
            channel_sizes.add(1)
        }

        this.new_channel(lines = channel_sizes[midi_channel_map[9]!!])

        val sorted_channels = midi_channel_map.values.sortedBy { it }
        sorted_channels.forEachIndexed { i: Int, channel: Int ->
            if (i == sorted_channels.size - 1) {
                return@forEachIndexed
            }
            this.new_channel(lines = channel_sizes[channel])
        }

        this.insert_beat(0, settree.size)

        val events_to_set = mutableSetOf<Triple<BeatKey, List<Int>, OpusEvent>>()
        for ((position, event_set) in mapped_events) {
            val tmp_channel_counts = HashMap<Int, Int>()
            val event_list = event_set.toMutableList()
            event_list.sortWith(compareBy { 127 - it.note })
            event_list.forEach { event: OpusEvent ->
                val channel_index = midi_channel_map[event.channel]!!
                if (event.channel == 9) {
                    percussion_channel = midi_channel_map[9]
                }

                val line_offset = tmp_channel_counts[channel_index] ?: 0
                tmp_channel_counts[channel_index] = line_offset + 1

                val working_position = mutableListOf<Int>()
                var working_beatkey: BeatKey? = null

                position.forEachIndexed { i: Int, (x, size): Pair<Int, Int> ->
                    if (i == 0) {
                        working_beatkey = BeatKey(channel_index, line_offset, x)
                    } else {
                        if (this.get_tree(working_beatkey!!, working_position).size != size) {
                            this.split_tree(working_beatkey!!, working_position, size)
                        }
                        working_position.add(x)
                    }
                }

                if (working_beatkey != null) {
                    events_to_set.add(Triple(working_beatkey!!, working_position, event))
                }
            }
        }

        for ((beatkey, position, event) in events_to_set) {
            if (event.channel == 9) {
                this.set_percussion_event(beatkey, position)
            } else {
                if (event.note in 0..127) {
                    this.set_event(beatkey, position, event)
                }
            }
        }

        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            for (j in channel.lines.indices) {
                for (k in 0 until this.beat_count) {
                    val beat_key = BeatKey(i, j, k)
                    val beat_tree = this.get_beat_tree(beat_key)
                    beat_tree.traverse { tree: OpusTree<OpusEvent>, event: OpusEvent? ->
                        if (event != null || tree.size <= 1 || !tree[0].is_event() || tree[0].event!!.duration < tree.size) {
                            return@traverse
                        }

                        for ((l, branch) in tree.divisions) {
                            if (l == 0) {
                                continue
                            }
                            if (!branch.is_eventless()) {
                                return@traverse
                            }
                        }

                        tree[0].event!!.duration = max(1, tree[0].event!!.duration / tree.size)

                        if (tree == beat_tree) {
                            this.replace_beat_tree(beat_key, tree[0])
                        } else {
                            tree.replace_with(tree[0])
                        }
                    }
                }
            }
        }

        if (percussion_channel != null) {
            for ((note, index) in percussion_map) {
                this.set_percussion_instrument(index, note)
            }
        }



        for ((midi_channel, bank, program) in instrument_map) {
            // Midi may have contained programchange event for channel, but no music
            val opus_channel = midi_channel_map[midi_channel] ?: continue
            if (bank != null) {
                this.set_channel_bank(opus_channel, bank)
            }
            if (program != null) {
                this.set_channel_program(opus_channel, program)
            }
        }
    }

    open fun set_line_volume(channel: Int, line_offset: Int, volume: Int) {
        this.channels[channel].set_line_volume(line_offset, volume)
    }

    fun get_line_volume(channel: Int, line_offset: Int): Int {
        return this.channels[channel].get_line_volume(line_offset)
    }

    open fun set_project_name(new_name: String) {
        this.project_name = new_name
    }

    open fun set_transpose(new_transpose: Int) {
        this.transpose = new_transpose
    }

    open fun set_tempo(new_tempo: Float) {
        this.tempo = new_tempo
    }

    fun get_ordered_beat_key_pair(first: BeatKey, second: BeatKey): Pair<BeatKey, BeatKey> {
        val (from_key, to_key) = if (first.channel < second.channel) {
            Pair(
                BeatKey(first.channel, first.line_offset, -1),
                BeatKey(second.channel, second.line_offset, -1)
            )
        } else if (first.channel == second.channel) {
            if (first.line_offset < second.line_offset) {
                Pair(
                    BeatKey(first.channel, first.line_offset, -1),
                    BeatKey(second.channel, second.line_offset, -1)
                )
            } else {
                Pair(
                    BeatKey(second.channel, second.line_offset, -1),
                    BeatKey(first.channel, first.line_offset, -1)
                )
            }
        } else {
            Pair(
                BeatKey(second.channel, second.line_offset, -1),
                BeatKey(first.channel, first.line_offset, -1)
            )
        }
        from_key.beat = min(first.beat, second.beat)
        to_key.beat = max(first.beat, second.beat)
        return Pair(from_key, to_key)
    }

    open fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        val (from_key, to_key) = this.get_ordered_beat_key_pair(first_corner, second_corner)

        // Start OverFlow Check ////
        var lines_in_range = 0
        var lines_available = 0
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (i < from_key.channel || i > to_key.channel) {
                return@forEachIndexed
            }
            for (j in 0 until channel.size) {
                if (i == from_key.channel && j < from_key.line_offset) {
                    continue
                } else if (i == to_key.channel && j > to_key.line_offset) {
                    continue
                }
                lines_in_range += 1
            }
        }
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (i < beat_key.channel) {
                return@forEachIndexed
            }
            for (j in 0 until channel.size) {
                if (i == beat_key.channel && j < beat_key.line_offset) {
                    continue
                }
                lines_available += 1
            }
        }

        val working_beat = beat_key.copy()
        while (from_key.channel != to_key.channel || from_key.line_offset != to_key.line_offset) {
            // INCLUSIVE
            for (b in 0 .. to_key.beat - from_key.beat) {
                this.overwrite_beat(
                    BeatKey(working_beat.channel, working_beat.line_offset, working_beat.beat + b),
                    BeatKey(from_key.channel, from_key.line_offset, from_key.beat + b)
                )
            }
            if (this.channels[from_key.channel].size - 1 > from_key.line_offset) {
                from_key.line_offset += 1
            } else if (this.channels.size - 1 > from_key.channel) {
                from_key.channel += 1
                from_key.line_offset = 0
            } else {
                break
            }

            if (this.channels[working_beat.channel].size - 1 > working_beat.line_offset) {
                working_beat.line_offset += 1
            } else if (this.channels.size - 1 > working_beat.channel) {
                working_beat.channel += 1
                working_beat.line_offset = 0
            } else {
                break
            }
        }

        for (b in 0 .. to_key.beat - from_key.beat) {
            this.overwrite_beat(
                BeatKey(working_beat.channel, working_beat.line_offset, working_beat.beat + b),
                BeatKey(from_key.channel, from_key.line_offset, from_key.beat + b)
            )
        }
    }

    open fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_event()) {
            // TODO: Throw error?
            return
        }

        tree.event!!.duration = duration
    }

    private fun tree_to_json(tree: OpusTree<OpusEvent>): OpusTreeJSON? {
        if (tree.is_leaf() && !tree.is_event()) {
            return null
        }

        val children = mutableListOf<OpusTreeJSON?>()
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                children.add(this.tree_to_json(tree[i]))
            }
        }

        return OpusTreeJSON(
            tree.event,
            if (children.isEmpty()) {
                null
            } else {
                children
            }
        )
    }

    fun convert_all_events_to_absolute() {
        this.channels.forEach { channel: OpusChannel ->
            channel.lines.forEach { line: OpusChannel.OpusLine ->
                line.beats.forEach { beat_tree: OpusTree<OpusEvent> ->
                    var previous_value = 0
                    beat_tree.traverse { _: OpusTree<OpusEvent>, event: OpusEvent? ->
                        if (event == null) {
                            return@traverse
                        }
                        if (event.relative) {
                            event.relative = false
                            event.note = event.note + previous_value
                        }

                        previous_value = event.note
                    }
                }
            }
        }
    }

    fun get_press_breakdown(): List<Pair<Double, Int>> {
        val tick_map = mutableListOf<Pair<Double, Boolean>>()
        this.channels.forEach { channel: OpusChannel ->
            channel.lines.forEach { line: OpusChannel.OpusLine ->
                line.beats.forEachIndexed { beat_index: Int, beat_tree: OpusTree<OpusEvent> ->
                    var previous_value = 0
                    beat_tree.traverse { tree: OpusTree<OpusEvent>, event: OpusEvent? ->
                        if (event == null) {
                            return@traverse
                        }

                        var tmp_tree = tree
                        val position = mutableListOf<Int>()
                        while (tmp_tree != beat_tree) {
                            position.add(0, tmp_tree.getIndex()!!)
                            tmp_tree = tmp_tree.get_parent()!!
                        }

                        var position_scalar: Double = 0.0
                        tmp_tree = beat_tree
                        var running_size = 1

                        for (p in position) {
                            running_size *= tmp_tree.size
                            position_scalar += p.toDouble() / running_size.toDouble()
                            tmp_tree = tmp_tree[p]
                        }

                        tick_map.add(
                            Pair(
                                position_scalar + beat_index,
                                true
                            )
                        )

                        tick_map.add(
                            Pair(
                                position_scalar + beat_index + (event.duration.toDouble() / running_size.toDouble()),
                                false
                            )
                        )
                    }
                }
            }
        }

        tick_map.sortBy { it.first }
        var breakdown = mutableListOf<Pair<Double, Int>>()

        var currently_on = 0
        var last_position = 0.0
        for ((position, state) in tick_map) {
            if (position != last_position) {
                breakdown.add(Pair((position - last_position) / (this.beat_count + 1).toDouble(), currently_on))
            }

            if (state) {
                currently_on += 1
            } else {
                currently_on -= 1
            }

            last_position = position
        }

        return breakdown
    }

    fun get_maximum_simultaneous_notes(): Int {
        return (this.get_press_breakdown().sortedBy { it.second }).last().second
    }

    /*
        Get the most usually active number of notes
     */
    fun get_mode_simultaneous_notes(): Pair<Int, Double> {
        val merged_counts = HashMap<Int, Double>()
        for ((percentage, press_count) in this.get_press_breakdown()) {
            merged_counts[press_count] = merged_counts.getOrDefault(press_count, 0.0) + percentage
        }
        var mode_press_count = 0
        var mode_percentage = 0.0
        for ((press_count, percentage) in merged_counts) {
            if ((percentage == mode_percentage && mode_press_count < press_count) || percentage > mode_percentage) {
                mode_press_count = press_count
                mode_percentage = percentage
            }
        }
        return Pair(mode_press_count, mode_percentage)
    }

    fun has_percussion(): Boolean {
        var channel = this.channels[this.channels.size - 1]
        return !channel.is_empty()
    }
}
