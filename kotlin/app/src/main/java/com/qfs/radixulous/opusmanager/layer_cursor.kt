package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.structure.OpusTree
import kotlin.math.min
import kotlin.math.max
// DEPRECATED
//open class CursorLayer() : HistoryLayer() {
//    var cursor: Cursor? = null
//
//    fun line_count(): Int {
//        var output: Int = 0
//        for (channel in this.channels) {
//            output += channel.size
//        }
//        return output
//    }
//
//    fun get_cursor(): Cursor {
//        if (this.cursor == null) {
//            this.cursor = Cursor(this)
//        }
//
//        return this.cursor!!
//    }
//
//
//    override fun new_line(channel: Int, line_offset: Int?): List<OpusTree<OpusEvent>> {
//        val output = super.new_line(channel, line_offset)
//        val beat = this.get_cursor().get_beatkey().beat
//        this.set_cursor_position(BeatKey(channel, line_offset ?: this.channels[channel].size - 1, beat), listOf())
//        return output
//    }
//
//    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
//        super.move_line(channel_old, line_old, channel_new, line_new)
//
//        val beat = this.get_cursor().get_beatkey().beat
//        this.set_cursor_position(BeatKey(channel_new, line_new, beat), this.get_cursor().get_position())
//    }
//
//    fun jump_to_beat(beat: Int) {
//        this.get_cursor().set_x(beat)
//        this.get_cursor().settle()
//    }
//
//    fun get_channel_index(y: Int): Pair<Int, Int> {
//        var counter = 0
//        for (channel in 0 until this.channels.size) {
//            for (i in 0 until this.channels[channel].size) {
//                if (counter == y) {
//                    return Pair(channel, i)
//                }
//                counter += 1
//            }
//        }
//        throw Exception("IndexError $y / $counter")
//    }
//
//    fun get_y(channel: Int, rel_line_offset: Int): Int {
//        val line_offset = if (rel_line_offset < 0) {
//            this.channels[channel].size + rel_line_offset
//        } else {
//            rel_line_offset
//        }
//
//        var y: Int = 0
//        for (i in 0 until this.channels.size) {
//            for (j in 0 until this.channels[i].size) {
//                if (channel == i && line_offset == j) {
//                    return y
//                }
//                y += 1
//            }
//        }
//
//        return -1
//    }
//
//    ///////// OpusManagerBase methods
//    override fun insert_beat(beat_index: Int) {
//        super.insert_beat(beat_index)
//        this.get_cursor().settle()
//    }
//
//    override fun remove_beat(beat_index: Int) {
//        super.remove_beat(beat_index)
//        this.get_cursor().settle()
//    }
//
//    override fun remove(beat_key: BeatKey, position: List<Int>) {
//        super.remove(beat_key, position)
//        val new_position = position.toMutableList()
//        if (new_position.last() > 0) {
//            new_position -= 1
//        } else {
//            // parent can't be null here, if it were to be, the super.remove() would throw an error
//            try {
//                // If get() called on leaf, we know to pop the last positional index
//                this.get_tree(beat_key, position)
//            } catch (e: Exception) {
//                new_position.removeLast()
//            }
//        }
//        this.set_cursor_position(beat_key, new_position)
//    }
//
//    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
//        super.split_tree(beat_key, position, splits)
//        this.get_cursor().settle()
//    }
//
//    override fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
//        var new_key = BeatKey(
//            channel,
//            max(0, line_offset),
//            this.get_cursor().get_beatkey().beat
//        )
//        if (line_offset <= this.channels[channel].size - 1) {
//            new_key.line_offset = max(0, new_key.line_offset - 1)
//            this.set_cursor_position(
//                new_key,
//                listOf()
//            )
//        }
//        val output = super.remove_line(channel, line_offset)
//        return output
//    }
//
//    override fun remove_channel(channel: Int) {
//        super.remove_channel(channel)
//        if (channel == 0) {
//            if (this.channels.isNotEmpty()) {
//                this.set_cursor_position(
//                    BeatKey(0, 0, this.get_cursor().get_beatkey().beat),
//                    listOf()
//                )
//            }
//        } else {
//            this.set_cursor_position(
//                BeatKey(
//                    channel - 1,
//                    this.channels[channel - 1].size - 1,
//                    this.get_cursor().get_beatkey().beat
//                ),
//                listOf()
//            )
//        }
//    }
//
//    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
//        super.overwrite_beat(old_beat, new_beat)
//        this.set_cursor_position(old_beat, listOf())
//    }
//
//    override fun new() {
//        this.get_cursor().set(0,0, listOf(0))
//        super.new()
//        this.get_cursor().settle()
//    }
//
//    override fun load(path: String) {
//        this.get_cursor().set(0,0, listOf(0))
//        super.load(path)
//        this.get_cursor().settle()
//    }
//
//    override fun import_midi(midi: MIDI) {
//        this.get_cursor().set(0,0, listOf(0))
//        super.import_midi(midi)
//        this.get_cursor().settle()
//    }
//
//    // NOTE: This *has* to be in the CursorLayer. a beatkey range assumes
//    open fun get_cursor_difference(beata: BeatKey, beatb: BeatKey): Pair<Int, Int> {
//        val beata_y = this.get_y(beata.channel, beata.line_offset)
//        val beatb_y = this.get_y(beatb.channel, beatb.line_offset)
//
//        return Pair(beatb_y - beata_y, beatb.beat - beata.beat)
//    }
//
//    open fun link_beat_range(beat: BeatKey, target_a: BeatKey, target_b: BeatKey) {
//        val cursor_diff = this.get_cursor_difference(target_a, target_b)
//
//        val (y_diff, y_i) = if (cursor_diff.first >= 0) {
//            Pair(
//                cursor_diff.first,
//                this.get_y(target_a.channel, target_a.line_offset)
//            )
//        } else {
//            Pair(
//                0 - cursor_diff.first,
//                this.get_y(target_b.channel, target_b.line_offset)
//            )
//        }
//
//        val (x_diff, x_i) = if (cursor_diff.second >= 0) {
//            Pair(
//                cursor_diff.second,
//                target_a.beat
//            )
//        } else {
//            Pair(
//                0 - cursor_diff.second,
//                target_b.beat
//            )
//        }
//        var y_new = this.get_y(beat.channel, beat.line_offset)
//        if (cursor_diff.first < 0) {
//            y_new -= y_diff
//        }
//        var x_new = beat.beat
//        if (cursor_diff.second < 0) {
//            x_new -= x_diff
//        }
//
//
//        if (y_new in (y_i .. y_diff + y_i) && x_new in (x_i .. x_diff + x_i)) {
//            throw Exception("Can't link a beat to its self")
//        }
//        if (y_i in (y_new .. y_diff + y_new) && x_i in (x_new .. x_diff + x_new)) {
//            throw Exception("Can't link a beat to its self")
//        }
//
//        val new_pairs = mutableListOf<Pair<BeatKey, BeatKey>>()
//        for (y in 0 .. y_diff) {
//            val pair = this.get_channel_index(y + y_new)
//            val target_pair = this.get_channel_index(y + y_i)
//            for (x in 0 .. x_diff) {
//                val working_position = BeatKey(
//                    pair.first,
//                    pair.second,
//                    x + x_new
//                )
//
//                val working_target = BeatKey(
//                    target_pair.first,
//                    target_pair.second,
//                    x + x_i
//                )
//                new_pairs.add(Pair(working_position, working_target))
//            }
//        }
//        this.batch_link_beats(new_pairs)
//    }
//
//    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
//        super.set_event(beat_key, position, event)
//        this.set_cursor_position(beat_key, position)
//    }
//
//    fun move_line(y_from: Int, y_to: Int) {
//        val (channel_from, line_from) = this.get_channel_index(y_from)
//        var (channel_to, line_to) = this.get_channel_index(y_to)
//
//        if (channel_to != channel_from) {
//            line_to += 1
//        }
//
//        this.move_line(channel_from, line_from, channel_to, line_to)
//    }
//
//    fun set_percussion_event_at_cursor() {
//        val beat_key = this.get_cursor().get_beatkey()
//        if (!this.is_percussion(beat_key.channel)) {
//            return
//        }
//
//        this.set_percussion_event(beat_key, this.get_cursor().get_position())
//    }
//
//
//    fun new_line_at_cursor() {
//        this.new_line(this.get_cursor().get_beatkey().channel)
//    }
//    fun remove_line_at_cursor() {
//        val beat_key = this.get_cursor().get_beatkey()
//        this.remove_line(beat_key.channel, beat_key.line_offset)
//    }
//
//    fun remove_beat_at_cursor() {
//        this.remove_beat(this.get_cursor().get_x())
//        //this.cursor_left()
//    }
//
//    fun split_tree_at_cursor(splits: Int = 2) {
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//        this.split_tree(beat_key, position, splits)
//        this.get_cursor().settle()
//    }
//
//    fun unset_at_cursor() {
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//        this.unset(beat_key, position)
//        this.get_cursor().settle()
//    }
//
//    fun remove_tree_at_cursor() {
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//
//        this.remove(beat_key, position)
//    }
//
//    fun insert_beat_at_cursor() {
//        this.insert_beat(this.get_cursor().get_x() + 1)
//    }
//
//    fun get_tree_at_cursor(): OpusTree<OpusEvent> {
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//        return this.get_tree(beat_key, position)
//    }
//
//    open fun clear_parent_at_cursor() {
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position().toMutableList()
//        if (position.isNotEmpty()) {
//            position.removeLast()
//        }
//        this.unset(beat_key, position)
//        this.set_cursor_position(beat_key, position)
//    }
//
//    fun increment_event_at_cursor() {
//        val tree = this.get_tree_at_cursor()
//        if (! tree.is_event()) {
//            return
//        }
//
//        val event = tree.get_event()!!
//        var new_value = event.note
//        if (event.relative) {
//            if ((event.note >= event.radix || event.note < 0 - event.radix) && event.note < (event.radix * (event.radix - 1))) {
//                new_value = event.note + event.radix
//            } else if (event.note < event.radix) {
//                new_value = event.note + 1
//            }
//        } else if (event.note < 127) {
//            new_value = event.note + 1
//        }
//        val new_event = OpusEvent(new_value, event.radix, event.channel, event.relative)
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//        this.set_event(beat_key, position, new_event)
//    }
//
//    fun decrement_event_at_cursor() {
//        val tree = this.get_tree_at_cursor()
//        if (! tree.is_event()) {
//            return
//        }
//
//        val event = tree.get_event()!!
//        var new_value = event.note
//        if (event.relative) {
//            if ((event.note > event.radix || event.note <= 0 - event.radix) && event.note > 0 - (event.radix * (event.radix - 1))) {
//                new_value = event.note - event.radix
//            } else if (event.note >= 0 - event.radix) {
//                new_value = event.note - 1
//            }
//        } else if (event.note > 0) {
//            new_value = event.note - 1
//        }
//
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//        val new_event = OpusEvent(new_value, event.radix, event.channel, event.relative)
//        this.set_event(beat_key, position, new_event)
//    }
//
//    fun overwrite_beat_at_cursor(beat_key: BeatKey) {
//        this.overwrite_beat(this.get_cursor().get_beatkey(), beat_key)
//    }
//
//    fun link_beat_at_cursor(beat_key: BeatKey) {
//        this.link_beats(this.get_cursor().get_beatkey(), beat_key)
//    }
//
//    fun unlink_beat_at_cursor() {
//        this.unlink_beat(this.get_cursor().get_beatkey())
//    }
//
//    fun set_event_at_cursor(event: OpusEvent) {
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//        this.set_event(beat_key, position, event)
//    }
//
//    fun insert_after_cursor() {
//        val beat_key = this.get_cursor().get_beatkey()
//        val position = this.get_cursor().get_position()
//        this.insert_after(beat_key, position)
//    }
//
//    fun convert_event_at_cursor_to_relative() {
//        val cursor = this.get_cursor()
//        this.convert_event_to_relative(cursor.get_beatkey(), cursor.get_position())
//    }
//
//    fun convert_event_at_cursor_to_absolute() {
//        val cursor = this.get_cursor()
//        this.convert_event_to_absolute(cursor.get_beatkey(), cursor.get_position())
//    }
//
//    fun has_preceding_absolute_event_at_cursor(): Boolean {
//        val cursor = this.get_cursor()
//        return this.has_preceding_absolute_event(cursor.get_beatkey(), cursor.get_position())
//    }
//
//    override fun apply_history_node(current_node: HistoryNode, depth: Int) {
//        super.apply_history_node(current_node, depth)
//    }
//}

