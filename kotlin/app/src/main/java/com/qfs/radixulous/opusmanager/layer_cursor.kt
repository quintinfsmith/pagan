package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.opusmanager.BeatKey
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.HistoryLayer
import kotlin.math.min
import kotlin.math.max

class Cursor(var opus_manager: CursorLayer) {
    var y: Int = 0
    var x: Int = 0
    var position: MutableList<Int> = mutableListOf()

    fun get_y(): Int {
        return this.y
    }
    fun set_y(new_y: Int) {
        this.y = new_y
    }
    fun get_x(): Int {
        return this.x
    }
    fun set_x(new_x: Int) {
        this.x = new_x
    }

    fun set(y: Int, x: Int, position: List<Int>) {
        this.y = y
        this.x = x
        if (position.isNotEmpty()) {
            this.position = position.toMutableList()
        } else {
            this.position = mutableListOf(0)
        }
    }
    fun set_by_beatkey_position(beat_key: BeatKey, position: List<Int>) {
        this.y = this.opus_manager.get_y(beat_key.channel, beat_key.line_offset)
        this.x = beat_key.beat
        this.position = position.toMutableList()
    }
    fun get_beatkey(): BeatKey {
        var channel_index = this.get_channel_index()
        return BeatKey(
            channel_index.first,
            channel_index.second,
            this.x
        )
    }

    fun get_channel_index(): Pair<Int, Int> {
        return this.opus_manager.get_channel_index(this.y)
    }

    fun move_left() {
        var working_tree = this.opus_manager.get_beat_tree(this.get_beatkey())
        for (i in this.position) {
            working_tree = working_tree.get(i)
        }

        while (this.position.isNotEmpty()) {
            if (this.position.last() == 0) {
                if (working_tree.parent != null) {
                    working_tree = working_tree.parent!!
                } else {
                    break
                }
                this.position.removeAt(this.position.size - 1)
            } else {
                this.position[this.position.size - 1] -= 1
                break
            }
        }

        if (this.x > 0 && this.position.isEmpty()) {
            this.x -= 1
            this.settle(true)
        } else {
            this.settle()
        }
    }

    fun move_right() {
        var working_tree = this.opus_manager.get_beat_tree(this.get_beatkey())
        for (i in this.position) {
            working_tree = working_tree.get(i)
        }

        while (this.position.isNotEmpty()) {
            if (working_tree.parent!!.size - 1 == this.position.last()) {
                this.position.removeAt(this.position.size - 1)
                working_tree = working_tree.parent!!
            } else if (working_tree.parent!!.size - 1 > this.position.last()) {
                this.position[this.position.size - 1] += 1
                break
            }
        }

        if (this.x < this.opus_manager.opus_beat_count - 1 && this.position.isEmpty()) {
            this.x += 1
            this.settle()
        } else {
            this.settle(true)
        }
    }

    fun move_up() {
        if (this.y > 0) {
            this.y -= 1
        }
        this.settle()
    }

    fun move_down() {
        var line_count = this.opus_manager.line_count()
        if (this.y < line_count - 1) {
            this.y += 1
        }
        this.settle()
    }

    fun get_position(): List<Int> {
        return this.position.toList()
    }

    fun settle(right_align: Boolean = false) {
        this.y = max(0, min(this.y, this.opus_manager.line_count() - 1))
        this.x = max(0, min(this.x, this.opus_manager.opus_beat_count - 1))

        // First, get the beat
        var working_beat = this.opus_manager.get_beat_tree(this.get_beatkey())
        var working_tree = working_beat

        if (working_tree.is_leaf()) {
            this.position = mutableListOf()
        }

        // Then get the current_working_tree
        var index = 0
        for (j in this.position) {
            if (working_tree.is_leaf() || working_tree.size <= j) {
                break
            }
            working_tree = working_tree.get(j)
            index += 1
        }

        while (index < this.position.size) {
            this.position.removeLast()
        }

        // Then find the leaf if not already found
        while (! working_tree.is_leaf()) {
            working_tree = if (right_align) {
                this.position.add(working_tree.size - 1)
                working_tree.get(working_tree.size - 1)
            } else {
                this.position.add(0)
                working_tree.get(0)
            }
        }
    }
}

open class CursorLayer() : FlagLayer() {
    var cursor: Cursor? = null
    var channel_order = Array(16) { i -> i }

    override fun reset() {
        this.cursor = Cursor(this)
        this.channel_order = Array(16) { i -> i }
        super.reset()
    }

    fun line_count(): Int {
        var output: Int = 0
        for (channel in this.channel_lines) {
            output += channel.size
        }
        return output
    }

