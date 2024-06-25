package com.qfs.pagan.opusmanager
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

open class OpusLayerLinks : OpusLayerBase() {
    class SelfLinkError(beat_key_a: BeatKey, beat_key_b: BeatKey): Exception("$beat_key_a is $beat_key_b")
    class LinkRangeOverlap(from_key: BeatKey, to_key: BeatKey, startkey: BeatKey): Exception("Range($from_key .. $to_key) Contains $startkey")
    class InvalidBeatKeyRange(a: BeatKey, b: BeatKey): Exception("$a .. $b")
    class BadRowLink(from_key: BeatKey, channel: Int, line_offset: Int): Exception("Can only link an entire row (or rows) to the first range of beats of its own row ($from_key != ${BeatKey(channel, line_offset, 0)})")

    var link_pools = mutableListOf<MutableSet<BeatKey>>()
    var link_pool_map = HashMap<BeatKey, Int>()
    // Indicates that the initial function has been called, and that links shouldn't be traversed
    // as they'll be handled later
    internal var link_lock: Int = 0
    // indicates that the current logic is being applied to a linked beat, rather than the original target
    internal var _link_deviation_count: Int = 0

    override fun clear() {
        super.clear()
        this.link_pools.clear()
        this.link_pool_map.clear()
        this.link_lock = 0
    }
    internal fun <T> lock_links(callback: () -> T): T {
        this.link_lock += 1
        return try {
            val output = callback()
            this.link_lock -= 1
            output
        } catch (e: Exception) {
            this.link_lock -= 1
            throw e
        }
    }

    private fun _apply_to_linked(beat_key: BeatKey, callback: (BeatKey) -> Unit) {
        this._link_deviation_count += 1
        try {
            for (linked_key in this._get_all_others_linked(beat_key)) {
                callback(linked_key)
            }
            this._link_deviation_count -= 1
        } catch (e: Exception) {
            this._link_deviation_count -= 1
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
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_key, second_key)
        for (beat_key in this.get_beatkeys_in_range(from_key, to_key)) {
            this.clear_link_pool(beat_key)
        }
    }

    open fun link_beats(beat_key: BeatKey, target: BeatKey) {
        if (beat_key == target) {
            throw SelfLinkError(beat_key, target)
        }

        if (this.is_percussion(beat_key.channel) != this.is_percussion(target.channel)) {
            throw MixedInstrumentException(beat_key, target)
        }

        val beat_pool_index = this.link_pool_map[beat_key]
        val target_pool_index = this.link_pool_map[target]
        if (beat_pool_index != null && target_pool_index != null) {
            this.replace_tree(beat_key, null, this.get_tree(target))
            if (beat_pool_index != target_pool_index) {
                this.merge_link_pools(beat_pool_index, target_pool_index)
            }
        } else if (beat_pool_index != null) {
            this.link_beat_into_pool(target, beat_pool_index, true)
        } else if (target_pool_index != null) {
            this.link_beat_into_pool(beat_key, target_pool_index, false)
        } else {
            this.replace_tree(beat_key, null, this.get_tree(target, listOf()))
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
            // dependent gets overridden with independent
            for ((dependent_key, independent_key) in beat_key_pairs) {
                if (this.is_networked(dependent_key)) {
                    this.unlink_beat(dependent_key)
                }
                this.link_beats(dependent_key, independent_key)
            }
        }
    }

