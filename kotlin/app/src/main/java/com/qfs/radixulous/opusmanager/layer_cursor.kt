package com.qfs.radixulous.opusmanager
import com.qfs.radixulous.apres.MIDI
import com.qfs.radixulous.structure.OpusTree
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
        if (this.opus_manager.opus_beat_count == 0) {
            // TODO: This'll problably bite me in the ass...
            return
        }

        this.y = max(0, min(this.y, this.opus_manager.line_count() - 1))
        this.x = max(0, min(this.x, this.opus_manager.opus_beat_count - 1))
        // First, get the beat
        var working_beat = this.opus_manager.get_beat_tree(this.get_beatkey())
        var working_tree = working_beat

        var working_position = mutableListOf<Int>()

        // Then get the current_working_tree
        var index = 0
        for (j in this.position) {
            if (working_tree.is_leaf()) {
                break
            }
            working_tree = if (working_tree.size <= j) {
                working_tree.get(working_tree.size - 1)
            } else {
                working_tree.get(j)
            }
            working_position.add(j)
        }

        // Then find the leaf if not already found
        while (! working_tree.is_leaf()) {
            working_tree = if (right_align) {
                working_position.add(working_tree.size - 1)
                working_tree.get(working_tree.size - 1)
            } else {
                working_position.add(0)
                working_tree.get(0)
            }
        }
        this.position = working_position
    }
}

open class CursorLayer() : HistoryLayer() {
    var cursor: Cursor? = null

    fun line_count(): Int {
        var output: Int = 0
        for (channel in this.channels) {
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
        if (!this.is_percussion(beat_key.channel)) {
            return
        }

        this.set_percussion_event(beat_key, this.get_cursor().get_position())
    }

    open fun set_cursor_position(beat_key: BeatKey, position: List<Int>){
        val cursor = this.get_cursor()
        if (cursor.get_beatkey() != beat_key || cursor.get_position() != position) {
            cursor.y = this.get_y(beat_key.channel, beat_key.line_offset)
            cursor.x = beat_key.beat
            cursor.position = position.toMutableList()
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.set_cursor_position(beat_key, position)
    }

    fun set_cursor_position(y: Int, x: Int, position: List<Int>){
        val cursor = this.get_cursor()
        if (cursor.y != y || cursor.x != x || cursor.get_position() != position) {
            this.get_cursor().y = y
            this.get_cursor().x = x
            this.get_cursor().position = position.toMutableList()
        }
    }

    fun new_line_at_cursor() {
        this.new_line(this.get_cursor().get_beatkey().channel)
    }

    override fun new_line(channel: Int, line_offset: Int?): List<OpusTree<OpusEvent>> {
        var cursor = this.get_cursor()
        var abs_line_offset = if (line_offset == null) {
            this.channels[channel].size - 1
        } else if (line_offset < 0) {
            this.channels[channel].size + line_offset
        } else {
            line_offset
        }
        var output = super.new_line(channel, line_offset)
        try {
            var beat_key = cursor.get_beatkey()
        } catch (e: Exception) {
            this.cursor_down()
        }
        this.cursor!!.settle()
        return output
    }

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        var original_line_count = this.line_count()
        super.move_line(channel_old, line_old, channel_new, line_new)
        if (this.line_count() == original_line_count - 1 && this.cursor?.y == original_line_count - 1) {
            this.cursor_up()
        }
    }

    fun remove_line_at_cursor() {
        var beat_key = this.get_cursor().get_beatkey()
        this.remove_line(beat_key.channel, beat_key.line_offset)
    }

    fun remove_beat_at_cursor() {
        this.remove_beat(this.get_cursor().get_x())
        //this.cursor_left()
    }

    fun split_tree_at_cursor(splits: Int = 2) {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        this.split_tree(beat_key, position, splits)
        this.get_cursor().settle()
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

        this.remove(beat_key, position)
    }

    fun insert_beat_at_cursor() {
        this.insert_beat(this.get_cursor().get_x() + 1)
    }

    fun get_tree_at_cursor(): OpusTree<OpusEvent> {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position()
        return this.get_tree(beat_key, position)
    }

    open fun clear_parent_at_cursor() {
        var beat_key = this.get_cursor().get_beatkey()
        var position = this.get_cursor().get_position().toMutableList()
        if (position.isNotEmpty()) {
            position.removeLast()
        }
        this.unset(beat_key, position)
        this.set_cursor_position(beat_key, position)
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
        for (channel in 0 until this.channels.size) {
            for (i in 0 until this.channels[channel].size) {
                if (counter == y) {
                    return Pair(channel, i)
                }
                counter += 1
            }
        }
        throw Exception("IndexError $y / $counter")
    }

    fun get_y(channel: Int, rel_line_offset: Int): Int {
        val line_offset = if (rel_line_offset < 0) {
            this.channels[channel].size + rel_line_offset
        } else {
            rel_line_offset
        }

        var y: Int = 0
        for (i in 0 until this.channels.size) {
            for (j in 0 until this.channels[i].size) {
                if (channel == i && line_offset == j) {
                    return y
                }
                y += 1
            }
        }

        return -1
    }

    ///////// OpusManagerBase methods
    override fun insert_beat(beat_index: Int) {
        super.insert_beat(beat_index)
        this.get_cursor().settle()
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)
        this.get_cursor().settle()
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {

        super.remove(beat_key, position)

        var cursor = this.get_cursor()
        if (position.isNotEmpty() && cursor.get_beatkey() == beat_key && cursor.get_position() == position) {
            var parent_tree = this.get_tree(
                beat_key,
                position.subList(0, position.size - 1)
            )
            if (cursor.position.last() ==  parent_tree.size) {
                cursor.position[cursor.position.size - 1] = cursor.position.last() - 1
            }
        }

        this.get_cursor().settle()
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.get_cursor().settle()
    }

    override fun remove_line(channel: Int, line_offset: Int): MutableList<OpusTree<OpusEvent>> {
        val output = super.remove_line(channel, line_offset)

        if (channel > this.channels.size - 1) {
            this.cursor_up()
        } else if (channel == this.channels.size - 1 && line_offset >= this.channels[channel].size) {
             this.cursor_up()
        }

        return output
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        var cursor = this.get_cursor()
        try {
            cursor.get_beatkey()
        } catch (e: Exception) {
            this.cursor_up()
        }
        this.get_cursor().settle()
    }

    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        super.overwrite_beat(old_beat, new_beat)
        this.get_cursor().settle()
    }

