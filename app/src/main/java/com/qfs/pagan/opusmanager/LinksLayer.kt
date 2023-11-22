package com.qfs.pagan.opusmanager
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

open class LinksLayer : BaseLayer() {
    class SelfLinkError(beat_key_a: BeatKey, beat_key_b: BeatKey): Exception("$beat_key_a is $beat_key_b")
    class LinkRangeOverlap(from_key: BeatKey, to_key: BeatKey, startkey: BeatKey): Exception("Range($from_key .. $to_key) Contains $startkey")
    class LinkRangeOverflow(from_key: BeatKey, to_key: BeatKey, startkey: BeatKey): Exception("Range($from_key .. $to_key) @ $startkey overflows")
    class InvalidBeatKeyRange(a: BeatKey, b: BeatKey): Exception("$a .. $b")
    class MixedLinkException : Exception("Can't link percussion with non-percussion channels")
    class BadRowLink(from_key: BeatKey, channel: Int, line_offset: Int): Exception("Can only link an entire row (or rows) to the first range of beats of its own row ($from_key != ${BeatKey(channel, line_offset, 0)})")

    var link_pools = mutableListOf<MutableSet<BeatKey>>()
    var link_pool_map = HashMap<BeatKey, Int>()
    // Indicates that links are being calculated to prevent recursion
    private var _link_locker: Int = 0

    override fun clear() {
        super.clear()
        this.link_pools.clear()
        this.link_pool_map.clear()
        this._link_locker = 0
    }
    private fun <T> lock_links(callback: () -> T): T {
        this._link_locker += 1
        try {
            val output = callback()
            this._link_locker -= 1
            return output
        } catch (e: Exception) {
            this._link_locker -= 1
            throw e
        }
    }

    open fun unlink_beat(beat_key: BeatKey) {
        val index = this.link_pool_map[beat_key] ?: return
        if (this.link_pools[index].size > 2) {
            this.link_pools[index].remove(beat_key)
            this.link_pool_map.remove(beat_key)
        } else {
            this.remove_link_pool(index)
        }
    }
    open fun unlink_range(first_key: BeatKey, second_key: BeatKey) {
        for (beat_key in this.get_beatkeys_in_range(first_key, second_key)) {
            if (!this.link_pool_map.contains(beat_key)) {
                continue
            }
            if (this.link_pools.size <= this.link_pool_map[beat_key]!!) {
                continue
            }
            this.unlink_beat(beat_key)
        }
    }

    open fun clear_link_pool(beat_key: BeatKey) {
        val index = this.link_pool_map[beat_key] ?: return
        this.remove_link_pool(index)
    }

