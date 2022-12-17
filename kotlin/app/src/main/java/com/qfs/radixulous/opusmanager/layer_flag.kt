package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.structure.OpusTree

class UpdatesCache {
    private var beat_flag: MutableList<Pair<Int, Int>> = mutableListOf()
    private var beat_change: MutableList<BeatKey> = mutableListOf()
    private var line_flag: MutableList<Triple<Int, Int, Int>> = mutableListOf()

    fun dequeue_line(): Triple<Int, Int, Int>? {
        return if (this.line_flag.isEmpty()) {
            null
        } else {
            this.line_flag.removeFirst()
        }
    }
    fun dequeue_beat(): Pair<Int, Int>? {
        return if (this.beat_flag.isEmpty()) {
            null
        } else {
            this.beat_flag.removeFirst()
        }
    }
    fun dequeue_change(): BeatKey? {
        return if (this.beat_change.isEmpty()) {
            null
        } else {
            this.beat_change.removeFirst()
        }
    }

    fun flag_beat_pop(index: Int) {
        this.beat_flag.add(Pair(index, 0))
    }
    fun flag_beat_new(index: Int) {
        this.beat_flag.add(Pair(index, 1))
    }
    fun flag_beat_change(beat_key: BeatKey) {
        this.beat_change.add(beat_key)
    }
    fun flag_line_pop(channel: Int, line_offset: Int) {
        this.line_flag.add(Triple(channel, line_offset, 0))
    }
    fun flag_line_new(channel: Int, line_offset: Int) {
        this.line_flag.add(Triple(channel, line_offset, 1))
    }
    fun flag_line_init(channel: Int, line_offset: Int) {
        this.line_flag.add(Triple(channel, line_offset, 2))
    }
    fun purge() {
        this.beat_flag.clear()
        this.beat_change.clear()
        this.line_flag.clear()
    }
}

open class FlagLayer : OpusManagerBase() {
    private var cache = UpdatesCache()

    override fun reset() {
        this.cache.purge()
        super.reset()
    }

    fun fetch_flag_line(): Triple<Int, Int, Int>? {
        return this.cache.dequeue_line()
    }

    fun fetch_flag_beat(): Pair<Int, Int>? {
        return this.cache.dequeue_beat()
    }

    fun fetch_flag_change(): BeatKey? {
        return this.cache.dequeue_change()
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.cache.flag_beat_change(beat_key)
    }

    override fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        super.overwrite_beat(old_beat, new_beat)
        this.cache.flag_beat_change(old_beat)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)
        this.cache.flag_beat_change(beat_key)
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.cache.flag_beat_change(beat_key)
    }

    override fun swap_channels(channel_a: Int, channel_b: Int) {
        super.swap_channels(channel_a, channel_b)
        val len_a = this.channel_lines[channel_a].size
        val len_b = this.channel_lines[channel_b].size
        for (i in 0 until len_b) {
            this.cache.flag_line_pop(channel_a, len_b - 1 - i)
        }

        for (i in 0 until len_a) {
            this.cache.flag_line_pop(channel_b, len_a - 1 - i)
        }

        for (i in 0 until len_b) {
            this.cache.flag_line_new(channel_b, i)
        }

        for (i in 0 until len_a) {
            this.cache.flag_line_new(channel_a, i)
        }
    }

    override fun new() {
        super.new()
        for (i in 0 until this.channel_lines.size) {
            for (j in 0 until this.channel_lines[i].size) {
                this.cache.flag_line_init(i, j)
            }
        }
        for (x in 0 until this.opus_beat_count) {
            this.cache.flag_beat_new(x)
        }
    }

    override fun load(path: String) {
        super.load(path)
        for (i in 0 until this.channel_lines.size) {
            for (j in 0 until this.channel_lines[i].size) {
                this.cache.flag_line_init(i, j)
            }
        }
        for (x in 0 until this.opus_beat_count) {
            this.cache.flag_beat_new(x)
        }
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        super.set_event(beat_key, position, event)
        this.cache.flag_beat_change(beat_key)
    }

    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        Log.e("AAA", "$beat_key, $position")
        super.set_percussion_event(beat_key, position)
        this.cache.flag_beat_change(beat_key)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this.cache.flag_beat_change(beat_key)
    }

    override fun insert_beat(index: Int?) {
        val orig_index: Int = index ?: (this.opus_beat_count - 1)

        super.insert_beat(index)

        this.cache.flag_beat_new(orig_index)
    }

    override fun new_line(channel: Int, index: Int?) {
        super.new_line(channel, index)

        val line_index = index ?: (this.channel_lines[channel].size - 1)
        this.cache.flag_line_new(channel, line_index)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.cache.flag_beat_change(beat_key)
    }

    override fun remove_beat(rel_beat_index: Int?) {
        val orig_index = rel_beat_index ?: (this.opus_beat_count - 1)
        super.remove_beat(rel_beat_index)
        this.cache.flag_beat_pop(orig_index)
    }

    override fun remove_line(channel: Int, index: Int?) {
        super.remove_line(channel, index)

        this.cache.flag_line_pop(channel, index ?: this.channel_lines[channel].size)
    }
}