    fun get_cursor(): Cursor {
        if (this.cursor == null) {
            this.cursor = Cursor(this)
        }

        return this.cursor!!
    }

    fun cursor_right() {
        this.get_cursor().move_right()
    }
    fun cursor_left() {
        this.get_cursor().move_left()
    }
    fun cursor_up() {
        this.get_cursor().move_up()
    }
    fun cursor_down() {
        this.get_cursor().move_down()
    }

    fun set_percussion_event_at_cursor() {
        var beat_key = this.get_cursor().get_beatkey()
        if (beat_key.channel != 9) {
            return
        }

        this.set_percussion_event(beat_key, this.get_cursor().get_position())
    }

    fun set_cursor_position(y: Int, x: Int, position: List<Int>){
        this.get_cursor().y = y
        this.get_cursor().x = x
        this.get_cursor().position = position.toMutableList()
    }

    fun new_line_at_cursor() {
        this.new_line(this.get_cursor().get_beatkey().channel)
    }

    override fun new_line(channel: Int, index: Int?) {
        var cursor = this.get_cursor()
        var abs_index = if (index == null) {
            this.channel_lines[channel].size - 1
        } else if (index < 0) {
            this.channel_lines[channel].size + index
        } else {
            index
        }
        super.new_line(channel, index)

        var beat_key = cursor.get_beatkey()
        if (channel < beat_key.channel || (channel == beat_key.channel && abs_index < beat_key.line_offset)) {
            this.cursor_down()
        }
    }

    fun remove_line_at_cursor() {
        var cursor_y = this.get_cursor().get_y()
        var beat_key = this.get_cursor().get_beatkey()
        this.remove_line(beat_key.channel, beat_key.line_offset)

    }

    fun remove_beat_at_cursor() {
        this.remove_beat(this.get_cursor().get_x())
        //this.cursor_left()
    }

    fun split_tree_at_cursor() {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        this.split_tree(beat_key, position, 2)
    }

    fun unset_at_cursor() {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        this.unset(beat_key, position)
        this.get_cursor().settle()
    }

    fun remove_tree_at_cursor() {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        var removed_parent = this.get_tree(beat_key, position).parent

        this.remove(beat_key, position)

        if (removed_parent != null && removed_parent.size > 1) {
            if (position.isNotEmpty() && position.last() > 0)  {
                this.get_cursor().position[position.size - 1] = position.last() - 1
            }
        }

        this.get_cursor().settle()
    }

    fun insert_beat_at_cursor() {
        this.insert_beat(this.get_cursor().get_x() + 1)
    }

    fun get_tree_at_cursor(): OpusTree<OpusEvent> {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        return this.get_tree(beat_key, position)
    }

    fun increment_event_at_cursor() {
        var tree = this.get_tree_at_cursor()
        if (! tree.is_event()) {
            return
        }

        var event = tree.get_event()!!
        var new_value = event.note
        if (event.relative) {
            if ((event.note >= event.radix || event.note < 0 - event.radix) && event.note < (event.radix * (event.radix - 1))) {
                new_value = event.note + event.radix
            } else if (event.note < event.radix) {
                new_value = event.note + 1
            }
        } else if (event.note < 127) {
            new_value = event.note + 1
        }
        var new_event = OpusEvent(new_value, event.radix, event.channel, event.relative)
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        this.set_event(beat_key, position, new_event)
    }

    fun decrement_event_at_cursor() {
        var tree = this.get_tree_at_cursor()
        if (! tree.is_event()) {
            return
        }

        var event = tree.get_event()!!
        var new_value = event.note
        if (event.relative) {
            if ((event.note > event.radix || event.note <= 0 - event.radix) && event.note > 0 - (event.radix * (event.radix - 1))) {
                new_value = event.note - event.radix
            } else if (event.note >= 0 - event.radix) {
                new_value = event.note - 1
            }
        } else if (event.note > 0) {
            new_value = event.note - 1
        }

        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        var new_event = OpusEvent(new_value, event.radix, event.channel, event.relative)
        this.set_event(beat_key, position, new_event)
    }

    fun jump_to_beat(beat: Int) {
        this.get_cursor().set_x(beat)
        this.get_cursor().settle()
    }

    fun overwrite_beat_at_cursor(beat_key: BeatKey) {
        this.overwrite_beat(this.get_cursor().get_beatkey(), beat_key)
    }

    fun link_beat_at_cursor(beat_key: BeatKey) {
        this.link_beats(this.get_cursor().get_beatkey(), beat_key)
    }

