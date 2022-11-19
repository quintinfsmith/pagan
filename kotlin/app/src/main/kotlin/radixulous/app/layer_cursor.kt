package radixulous.app.opusmanager
import radixulous.app.structure.OpusTree
import radixulous.app.opusmanager.BeatKey
import radixulous.app.opusmanager.OpusEvent
import radixulous.app.opusmanager.HistoryLayer
import kotlin.math.min

class Cursor {
    var y: Int = 0
    var x: Int = 0
    var position: MutableList<Int> = mutableListOf()
    constructor(var opus_manager: CursorLayer)

    fun set(y: Int, x: Int, position: List<Int>) {
        this.y = y
        this.x = x
        if (position.size > 0) {
            this.position = position.toMutableList()
        } else {
            this.position = mutableListFrom(0)
        }
    }

    fun get_beatkey(): BeatKey {
    }

    fun move_left() {
        var working_tree = this.opus_manager.get_beat_tree(this.get_beatkey())
    }
    fun move_right() {}
    fun move_up() {}
    fun move_down() {}
}

class CursorLayer: HistoryLayer {
    var cursor = Cursor(this)
    var channel_order = Array(16, {i -> i})
    fun line_count(): Int {
        var output: Int = 0
        for (channel in self.channel_trees) {
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

    fun set_percurssion_event_at_cursor {
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
        this.cursor.move_left()
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
        this.remove(beat_key, position)
        this.cursor.settle()
    }

    fun insert_beat_at_cursor() {
        this.insert_beat(this.cursor.get_x() + 1)
        this.cursor.settle()
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
            if ((event.note >= event.base || event.note < 0 - event.base) && event.note < (event.base * (event.base - 1))) {
                new_value = event.note + event.base
            } else if (event.note < event.base) {
                new_value = event.note + 1
            }
        } else if (event.note < 127) {
            new_value = event.note + 1
        }

        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
        this.set_event(beat_key, position, new_value, event.relative)
    }

    fun decrement_event_at_cursor() {
        var tree = this.get_tree_at_cursor()
        if (! tree.is_event()) {
            return
        }

        var event = tree.get_event()!!
        var new_value = event.note
        if (event.relative) {
            if ((event.note > event.base || event.note <= 0 - event.base) && event.note > 0 - (event.base * (event.base - 1))) {
                new_value = event.note - event.base
            } else if (event.note >= 0 - event.base) {
                new_value = event.note - 1
            }
        } else if (event.note > 0) {
            new_value = event.note - 1
        }

        var beat_key = this.cursor.get_beatkey()
        var position = this.cursor.get_position()
        this.set_event(beat_key, position, new_value, event.relative)
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

    fun get_channel_index(y: Int): Pair(Int, Int) {
        var counter = 0
        for (channel in this.channel_order) {
            for (i in 0 .. this.channel_trees[channel].size) {
                if (counter == y) {
                    return Pair(channel, i)
                }
                counter += 1
            }
        }
        throw Exception("IndexError")
    }

    fun get_y(channel: Int, rel_line_offset: Int) {
        var line_offset
        if (rel_line_offset < 0) {
            line_offset = this.channel_trees[channel].size + rel_line_offset
        } else {
            line_offset = rel_line_offset
        }

        var y = 0
        for i in this.channel_order {
            for (j in 0 .. this.channel_trees[i].size) {
                if (channel == i && line_offset == j) {
                    return y
                }
                y += 1
            }
        }

        return -1
    }

    ///////// OpusManagerBase methods

    open override fun insert_beat(index: Int?) {
        super.insert_beat(index)
        this.cursor.settle()
    }

    open override fun remove_beat(index: Int) {
        super.remove_beat(index)
        this.cursor.settle()
    }

    open override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove_beat(beat_key, position)
        this.cursor.settle()
    }

    open override fun swap_channels(channel_a: Intt, channel_b: Int) {
        var original_beatkey = this.cursor.get_beatkey()
        super.swap_channels(channel_a, channel_b)

        var new_y = this.get_y(original_beatkey.channel)
        ney_y += min(original_beatkey.line_offset, this.channel_trees[original_beatkey.channel].size - 1)

        this.cursor.set_y(new_y)
        this.cursor.settle()
    }

    open override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.cursor.settle()
    }

    open override fun remove_line(channel: Int, index: Int?) {
        super.remove_line(channel, index)
        this.cursor.settle()
    }
    open override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.cursor.settle()
    }
    open override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        this.overwrite_beat(old_beat, new_beat)
        this.cursor.settle()
    }
    open override fun _new() {
        super._new()
        this.cursor.set(0,0,[0])
        this.cursor.settle()
    }

    open override fun _load(path: String) {
        super._load(path)
        this.cursor.settle()
    }

    /////// HistoryLayer Methods
    open override fun append_undoer() {}
    open override fun close_multi() {
        super.close_multi()
        if (! this.history_locked || this.multi_counter > 0) {
            this.history_ledger.get(-1).add(
            )
        }
    }
}