    open fun clear_link_pools_by_range(first_key: BeatKey, second_key: BeatKey) {
        val (from_key, to_key) = this.get_ordered_beat_key_pair(first_key, second_key)
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (i < from_key.channel || i > to_key.channel) {
                return@forEachIndexed
            }
            for (j in 0 until channel.size) {
                if (i == from_key.channel && j < from_key.line_offset) {
                    continue
                } else if (i == to_key.channel && j > to_key.line_offset) {
                    continue
                }
                for (k in from_key.beat .. to_key.beat) {
                    this.clear_link_pool(BeatKey(i, j, k))
                }
            }
        }

    }

    open fun link_beats(beat_key: BeatKey, target: BeatKey) {
        if (beat_key == target) {
            throw SelfLinkError(beat_key, target)
        }
        if ((beat_key.channel == this.channels.size - 1 || target.channel == this.channels.size - 1) && target.channel != beat_key.channel) {
            throw MixedLinkException()
        }

        val beat_pool_index = this.link_pool_map[beat_key]
        val target_pool_index = this.link_pool_map[target]
        if (beat_pool_index != null && target_pool_index != null) {
            this.overwrite_beat(beat_key, target)
            if (beat_pool_index != target_pool_index) {
                this.merge_link_pools(beat_pool_index, target_pool_index)
            }
        } else if (beat_pool_index != null) {
            this.link_beat_into_pool(target, beat_pool_index, true)
        } else if (target_pool_index != null) {
            this.link_beat_into_pool(beat_key, target_pool_index, false)
        } else {
            this.overwrite_beat(beat_key, target)
            this.create_link_pool(listOf(beat_key, target))
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

    open fun remove_link_pool(index: Int) {
        val keys = this.link_pools.removeAt(index)
        for (beat_key in keys) {
            this.link_pool_map.remove(beat_key)
        }

        // Adjust link_pool_map
        for ((beat_key, pool_index) in this.link_pool_map) {
            if (pool_index > index) {
                this.link_pool_map[beat_key] = pool_index - 1
            }
        }
    }
    open fun create_link_pool(beat_keys: List<BeatKey>) {
        val pool_index = this.link_pools.size
        this.link_pools.add(beat_keys.toMutableSet())
        for (beat_key in beat_keys) {
            this.link_pool_map[beat_key] = pool_index
        }
    }

    open fun batch_link_beats(beat_key_pairs: List<Pair<BeatKey, BeatKey>>) {
        this.lock_links {
            for ((from_key, to_key) in beat_key_pairs) {
                this.link_beats(from_key, to_key)
            }
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
    open fun merge_link_pools(index_first: Int, index_second: Int) {
        if (index_first == index_second) {
            return
        }
        // First merge the beat's pool into the targets
        for (key in this.link_pools[index_first]) {
            this.link_pool_map[key] = index_second
            this.link_pools[index_second].add(key)
        }

        // then remove the old pool
        this.link_pools.removeAt(index_first)

        // update the indices
        for ((key, index) in this.link_pool_map) {
            if (index > index_first) {
                this.link_pool_map[key] = index - 1
            }
        }
    }

    fun get_all_linked(beat_key: BeatKey): Set<BeatKey> {
        if (this._link_locker > 1) {
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

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.insert(linked_key, position)
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
    // NOTE: Remap_links always needs to be called BEFORE the super call
    // This puts it in the correct order on the history stack
    // Since remap_links is only needed on line/channel/beat operations, there's no need to
    // consider links in the base layer operations so it's ok to remap before
    open fun remap_links(remap_hook: (beat_key: BeatKey) -> BeatKey?) {
        val new_pool_map = HashMap<BeatKey, Int>()
        val new_pools = mutableListOf<MutableSet<BeatKey>>()
        for (pool in this.link_pools) {
            val new_pool = mutableSetOf<BeatKey>()
            for (beatkey in pool) {
                val new_beatkey = remap_hook(beatkey) ?: continue
                new_pool.add(new_beatkey)
                new_pool_map[new_beatkey] = new_pools.size
            }
            // Don't keep pools if there is only one entry left
            if (new_pool.size == 1) {
                new_pool_map.remove(new_pool.first())
            }  else {
                new_pools.add(new_pool)
            }
        }
        this.link_pools = new_pools
        this.link_pool_map = new_pool_map
    }

    override fun remove_channel(channel: Int) {
        this.remap_links { beat_key: BeatKey  ->
            if (beat_key.channel == channel) {
                null
            } else if (beat_key.channel < channel) {
                beat_key
            } else {
                BeatKey(
                    beat_key.channel - 1,
                    beat_key.line_offset,
                    beat_key.beat
                )
            }
        }

        super.remove_channel(channel)
    }



    fun is_networked(beat_key: BeatKey): Boolean {
        return this.link_pool_map.contains(beat_key)
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        this.remap_links { beat_key: BeatKey ->
             if (beat_key.beat >= beat_index) {
                BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat + 1)
            } else {
                beat_key
            }
        }

        super.insert_beat(beat_index, beats_in_column)
    }

    override fun remove_beat(beat_index: Int) {
        super.remove_beat(beat_index)
        this.remap_links { beat_key: BeatKey ->
            if (beat_key.beat > beat_index) {
                BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat - 1)
            } else if (beat_key.beat < beat_index) {
                beat_key
            } else {
                null
            }
        }

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
        val (from_key, to_key) = this.get_ordered_beat_key_pair(target_a, target_b)

        val overlap = if (beat.beat in (from_key.beat .. to_key.beat)) {
            if (beat.channel in (from_key.channel..to_key.channel)) {
                if (to_key.channel == from_key.channel) {
                    beat.line_offset in (from_key.line_offset..to_key.line_offset)
                } else if (beat.channel == from_key.channel) {
                    beat.line_offset in (from_key.line_offset until this.channels[from_key.channel].size)
                } else if (beat.channel == to_key.channel) {
                    beat.line_offset in (0 until to_key.line_offset)
                } else {
                    beat.channel in (from_key.channel + 1 until to_key.channel)
                }
            } else {
                false
            }
        } else {
            false
        }

        if (overlap) {
            throw LinkRangeOverlap(from_key, to_key, beat)
        }
        if (this.beat_count <= beat.beat + (to_key.beat - from_key.beat)) {
            throw LinkRangeOverflow(from_key, to_key, beat)
        }

        // Start OverFlow Check ////
        var lines_in_range = 0
        var lines_available = 0
        val percussion_map = mutableListOf<Boolean>()
        val percussion_channel = this.channels.size - 1
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (i < from_key.channel || i > to_key.channel) {
                return@forEachIndexed
            }
            for (j in 0 until channel.size) {
                if (i == from_key.channel && j < from_key.line_offset) {
                    continue
                } else if (i == to_key.channel && j > to_key.line_offset) {
                    continue
                }
                percussion_map.add(i == percussion_channel)
                lines_in_range += 1
            }
        }
        val target_percussion_map = mutableListOf<Boolean>()
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            if (i < beat.channel) {
                return@forEachIndexed
            }
            for (j in 0 until channel.size) {
                if (i == beat.channel && j < beat.line_offset) {
                    continue
                }
                target_percussion_map.add(i == percussion_channel)
                lines_available += 1
            }
        }

        if (lines_available < lines_in_range) {
            throw LinkRangeOverflow(from_key, to_key, beat)
        }
        // End Overflow Check ////
        if (percussion_map != target_percussion_map.subList(0, percussion_map.size)) {
            throw MixedLinkException()
        }


        val working_beat = beat.copy()
        val new_pairs = mutableListOf<Pair<BeatKey, BeatKey>>()
        while (from_key.channel != to_key.channel || from_key.line_offset != to_key.line_offset) {
            // INCLUSIVE
            for (b in 0 .. to_key.beat - from_key.beat) {
                new_pairs.add(
                    Pair(
                        BeatKey(working_beat.channel, working_beat.line_offset, working_beat.beat + b),
                        BeatKey(from_key.channel, from_key.line_offset, from_key.beat + b)
                    )
                )
            }
            if (this.channels[from_key.channel].size - 1 > from_key.line_offset) {
                from_key.line_offset += 1
            } else if (this.channels.size - 1 > from_key.channel) {
                from_key.channel += 1
                from_key.line_offset = 0
            } else {
                throw InvalidBeatKeyRange(target_a, target_b)
            }

            if (this.channels[working_beat.channel].size - 1 > working_beat.line_offset) {
                working_beat.line_offset += 1
            } else if (this.channels.size - 1 > working_beat.channel) {
                working_beat.channel += 1
                working_beat.line_offset = 0
            } else {
                throw BadBeatKey(working_beat)
            }
        }

        for (b in 0 .. to_key.beat - from_key.beat) {
            new_pairs.add(
                Pair(
                    BeatKey(working_beat.channel, working_beat.line_offset, working_beat.beat + b),
                    BeatKey(from_key.channel, from_key.line_offset, from_key.beat + b)
                )
            )
        }

        this.batch_link_beats(new_pairs)
    }

    open fun link_column(column: Int, beat_key: BeatKey) {
        val new_pool = mutableListOf<BeatKey>()
        this.channels.forEachIndexed { i: Int, channel: OpusChannel ->
            for (j in 0 until channel.size) {
                val working_key = BeatKey(i, j, column)
                if (working_key != beat_key) {
                    this.overwrite_beat(working_key, beat_key)
                }
                new_pool.add(working_key)
            }
        }
        this.create_link_pool(new_pool)
    }

    open fun link_row(channel: Int, line_offset: Int, beat_key: BeatKey) {
        if (beat_key.channel != channel || beat_key.line_offset != line_offset || beat_key.beat != 0) {
            throw BadRowLink(beat_key, channel, line_offset)
        }
        val working_key = BeatKey(channel, line_offset, 0)
        val new_pool = mutableListOf<BeatKey>()
        for (x in 0 until this.beat_count) {
            working_key.beat = x
            if (working_key != beat_key) {
                this.overwrite_beat(working_key, beat_key)
            }
            new_pool.add(working_key.copy())
        }
        this.create_link_pool(new_pool)
    }


    open fun link_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        val (from_key, to_key) = this.get_ordered_beat_key_pair(first_key, second_key)
        // from_key -> to_key need to be first beat. it's a bit arbitrary but from a ui perspective makes it cleaner
        if (from_key.channel != channel || from_key.line_offset != line_offset || from_key.beat != 0) {
            throw BadRowLink(from_key, channel, line_offset)
        }

        val from_beat = min(from_key.beat, to_key.beat)
        val to_beat = max(from_key.beat, to_key.beat)
        val range_width = (to_beat - from_beat) + 1

        this.overwrite_beat_range(
            BeatKey(channel, line_offset, 0),
            first_key,
            second_key
        )

        from_key.beat = 0
        to_key.beat = range_width - 1
        for (i in 1 until this.beat_count / range_width) {
            this.link_beat_range(
                BeatKey(channel, line_offset, i * range_width),
                from_key,
                to_key
            )
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusChannel.OpusLine) {
        this.remap_links { beat_key: BeatKey ->
            if (beat_key.channel == channel && beat_key.line_offset >= line_offset) {
                BeatKey(
                    beat_key.channel,
                    beat_key.line_offset + 1,
                    beat_key.beat
                )
            } else {
                beat_key
            }
        }
        super.insert_line(channel, line_offset, line)
    }

    override fun new_line(channel: Int, line_offset: Int?): OpusChannel.OpusLine {
        if (line_offset != null) {
            this.remap_links { beat_key: BeatKey ->
                if (beat_key.channel == channel && beat_key.line_offset >= line_offset) {
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset + 1,
                        beat_key.beat
                    )
                } else {
                    beat_key
                }
            }
        }
        return super.new_line(channel, line_offset)
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusChannel.OpusLine {
        this.remap_links { beat_key: BeatKey ->
            if (beat_key.channel == channel) {
                if (beat_key.line_offset > line_offset) {
                    BeatKey(
                        beat_key.channel,
                        beat_key.line_offset - 1,
                        beat_key.beat
                    )
                } else if (beat_key.line_offset == line_offset) {
                    null // DO NOT retain beat keys from deleted line
                } else {
                    beat_key
                }
            } else {
                beat_key
            }
        }
        return super.remove_line(channel, line_offset)
    }

    override fun move_line(channel_old: Int, line_old: Int, channel_new: Int, line_new: Int) {
        // Create a map of where the removed line was. no need to pop at this point since
        // that is handled in remove_line() within move_line()
        val new_pools = mutableListOf<MutableSet<BeatKey>>()
        this.link_pools.forEach { pool: MutableSet<BeatKey> ->
            val new_pool = mutableSetOf<BeatKey>()
            for (beat_key in pool) {
                // new_channel will only be changed if the beatkey is on the line being moved AND being moved out of the channel
                var new_channel = beat_key.channel

                val new_line = if (beat_key.channel == channel_old) {
                    if (beat_key.line_offset == line_old) {
                        if (channel_old == channel_new) {
                            if (line_new > line_old) {
                                line_new - 1
                            } else {
                                line_new
                            }
                        } else {
                            new_channel = channel_new
                            line_new
                        }
                    } else if (beat_key.line_offset > line_old) {
                        if (channel_old == channel_new) {
                            if (beat_key.line_offset < line_new) {
                                beat_key.line_offset - 1
                            } else {
                                beat_key.line_offset
                            }
                        } else {
                            beat_key.line_offset - 1
                        }
                    } else if (beat_key.line_offset >= line_new) {
                        beat_key.line_offset + 1
                    } else {
                        beat_key.line_offset
                    }
                } else if (beat_key.channel == channel_new) {
                    // can skip old == new, implicitly handled when channel == channel_old
                    if (beat_key.line_offset <= line_new) {
                        beat_key.line_offset
                    } else {
                        beat_key.line_offset + 1
                    }
                } else {
                    beat_key.line_offset
                }

                new_pool.add(BeatKey(new_channel, new_line, beat_key.beat))
           }
           new_pools.add(new_pool)
       }

       this.lock_links {
           super.move_line(channel_old, line_old, channel_new, line_new)
       }


       this.link_pools = new_pools
       this.link_pool_map.clear()
       this.link_pools.forEachIndexed { i: Int, pool: MutableSet<BeatKey> ->
           for (beat_key in pool) {
               this.link_pool_map[beat_key] = i
           }
       }
   }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        val working_channel = channel ?: (this.channels.size - 1)
        this.remap_links { beat_key: BeatKey ->
            if (beat_key.channel < working_channel) {
                beat_key
            } else {
                BeatKey(
                    beat_key.channel + 1,
                    beat_key.line_offset,
                    beat_key.beat
                )
            }
        }
        super.new_channel(channel, lines, uuid)
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        this.lock_links {
            for (linked_key in this.get_all_linked(beat_key)) {
                super.set_duration(linked_key, position, duration)
            }
        }
    }
}
