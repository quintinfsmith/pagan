package com.qfs.radixulous.opusmanager
import android.util.Log
import com.qfs.radixulous.structure.OpusTree
import java.io.File
import java.lang.Integer.max
import java.lang.Integer.min

open class LinksLayer() : OpusManagerBase() {
    var link_pools = mutableListOf<MutableSet<BeatKey>>()
    var link_pool_map = HashMap<BeatKey, Int>()
    // Indicates that links are being calculated to prevent recursion
    var link_locker: Int = 0

    override fun clear() {
        super.clear()
        this.link_pools.clear()
        this.link_pool_map.clear()
        this.link_locker = 0
    }
    fun <T> lock_links(callback: () -> T): T {
        this.link_locker += 1
        try {
            val output = callback()
            this.link_locker -= 1
            return output
        } catch (e: Exception) {
            this.link_locker -= 1
            throw e
        }
    }

    open fun unlink_beat(beat_key: BeatKey) {
        val index = this.link_pool_map.remove(beat_key) ?: return
        this.link_pools[index].remove(beat_key)
    }

    fun clear_link_pool(beat_key: BeatKey) {
        val index = this.link_pool_map.remove(beat_key) ?: return
        for (key in this.link_pools[index].toList()) {
            this.unlink_beat(key)
        }
        this.link_pools.removeAt(index)
    }

    open fun link_beats(beat_key: BeatKey, target: BeatKey) {
        if (beat_key == target) {
            throw Exception("Can't link beat to self")
        }

        val beat_pool_index = this.link_pool_map[beat_key]
        val target_pool_index = this.link_pool_map[target]

        if (beat_pool_index != null && target_pool_index != null) {
            this.overwrite_beat(beat_key, target)
            this.merge_link_pools(beat_pool_index, target_pool_index)
        } else {
            if (beat_pool_index != null) {
                this.link_beat_into_pool(target, beat_pool_index, true)
            } else if (target_pool_index != null) {
                this.link_beat_into_pool(beat_key, target_pool_index, false)
            } else {
                this.overwrite_beat(beat_key, target)
                this.create_link_pool(listOf(beat_key, target))
            }
        }
    }

    fun linked_have_preceding_absolute_event(beat_key: BeatKey, position: List<Int>): Boolean {
        for (linked_key in this.get_all_linked(beat_key)) {
            if (! this.has_preceding_absolute_event(linked_key, position)) {
                return false
            }
        }
        return true
    }


    open fun create_link_pool(beat_keys: List<BeatKey>) {
        val pool_index = this.link_pools.size
        this.link_pools.add(beat_keys.toMutableSet())
        for (beat_key in beat_keys) {
            this.link_pool_map[beat_key] = pool_index
        }
    }

    open fun batch_link_beats(beat_key_pairs: List<Pair<BeatKey, BeatKey>>) {
        for ((from_key, to_key) in beat_key_pairs) {
            this.link_beats(from_key, to_key)
        }
    }

    open fun link_beat_into_pool(beat_key: BeatKey, index: Int, overwrite_pool: Boolean = false) {
        if (overwrite_pool) {
            // Will overwrite all linked
            this.overwrite_beat(this.link_pools[index].first(), beat_key)
        } else {
            this.overwrite_beat(beat_key, this.link_pools[index].first())
        }
        this.link_pool_map[beat_key] = index
        this.link_pools[index].add(beat_key)
    }

    // Only call from link_beats_function
    open fun merge_link_pools(old_pool: Int, new_pool: Int) {
        // First merge the beat's pool into the targets
        for (key in this.link_pools[old_pool]) {
            this.link_pool_map[key] = new_pool
            this.link_pools[new_pool].add(key)
        }

        // then remove the old pool
        this.link_pools.removeAt(old_pool)

        // update the indices
        for ((key, index) in this.link_pool_map) {
            if (index >= old_pool) {
                this.link_pool_map[key] = index - 1
            }
        }
    }

