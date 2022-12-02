package com.qfs.radixulous.opusmanager
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

        while (! this.position.isEmpty()) {
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

open class CursorLayer() : LinksLayer() {
    var cursor = Cursor(this)
    var channel_order = Array(16, { i -> i })
    fun line_count(): Int {
        var output: Int = 0
        for (channel in this.channel_lines) {
            output += channel.size
        }
        return output
    }

    fun cursor_right() {
        this.cursor.move_right()
    }
    fun cursor_left() {
        this.cursor.move_left()
    }
    fun cursor_up() {
        this.cursor.move_up()
    }
    fun cursor_down() {
        this.cursor.move_down()
    }

    fun set_percurssion_event_at_cursor() {
        var beat_key = this.cursor.get_beatkey()
        if (beat_key.channel != 9) {
            return
        }

        this.set_percussion_event(beat_key, this.cursor.get_position())
    }

    fun new_line_at_cursor() {
        this.new_line(this.cursor.get_beatkey().channel)
    }

    fun remove_line_at_cursor() {
        var cursor_y = this.cursor.get_y()
        if (cursor_y ==  this.line_count() - 1) {
            this.cursor.set_y(cursor_y - 1)
        }
        var beat_key = this.cursor.get_beatkey()
        this.remove_line(beat_key.channel, beat_key.line_offset)
    }

    fun remove_beat_at_cursor() {
        this.remove_beat(this.cursor.get_x())
        //this.cursor_left()
    }

    fun split_tree_at_cursor() {
        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
        this.split_tree(beat_key, position, 2)
        this.cursor.settle()
    }

    fun unset_at_cursor() {
        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
        this.unset(beat_key, position)
    }

    fun remove_tree_at_cursor() {
        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
        var removed_parent = this.get_tree(beat_key, position).parent

        this.remove(beat_key, position)

        if (removed_parent != null && removed_parent.size > 1) {
            if (position.isNotEmpty() && position.last() > 0)  {
                this.cursor.position[position.size - 1] = position.last() - 1
            }
        }

        this.cursor.settle()
    }

    fun insert_beat_at_cursor() {
        this.insert_beat(this.cursor.get_x() + 1)
    }

    fun get_tree_at_cursor(): OpusTree<OpusEvent> {
        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
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
        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
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

        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
        var new_event = OpusEvent(new_value, event.radix, event.channel, event.relative)
        this.set_event(beat_key, position, new_event)
    }

    fun jump_to_beat(beat: Int) {
        this.cursor.set_x(beat)
    }

    fun overwrite_beat_at_cursor(beat_key: BeatKey) {
        this.overwrite_beat(this.cursor.get_beatkey(), beat_key)
    }

    fun link_beat_at_cursor(beat_key: BeatKey) {
        this.link_beats(this.cursor.get_beatkey(), beat_key)
    }

    fun unlink_beat_at_cursor() {
        this.unlink_beat(this.cursor.get_beatkey())
    }

    fun insert_after_cursor() {
        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
        this.insert_after(beat_key, position)
    }

    fun get_channel_index(y: Int): Pair<Int, Int> {
        var counter = 0
        for (channel in this.channel_order) {
            for (i in 0 .. this.channel_lines[channel].size - 1) {
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
        this.cursor.settle()
    }

    override fun remove_beat(index: Int?) {
        super.remove_beat(index)
        this.cursor.settle()
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.cursor.settle()
    }

    override fun swap_channels(channel_a: Int, channel_b: Int) {
        var original_beatkey = this.cursor.get_beatkey()
        super.swap_channels(channel_a, channel_b)

        var new_y = this.get_y(original_beatkey.channel, 0)
        new_y += min(original_beatkey.line_offset, this.channel_lines[original_beatkey.channel].size - 1)

        this.cursor.set_y(new_y)
        this.cursor.settle()
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.cursor.settle()
    }

    override fun remove_line(channel: Int, index: Int?) {
        super.remove_line(channel, index)
        this.cursor.settle()
    }
    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.cursor.settle()
    }
    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.overwrite_beat(old_beat, new_beat)
        this.cursor.settle()
    }
    override fun new() {
        super.new()
        this.cursor.set(0,0, listOf(0))
        this.cursor.settle()
    }

    override fun load(path: String) {
        super.load(path)
        this.cursor.settle()
    }
}
