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

open class FlagLayer : LinksLayer() {
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

    fun flag_beat_change(beat_key: BeatKey) {
        this.cache.flag_beat_change(beat_key)
    }

    override fun remove_channel(channel: Int) {
        for (i in 0 until this.channels[channel].size) {
            this.cache.flag_line_pop(channel, 0)
        }
        super.remove_channel(channel)
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

    override fun new() {
        super.new()
        this.reflag()
    }

    override fun load(path: String) {
        super.load(path)
        this.reflag()
    }

    fun reflag() {
        this.cache.purge()
        for (i in 0 until this.channels.size) {
            for (j in 0 until this.channels[i].size) {
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

    override fun new_line(channel: Int, index: Int?): List<OpusTree<OpusEvent>> {
        var output = super.new_line(channel, index)

        val line_index = index ?: (this.channels[channel].size - 1)
        this.cache.flag_line_new(channel, line_index)

        for (i in 0 until this.opus_beat_count)     {
            this.cache.flag_beat_change(BeatKey(channel, line_index, i))
        }

        return output
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.cache.flag_beat_change(beat_key)
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)
        this.cache.flag_beat_pop(beat_index)
    }

    override fun remove_line(channel: Int, index: Int) {
        super.remove_line(channel, index)
        this.cache.flag_line_pop(channel, index)
    }

    //override fun link_beats(beat_key: BeatKey, target: BeatKey) {
    //    super.link_beats(beat_key, target)
    //    this.cache.flag_beat_change(beat_key)
    //    this.cache.flag_beat_change(target)
    //}

    override fun unlink_beat(beat_key: BeatKey) {
        this.cache.flag_beat_change(beat_key)
        var target_key = this.linked_beat_map.get(beat_key)
        if (target_key != null) {
            this.cache.flag_beat_change(target_key!!)
        }
        super.unlink_beat(beat_key)
    }
}