    override fun new() {
        this.get_cursor().set(0,0, listOf(0))
        super.new()
        this.get_cursor().settle()
    }

    override fun load(path: String) {
        this.get_cursor().set(0,0, listOf(0))
        super.load(path)
        this.get_cursor().settle()
    }

    override fun import_midi(midi: MIDI) {
        this.get_cursor().set(0,0, listOf(0))
        super.import_midi(midi)
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

        var (y_diff, y_i) = if (cursor_diff.first >= 0) {
            Pair(
                cursor_diff.first,
                this.get_y(target_a.channel, target_a.line_offset)
            )
        } else {
            Pair(
                0 - cursor_diff.first,
                this.get_y(target_b.channel, target_b.line_offset)
            )
        }

        var (x_diff, x_i) = if (cursor_diff.second >= 0) {
            Pair(
                cursor_diff.second,
                target_a.beat
            )
        } else {
            Pair(
                0 - cursor_diff.second,
                target_b.beat
            )
        }
        var y_new = this.get_y(beat.channel, beat.line_offset)
        if (cursor_diff.first < 0) {
            y_new -= y_diff
        }
        var x_new = beat.beat
        if (cursor_diff.second < 0) {
            x_new -= x_diff
        }


        if (y_new in (y_i .. y_diff + y_i) && x_new in (x_i .. x_diff + x_i)) {
            throw Exception("Can't link a beat to its self")
        }
        if (y_i in (y_new .. y_diff + y_new) && x_i in (x_new .. x_diff + x_new)) {
            throw Exception("Can't link a beat to its self")
        }

        var new_pairs = mutableListOf<Pair<BeatKey, BeatKey>>()
        for (y in 0 .. y_diff) {
            var pair = this.get_channel_index(y + y_new)
            var target_pair = this.get_channel_index(y + y_i)
            for (x in 0 .. x_diff) {
                var working_position = BeatKey(
                    pair.first,
                    pair.second,
                    x + x_new
                )

                var working_target = BeatKey(
                    target_pair.first,
                    target_pair.second,
                    x + x_i
                )
                new_pairs.add(Pair(working_position, working_target))
            }
        }
        this.batch_link_beats(new_pairs)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        super.set_event(beat_key, position, event)
        this.set_cursor_position(beat_key, position)
    }

    //-- History Layer --//
    override fun apply_undo() {
        super.apply_undo()
        this.cursor?.settle()
    }
    // End History Layer //

    fun move_line(y_from: Int, y_to: Int) {
        val (channel_from, line_from) = this.get_channel_index(y_from)
        var (channel_to, line_to) = this.get_channel_index(y_to)

        if (channel_to != channel_from) {
            line_to += 1
        }

        this.move_line(channel_from, line_from, channel_to, line_to)
    }
}