    fun get_all_linked(beat_key: BeatKey): Set<BeatKey> {
        if (this.link_locker > 1) {
            return setOf(beat_key)
        }

        val pool_index = this.link_pool_map[beat_key] ?: return setOf(beat_key)
        return this.link_pools[pool_index]
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>, tree: OpusTree<OpusEvent>) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.replace_tree(linked_key, position, tree)
            }
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.insert_after(linked_key, position)
            }
        }
    }
    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.remove(linked_key, position)
            }
        }
    }
    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.set_percussion_event(linked_key, position)
            }
        }
    }
    override fun set_event(beat_key: BeatKey, position: List<Int>, event: OpusEvent) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.set_event(linked_key, position, event.copy())
            }
        }
    }
    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.split_tree(linked_key, position, splits)
            }
        }
    }
    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.unset(linked_key, position)
            }
        }
    }

    /////////
    private fun remap_links(remap_hook: (beat_key: BeatKey, args: List<Int>) -> BeatKey?, args: List<Int>) {
        val new_pool_map = HashMap<BeatKey, Int>()
        val new_pools = mutableListOf<MutableSet<BeatKey>>()
        for (pool in this.link_pools) {
            val new_pool = mutableSetOf<BeatKey>()
            for (beatkey in pool) {
                val new_beatkey = remap_hook(beatkey, args) ?: continue
                new_pool.add(new_beatkey)
                new_pool_map[new_beatkey] = new_pools.size
            }
            new_pools.add(new_pool)
        }
        this.link_pools = new_pools
        this.link_pool_map = new_pool_map
    }

    private fun rh_change_line_channel(beat_key: BeatKey, args: List<Int>): BeatKey? {
        val old_channel = args[0]
        val line_offset = args[1]
        val new_channel = args[2]
        val new_offset = args[3]

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
        val index = args[0]
        val new_beat = if (beat.beat >= index) {
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
        val channel = args[0]
        val line_offset = args[1]
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

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.remap_links({ beat_key: BeatKey, args: List<Int> ->
             if (beat_key.beat >= beat_index) {
                BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat + 1)
            } else {
                beat_key
            }
        }, listOf())
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)
        this.remap_links({ beat_key: BeatKey, args: List<Int> ->
            if (beat_key.beat > beat_index) {
                BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat - 1)
            } else if (beat_key.beat < beat_index) {
                beat_key
            } else {
                null
            }
        }, listOf())
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
        val data = super.to_json()
        val reflections: MutableList<List<BeatKey>> = mutableListOf()
        for (pool in this.link_pools) {
            reflections.add(pool.toList())
        }
        data.reflections = reflections
        return data
    }

    open fun link_beat_range(beat: BeatKey, target_a: BeatKey, target_b: BeatKey) {
        var (from_key, to_key) = if (target_a.channel < target_b.channel) {
            Pair(
                BeatKey(target_a.channel, target_a.line_offset, -1),
                BeatKey(target_b.channel, target_b.line_offset, -1)
            )
        } else if (target_a.channel == target_b.channel) {
            if (target_a.line_offset < target_b.line_offset) {
                Pair(
                    BeatKey(target_a.channel, target_a.line_offset, -1),
                    BeatKey(target_b.channel, target_b.line_offset, -1)
                )
            } else {
                Pair(
                    BeatKey(target_b.channel, target_b.line_offset, -1),
                    BeatKey(target_a.channel, target_a.line_offset, -1)
                )
            }
        } else {
            Pair(
                BeatKey(target_b.channel, target_b.line_offset, -1),
                BeatKey(target_a.channel, target_a.line_offset, -1)
            )
        }
        from_key.beat = min(target_a.beat, target_b.beat)
        to_key.beat = max(target_a.beat, target_b.beat)
        if (from_key == to_key) {
            throw Exception("Can't self-link beats")
        }
        var working_beat = beat.copy()
        val new_pairs = mutableListOf<Pair<BeatKey, BeatKey>>()
        while (from_key.channel != to_key.channel || from_key.line_offset != to_key.line_offset) {
            // INCLUSIVE
            for (b in 0 .. to_key.beat - from_key.beat) {
                new_pairs.add(
                    Pair(
                        BeatKey(from_key.channel, from_key.line_offset, from_key.beat + b),
                        BeatKey(working_beat.channel, working_beat.line_offset, working_beat.beat + b)
                    )
                )
            }
            if (this.channels[from_key.channel].size - 1 > from_key.line_offset) {
                from_key.line_offset += 1
            } else if (this.channels.size - 1 > from_key.channel) {
                from_key.channel += 1
                from_key.line_offset = 0
            } else {
                throw Exception("Bad BeatKey Range: $target_a .. $target_b")
            }

            if (this.channels[working_beat.channel].size - 1 > working_beat.line_offset) {
                working_beat.line_offset += 1
            } else if (this.channels.size - 1 > working_beat.channel) {
                working_beat.channel += 1
                working_beat.line_offset = 0
            } else {
                throw Exception("Bad BeatKey: $working_beat")
            }
        }
        for (b in 0 .. to_key.beat - from_key.beat) {
            new_pairs.add(
                Pair(
                    BeatKey(from_key.channel, from_key.line_offset, from_key.beat + b),
                    BeatKey(working_beat.channel, working_beat.line_offset, working_beat.beat + b)
                )
            )
        }
        this.batch_link_beats(new_pairs)
    }
}