    fun unlink_beat_at_cursor() {
        this.unlink_beat(this.get_cursor().get_beatkey())
    }

    fun set_event_at_cursor(event: OpusEvent) {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        this.set_event(beat_key, position, event)
    }

    fun insert_after_cursor() {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        this.insert_after(beat_key, position)
    }

    fun get_channel_index(y: Int): Pair<Int, Int> {
        var counter = 0
        for (channel in this.channel_order) {
            for (i in 0 until this.channel_lines[channel].size) {
                if (counter == y) {
                    return Pair(channel, i)
                }
                counter += 1
            }
        }
        throw Exception("IndexError")
    }

    fun get_y(channel: Int, rel_line_offset: Int): Int {
        val line_offset = if (rel_line_offset < 0) {
            this.channel_lines[channel].size + rel_line_offset
        } else {
            rel_line_offset
        }

        var y: Int = 0
        for (i in this.channel_order) {
            for (j in 0 .. this.channel_lines[i].size - 1) {
                if (channel == i && line_offset == j) {
                    return y
                }
                y += 1
            }
        }

        return -1
    }

    ///////// OpusManagerBase methods
    override fun insert_beat(index: Int?) {
        super.insert_beat(index)
        this.get_cursor().settle()
    }

    override fun remove_beat(index: Int?) {
        super.remove_beat(index)
        this.get_cursor().settle()
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.get_cursor().settle()
    }

    override fun swap_channels(channel_a: Int, channel_b: Int) {
        var original_beatkey = this.get_cursor().get_beatkey()
        super.swap_channels(channel_a, channel_b)

        var new_y = this.get_y(original_beatkey.channel, 0)
        new_y += min(original_beatkey.line_offset, this.channel_lines[original_beatkey.channel].size - 1)

        this.get_cursor().set_y(new_y)
        this.get_cursor().settle()
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.get_cursor().position.add(0)
    }

    override fun remove_line(channel: Int, index: Int?) {
        var cursor = this.get_cursor()
        var beat_key = cursor.get_beatkey()
        var abs_index = if (index == null) {
            this.channel_lines[channel].size - 1
        } else if (index < 0) {
            this.channel_lines[channel].size + index
        } else {
            index
        }

        super.remove_line(channel, index)

        if (channel < beat_key.channel || (channel == beat_key.channel && abs_index <= beat_key.line_offset)) {
            this.cursor_up()
        }
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.get_cursor().settle()
    }

    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        super.overwrite_beat(old_beat, new_beat)
        this.get_cursor().settle()
    }

    override fun new() {
        super.new()
        this.get_cursor().set(0,0, listOf(0))
        this.get_cursor().settle()
    }

    override fun load(path: String) {
        super.load(path)
        this.get_cursor().set(0,0, listOf(0))
        this.get_cursor().settle()
    }

    fun convert_event_at_cursor_to_relative() {
        var cursor = this.get_cursor()
        this.convert_event_to_relative(cursor.get_beatkey(), cursor.get_position())
    }

    fun convert_event_at_cursor_to_absolute() {
        var cursor = this.get_cursor()
        this.convert_event_to_absolute(cursor.get_beatkey(), cursor.get_position())
    }

    fun has_preceding_absolute_event_at_cursor(): Boolean {
        var cursor = this.get_cursor()
        return this.has_preceding_absolute_event(cursor.get_beatkey(), cursor.get_position())
    }

    // NOTE: This *has* to be in the CursorLayer. a beatkey range assumes
    open fun get_cursor_difference(beata: BeatKey, beatb: BeatKey): Pair<Int, Int> {
        var beata_y = this.get_y(beata.channel, beata.line_offset)
        var beatb_y = this.get_y(beatb.channel, beatb.line_offset)

        return Pair(beatb_y - beata_y, beatb.beat - beata.beat)
    }

    open fun link_beat_range(beat: BeatKey, target_a: BeatKey, target_b: BeatKey) {
        var cursor_diff = this.get_cursor_difference(target_a, target_b)

        for (y in 0 .. cursor_diff.first) {
            var pair = this.get_channel_index(y + this.get_y(beat.channel, beat.line_offset))
            var target_pair = this.get_channel_index(
                y + this.get_y(
                    target_a.channel,
                    target_a.line_offset
                )
            )
            for (x in 0 .. cursor_diff.second) {
                var working_position = BeatKey(
                    pair.first,
                    pair.second,
                    x + beat.beat
                )

                var working_target = BeatKey(
                    target_pair.first,
                    target_pair.second,
                    x + target_a.beat
                )

                this.link_beats(working_position, working_target)
            }
        }
    }
}
