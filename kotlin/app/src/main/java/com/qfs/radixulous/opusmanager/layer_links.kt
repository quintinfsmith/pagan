package com.qfs.radixulous.opusmanager
import com.qfs.radixulous.structure.OpusTree
import java.io.File

open class LinksLayer() : AbsoluteValueLayer() {
    var link_pools = mutableListOf<MutableSet<BeatKey>>()
    var link_pool_map = HashMap<BeatKey, Int>()
    // Indicates that links are being calculated to prevent recursion
    var link_locker: Int = 0

    override fun purge_cache() {
        super.purge_cache()
        this.link_pools.clear()
        this.link_pool_map.clear()
        this.link_locker = 0
    }

    open fun unlink_beat(beat_key: BeatKey) {
        var index = this.link_pool_map.remove(beat_key) ?: return
        this.link_pools[index].remove(beat_key)
    }

    fun clear_link_pool(beat_key: BeatKey) {
        var index = this.link_pool_map.remove(beat_key) ?: return
        for (key in this.link_pools[index]) {
            this.unlink_beat(key)
        }
        this.link_pools.removeAt(index)
    }

    open fun link_beats(beat_key: BeatKey, target: BeatKey) {
        if (beat_key == target) {
            throw Exception("Can't link beat to self")
        }
        // Remove any existing link
        this.unlink_beat(beat_key)

        // Replace existing tree with a copy of the target
        this.overwrite_beat(beat_key, target)

        var pool_index = this.link_pool_map[beat_key] ?: this.link_pools.size

        this.link_pool_map[beat_key] = pool_index
        if (pool_index == this.link_pools.size) {
            this.link_pools.add(mutableSetOf(beat_key, target))
            this.link_pool_map[target] = pool_index
        } else {
            this.link_pools[pool_index].add(beat_key)
        }

    }

    fun get_all_linked(beat_key: BeatKey): Set<BeatKey> {
        if (this.link_locker > 1) {
            return setOf(beat_key)
        }

        var pool_index = this.link_pool_map[beat_key] ?: return setOf(beat_key)
        return this.link_pools[pool_index]
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.replace_tree(linked_key, position, tree)
        }
        this.link_locker -= 1
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.insert_after(linked_key, position)
        }
        this.link_locker -= 1
    }
    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.remove(linked_key, position)
        }
        this.link_locker -= 1
    }
    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.set_percussion_event(linked_key, position)
        }
        this.link_locker -= 1
    }
    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.set_event(linked_key, position, event)
        }
        this.link_locker -= 1
    }
    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.split_tree(linked_key, position, splits)
        }
        this.link_locker -= 1
    }
    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.link_locker += 1
        for (linked_key in this.get_all_linked(beat_key)) {
            super.unset(linked_key, position)
        }
        this.link_locker -= 1
    }

    /////////
    private fun remap_links(remap_hook: (beat_key: BeatKey, args: List<Int>) -> BeatKey?, args: List<Int>) {
        var new_pool_map = HashMap<BeatKey, Int>()
        var new_pools = mutableListOf<MutableSet<BeatKey>>()
        for (pool in this.link_pools) {
            var new_pool = mutableSetOf<BeatKey>()
            for (beatkey in pool) {
                var new_beatkey = remap_hook(beatkey, args) ?: continue
                new_pool.add(new_beatkey)
                new_pool_map[new_beatkey] = new_pools.size
            }
            new_pools.add(new_pool)
        }
        this.link_pools = new_pools
        this.link_pool_map = new_pool_map
    }

    private fun rh_change_line_channel(beat_key: BeatKey, args: List<Int>): BeatKey? {
        var old_channel = args[0]
        var line_offset = args[1]
        var new_channel = args[2]
        var new_offset = args[3]

        var new_beat = beat_key
        if (beat_key.channel == old_channel) {
            if (beat_key.line_offset == line_offset) {
                new_beat = BeatKey(new_channel, new_offset, beat_key.beat)
            } else if (beat_key.line_offset > line_offset) {
                new_beat = BeatKey(beat_key.channel, beat_key.line_offset - 1, beat_key.beat)
            }
        }
        return new_beat
    }


    private fun rh_remove_beat(beat: BeatKey, args: List<Int>): BeatKey? {
        var index = args[0]
        var new_beat = if (beat.beat >= index) {
            BeatKey(beat.channel, beat.line_offset, beat.beat - 1)
        } else {
            beat
        }

        return new_beat
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.remap_links(this::rh_remove_channel, listOf(channel))
    }

    private fun rh_remove_channel(beat: BeatKey, args: List<Int>): BeatKey? {
        return if (beat.channel == args[0]) {
            null
        } else {
            beat
        }
    }

    private fun rh_remove_line(beat: BeatKey, args: List<Int>): BeatKey? {
        var channel = args[0]
        var line_offset = args[1]
        var new_beat: BeatKey? = beat
        if (beat.channel == channel) {
            if (beat.line_offset == line_offset) {
                new_beat = null
            } else if (beat.line_offset > line_offset) {
                new_beat = BeatKey(channel, line_offset - 1, beat.beat)
            }
        }
        return new_beat
    }

    fun is_networked(channel: Int, line_offset: Int, beat: Int): Boolean {
        return this.link_pool_map.contains(BeatKey(channel, line_offset, beat))
    }

    override fun load_json(json_data: LoadedJSONData) {
        super.load_json(json_data)
        if (json_data.reflections == null) {
            return
        }

        json_data.reflections!!.forEachIndexed { i: Int, pool: List<BeatKey> ->
            this.link_pools.add(pool.toMutableSet())
            for (beatkey in pool) {
                this.link_pool_map[beatkey] = i
            }
        }
    }

    override fun to_json(): LoadedJSONData {
        var data = super.to_json()
        var reflections: MutableList<List<BeatKey>> = mutableListOf()
        for (pool in this.link_pools) {
            reflections.add(pool.toList())
        }
        data.reflections = reflections
        return data
    }
}