    open fun link_beat_into_pool(beat_key: BeatKey, index: Int, overwrite_pool: Boolean = false) {
        if (overwrite_pool) {
            // Will overwrite all linked
            this.replace_tree(this.link_pools[index].first(), null, this.get_tree(beat_key))
        } else {
            this.replace_tree(beat_key, null, this.get_tree(this.link_pools[index].first()))
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

    private fun _get_all_others_linked(beat_key: BeatKey): Set<BeatKey> {
        val output = this.get_all_linked(beat_key).toMutableSet()
        output.remove(beat_key)
        return output
    }

    fun get_all_linked(beat_key: BeatKey): Set<BeatKey> {
        if (this.link_lock > 1) {
            return setOf(beat_key)
        }

        val pool_index = this.link_pool_map[beat_key] ?: return setOf(beat_key)
        return this.link_pools[pool_index]
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        this.lock_links {
            super.set_tuning_map(new_map, mod_events)
        }
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        this.lock_links {
            super.replace_tree(beat_key, position, tree)
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.replace_tree(linked_key, position, tree)
            }
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            super.insert(beat_key, position)
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.insert(linked_key, position)
            }
        }
    }
    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            super.insert_after(beat_key, position)
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.insert_after(linked_key, position)
            }
        }
    }
    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            super.remove(beat_key, position)
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.remove(linked_key, position)
            }
        }
    }
    override fun set_percussion_event(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.set_percussion_event(linked_key, position)
            }
            super.set_percussion_event(beat_key, position)
        }
    }
    override fun set_event(beat_key: BeatKey, position: List<Int>, event: InstrumentEvent) {
        this.lock_links {
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.set_event(linked_key, position, event.copy())
            }
            super.set_event(beat_key, position, event.copy())
        }
    }
    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.lock_links {
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.split_tree(linked_key, position, splits)
            }
            super.split_tree(beat_key, position, splits, move_event_to_end)
        }
    }
    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.lock_links {
            this._apply_to_linked(beat_key) { linked_key: BeatKey ->
                this.unset(linked_key, position)
            }
            super.unset(beat_key, position)
        }
    }

    /////////
    // NOTE: Remap_links always needs to be called BEFORE the super call
    // This puts it in the correct order on the history stack
    // Since remap_links is only needed on line/channel/beat operations, there's no need to
    // consider links in the base layer operations so it's ok to remap before
    // ALSO: remap_links should always be called outside of the lock_links wrapper
    open fun remap_links(remap_hook: (beat_key: BeatKey) -> BeatKey?) {
        if (this.link_lock > 0) {
            return
        }
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

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<InstrumentEvent>>?) {
        this.remap_links { beat_key: BeatKey ->
             if (beat_key.beat >= beat_index) {
                BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat + 1)
            } else {
                beat_key
            }
        }

        super.insert_beat(beat_index, beats_in_column)
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        this.remap_links { beat_key: BeatKey ->
            if (beat_key.beat >= beat_index) {
                BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat + count)
            } else {
                beat_key
            }
        }

        this.lock_links {
            super.insert_beats(beat_index, count)
        }
    }

    override fun remove_beat(beat_index: Int) {
        this.remap_links { beat_key: BeatKey ->
            if (beat_key.beat > beat_index) {
                BeatKey(beat_key.channel, beat_key.line_offset, beat_key.beat - 1)
            } else if (beat_key.beat < beat_index) {
                beat_key
            } else {
                null
            }
        }
        super.remove_beat(beat_index)
    }

    override fun import_from_other(other: OpusLayerBase) {
        super.import_from_other(other)
        for (i in (other as OpusLayerLinks).link_pools.indices) {
            val pool = other.link_pools[i]
            this.link_pools.add(pool)
            for (beatkey in pool) {
                this.link_pool_map[beatkey] = i
            }
        }
    }

    open fun link_beat_range(beat_key: BeatKey, target_a: BeatKey, target_b: BeatKey) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(target_a, target_b)
        val keys_to_link_independent = this.get_beatkeys_in_range(from_key, to_key)
        val keys_to_link_dependent = this._get_beatkeys_from_range(beat_key, from_key, to_key)

        if (keys_to_link_independent.toSet().intersect(keys_to_link_dependent.toSet()).isNotEmpty()) {
            throw LinkRangeOverlap(from_key, to_key, beat_key)
        }

        this.batch_link_beats(List<Pair<BeatKey, BeatKey>>(keys_to_link_independent.size) { i: Int ->
            Pair(
                keys_to_link_dependent[i],
                keys_to_link_independent[i]
            )
        })
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
                this.replace_tree(working_key, listOf(), this.get_tree(beat_key, listOf()))
            }
            new_pool.add(working_key.copy())
        }
        this.create_link_pool(new_pool)
    }


    open fun link_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        val (from_key, to_key) = OpusLayerBase.get_ordered_beat_key_pair(first_key, second_key)
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

        val abs_from_line_offset = this.get_abs_offset(from_key.channel, from_key.line_offset)
        val abs_to_line_offset = this.get_abs_offset(to_key.channel, to_key.line_offset)

        for (c in abs_from_line_offset .. abs_to_line_offset) {
            val (working_channel, working_line_offset) = this.get_std_offset(c)
            for (i in 0 until range_width) {
                val beat_key_list = mutableListOf<BeatKey>()
                for (j in 0 until this.beat_count / range_width) {
                    beat_key_list.add(
                        BeatKey(
                            working_channel,
                            working_line_offset,
                            (from_key.beat + i) + (j * range_width)
                        )
                    )
                }
                this.create_link_pool(beat_key_list)
            }
        }
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLine) {
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

    override fun new_line(channel: Int, line_offset: Int?): OpusLineAbstract<*> {
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

    override fun remove_line(channel: Int, line_offset: Int): OpusLine {
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
            for (linked_key in this._get_all_others_linked(beat_key)) {
                this.set_duration(linked_key, position, duration)
            }
            super.set_duration(beat_key, position, duration)
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        this.remap_links { beat_key: BeatKey ->
            if (beat_key.channel == channel_a && beat_key.line_offset == line_a) {
                BeatKey(channel_b, line_b, beat_key.beat)
            } else if (beat_key.channel == channel_b && beat_key.line_offset == line_b) {
                BeatKey(channel_a, line_a, beat_key.beat)
            } else {
                beat_key
            }
        }

        super.swap_lines(channel_a, line_a, channel_b, line_b)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is OpusLayerLinks)  {
            return false
        }

        for (pool in this.link_pools) {
            var has_match = false
            for (i in 0 until other.link_pools.size) {
                if (other.link_pools[i] == pool) {
                    has_match = true
                    break
                }
            }

            if (!has_match) {
                return false
            }
        }

        return super.equals(other)
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        val keys_in_range = this.get_beatkeys_in_range(first_corner, second_corner)

        val difference = this.get_abs_difference(first_corner, second_corner)
        val abs_offset = this.get_abs_offset(beat_key.channel, beat_key.line_offset)
        val (new_channel, new_line_offset) = this.get_std_offset(abs_offset + difference.first)

        val keys_to_forget = this.get_beatkeys_in_range(
            beat_key,
            BeatKey(
                new_channel,
                new_line_offset,
                beat_key.beat + difference.second
            )
        )

        this.remap_links { working_key: BeatKey ->
            if (keys_to_forget.contains(working_key)) {
                null
            } else if (keys_in_range.contains(working_key)) {
                val (y_diff, x_diff) = this.get_abs_difference(first_corner, working_key)
                val (to_channel, to_line_offset) = this.get_std_offset(abs_offset + y_diff)
                BeatKey(
                    to_channel,
                    to_line_offset,
                    beat_key.beat + x_diff
                )
            } else {
                working_key
            }
        }
        super.move_beat_range(beat_key, first_corner, second_corner)
    }

    override fun move_leaf(
        beatkey_from: BeatKey,
        position_from: List<Int>,
        beatkey_to: BeatKey,
        position_to: List<Int>
    ) {
        if (position_from.isEmpty()) {
            this.remap_links { beat_key: BeatKey ->
                when (beat_key) {
                    beatkey_from -> beatkey_to
                    beatkey_to -> null
                    else -> beat_key
                }
            }
        }

        this.lock_links {
            super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        }
    }

    /* Not Currently In Use. */
    // open fun link_alike(corner_top: BeatKey, corner_bottom: BeatKey) {
    //     val alike_ranges = this.find_like_range(corner_top, corner_bottom)
    //     for (range in alike_ranges) {
    //         try {
    //             this.link_beat_range(range.first, corner_top, corner_bottom)
    //         } catch (e: OpusLayerLinks.MixedInstrumentException) {
    //             //pass
    //         }
    //     }
    // }

}
