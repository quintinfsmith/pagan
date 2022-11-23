package radixulous.app.opusmanager
import radixulous.app.structure.OpusTree
import radixulous.app.opusmanager.BeatKey
import radixulous.app.opusmanager.OpusEvent
import radixulous.app.opusmanager.OpusManagerBase

class UpdatesCache {
    var beat_pop: MutableList<Int> = mutableListOf()
    var beat_new: MutableList<Int> = mutableListOf()
    var beat_change: MutableList<BeatKey> = mutableListOf()
    var line_new: MutableList<Pair<Int, Int>> = mutableListOf()
    var line_pop: MutableList<Pair<Int, Int>> = mutableListOf()
    var line_init: MutableList<Pair<Int, Int>> = mutableListOf()

    public fun flag_beat_pop(index: Int) {
        this.beat_pop.add(index)
    }

    public fun flag_beat_new(index: Int) {
        this.beat_new.add(index)
    }

    public fun flag_beat_change(beat_key: BeatKey) {
        this.beat_change.add(beat_key)
    }

    public fun flag_line_pop(channel: Int, line_offset: Int) {
        this.line_pop.add(Pair(channel, line_offset))
    }

    public fun flag_line_new(channel: Int, line_offset: Int) {
        this.line_new.add(Pair(channel, line_offset))
    }

    public fun flag_line_init(channel: Int, line_offset: Int) {
        this.line_init.add(Pair(channel, line_offset))
    }
}

open class FlagLayer() : OpusManagerBase() {
    var cache = UpdatesCache()

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
        var len_a = this.channel_trees[channel_a].size
        var len_b = this.channel_trees[channel_b].size
        for (i in 0 .. len_b - 1) {
            this.cache.flag_line_pop(channel_a, len_b - 1 - i)
        }

        for (i in 0 .. len_a - 1) {
            this.cache.flag_line_pop(channel_b, len_a - 1 - i)
        }

        for (i in 0 .. len_b - 1) {
            this.cache.flag_line_new(channel_b, i)
        }

        for (i in 0 .. len_a - 1) {
            this.cache.flag_line_new(channel_a, i)
        }
    }

    override fun new() {
        super.new()
        for (i in 0 .. this.opus_beat_count - 1) {
            this.cache.flag_beat_new(i)
        }
        for (i in 0 .. this.channel_trees.size - 1) {
            var channel = this.channel_trees[i]
            for (j in 0 .. channel.size - 1) {
                this.cache.flag_line_init(i, j)
            }
        }
    }
    override fun load(path: String) {
        super.load(path)
        for (i in 0 .. this.opus_beat_count - 1) {
            this.cache.flag_beat_new(i)
        }
        for (i in 0 .. this.channel_trees.size - 1) {
            var channel = this.channel_trees[i]
            for (j in 0 .. channel.size - 1) {
                this.cache.flag_line_init(i, j)
            }
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
        var rindex: Int
        if (index == null) {
            rindex = this.opus_beat_count - 1
        } else {
            rindex = index
        }

        super.insert_beat(index)

        this.cache.flag_beat_new(rindex)
    }

    override fun new_line(channel: Int, index: Int?) {
        super.new_line(channel, index)

        var line_index = if (index == null) {
            this.channel_trees[channel].size - 1
        } else {
            index
        }
        this.cache.flag_line_new(channel, line_index)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.cache.flag_beat_change(beat_key)
    }

    override fun remove_beat(index: Int?) {
        var rindex = if (index == null) {
            this.opus_beat_count - 1
        } else {
            index
        }
        super.remove_beat(index)
        this.cache.flag_beat_pop(rindex)
    }

    override fun remove_line(channel: Int, index: Int?) {
        super.remove_line(channel, index)

        if (index == null) {
            this.cache.flag_line_pop(channel, this.channel_trees[channel].size)
        } else {
            this.cache.flag_line_pop(channel, index)
        }
    }
}
