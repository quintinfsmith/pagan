package radixulous.app.opusmanager
import radixulous.app.structure.OpusTree
import radixulous.app.layer_base.BeatKey
import radixulous.app.layer_base.OpusEvent
import radixulous.app.layer_base.OpusManagerBase

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

class FlagLayer: OpusManagerBase {
    constructor {
        super()
        var cache = UpdatesCache()
    }

    public fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.cache.flag_beat_change(beat_key)
    }

    public fun overwrite_beat(old_beat: BeatKey, new_beat: BeatKey) {
        super.overwrite_beat(old_beat, new_beat)
        this.cache.flag_beat_change(old_beat)
    }
    public fun unlink_beat(beat_key, BeatKey) {
        super.unlink_beat(beat_key)
        this.cache.flag_beat_change(beat_key)
    }

    public fun insert_after(beat_key: BeatKey, position: List<Int>) {
        super.insert_after(beat_key, position)
        this.cache.flag_beat_change(beat_key)
    }

    public fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        super.split_tree(beat_key, position, splits)
        this.cache.flag_beat_change(beat_key)
    }

    public fun swap_channels(channel_a: Int, channel_b: Int) {
        super.swap_channels(channel_a, channel_b)
        var len_a = this.channel_trees[channel_a].size
        var len_b = this.channel_trees[channel_b].size
        for (i in 0 .. len_b) {
            this.cache.flag_line_pop(channel_a, len_b - 1 - i)
        }

        for (i in 0 .. len_a) {
            this.cache.flag_line_pop(channel_b, len_a - 1 - i)
        }

        for (i in 0 .. len_b) {
            this.cache.flag_line_new(channel_b, i)
        }

        for (i in 0 .. len_a) {
            this.cache.flag_line_new(channel_a, i)
        }
    }

    fun _new() {
        super._new()
        for (i in 0 .. this.opus_beat_count) {
            this.cache.flag_beat_new(i)
        }
        for (i in 0 .. this.channel_trees.size) {
            var channel = this.channel_trees[i]
            for (j in 0 .. channel.size) {
                this.flag_line_init(i, j)
            }
        }
    }
    fun _load(path: String) {
        super._load(path)
        for (i in 0 .. this.opus_beat_count) {
            this.cache.flag_beat_new(i)
        }
        for (i in 0 .. this.channel_trees.size) {
            var channel = this.channel_trees[i]
            for (j in 0 .. channel.size) {
                this.flag_line_init(i, j)
            }
        }
    }

    open public fun set_event(beat_key: BeatKey, position: List<Int>, value: Int, relative: Boolean = false) {
        super.set_event(beat_key, position, value, relative)
        this.flag_beat_change(beat_key)
    }

    open public fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        super.set_percussion_event(beat_key, position)
        this.flag_beat_change(beat_key)
    }

    open public fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        self.flag_beat_change(beat_key)
    }

    open public fun insert_beat(index: Int? = null) {
        var rindex
        if (index == null) {
            rindex = this.opus_beat_count - 1
        } else {
            rindex = index
        }

        super.insert_beat(index)

        this.flag_beat_new(rindex)
    }

    open public fun new_line(channel: Int = 0, index: Int? = null) {
        super.new_line(channel, index)

        var line_index
        if (index == null) {
            line_index = this.channel_trees[channel].size - 1
        } else {
            line_index = index
        }
        this.flag_line_new(channel, line_index)
    }

    open public fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        this.flag_beat_change(beat_key)
    }

    open public fun remove_beat(index: Int? = null) {
        var rindex
        if (index == null) {
            rindex = this.opus_beat_count - 1
        } else {
            rindex = index
        }
        super.remove_beat(index)
        this.flag_beat_pop(rindex)
    }

    open public fun remove_line(channel: Int, index: Int? = null) {
        super.remove_line(channel, index)

        if (index == null) {
            this.flag_line_pop(channel, this.channel_trees[channel].size)
        } else {
            this.flag_line_pop(channel, index)
        }
    }
}

