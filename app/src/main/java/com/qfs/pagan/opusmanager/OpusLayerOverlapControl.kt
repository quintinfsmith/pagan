package com.qfs.pagan.opusmanager

import com.qfs.pagan.Rational
import com.qfs.pagan.structure.OpusTree

open class OpusLayerOverlapControl: OpusLayerBase() {
    class BlockedTreeException(beat_key: BeatKey, position: List<Int>, blocker_key: BeatKey, blocker_position: List<Int>): Exception("$beat_key | $position is blocked by event @ $blocker_key $blocker_position")

    private val _cache_blocked_tree_map = HashMap<Pair<BeatKey, List<Int>>, MutableList<Triple<BeatKey, List<Int>, Rational>>>()
    private val _cache_inv_blocked_tree_map = HashMap<Pair<BeatKey, List<Int>>, Triple<BeatKey, List<Int>, Rational>>()

    private fun _init_blocked_tree_caches() {
        var channels = this.get_all_channels()
        this._cache_blocked_tree_map.clear()
        this._cache_inv_blocked_tree_map.clear()

        for (i in 0 until channels.size) {
            for (j in 0 until channels[i].size) {
                var beat_key = BeatKey(i, j, 0)
                var position = this.get_first_position(beat_key, listOf())
                while (true) {
                    val working_tree = this.get_tree(beat_key, position)
                    if (working_tree.is_event()) {
                        this._cache_tree_overlaps(beat_key, position)
                    }

                    val pair = this.get_proceding_leaf_position(beat_key, position) ?: break
                    beat_key = pair.first
                    position = pair.second
                }
            }
        }
    }

    override fun on_project_changed() {
        super.on_project_changed()
        this._init_blocked_tree_caches()
    }

    fun _blocked_tree_check(beat_key: BeatKey, position: List<Int>) {
        val (blocker_key, blocker_position, amount) = this._cache_inv_blocked_tree_map[Pair(beat_key, position)] ?: return
        throw BlockedTreeException(beat_key, position, blocker_key, blocker_position)
    }

    fun get_blocking_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        val tree = this.get_tree(beat_key, position)
        val stack = mutableListOf(Pair(tree, position))

        while (stack.isNotEmpty()) {
            val (working_tree, working_position) = stack.removeFirst()
            if (working_tree.is_leaf()) {
                if (this._cache_inv_blocked_tree_map.containsKey(Pair(beat_key, working_position))) {
                    val entry = this._cache_inv_blocked_tree_map[Pair(beat_key, working_position)]!!
                    return Pair(entry.first.copy(), entry.second.toList())
                }
            } else {
                for (i in 0 until working_tree.size) {
                    stack.add(
                        Pair(
                            working_tree[i],
                            List(working_position.size + 1) { j: Int ->
                                if (j == working_position.size) {
                                    i
                                } else {
                                    working_position[j]
                                }
                            }
                        )
                    )
                }
            }
        }
        return null
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        TODO("NEED OVERLAP CHECK")
        val working_position = position ?: listOf()
        this.decache_overlapping_leaf(beat_key, working_position)
        super.replace_tree(beat_key, position, tree)
        this._cache_tree_overlaps(beat_key, working_position)
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: InstrumentEvent) {
        val blocked_pair = this.is_blocked_set_event(beat_key, position, event.duration)
        if (blocked_pair != null) {
            throw BlockedTreeException(beat_key, position, blocked_pair.first, blocked_pair.second)
        }

        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.set_event(beat_key, position, event)
        }
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        val parent_position = if (position.isNotEmpty()) {
            position.subList(0,  position.size - 1)
        } else {
            position
        }

        this.recache_blocked_tree_wrapper(beat_key, parent_position) {
            super.insert(beat_key, position)
        }
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.insert_after(beat_key, position)
        }
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        val blocked_pair = this.is_blocked_remove(beat_key, position)
        if (blocked_pair != null) {
            throw BlockedTreeException(beat_key, position, blocked_pair.first, blocked_pair.second)
        }
        this.recache_blocked_tree_wrapper(beat_key, position.subList(0, position.size - 1)) {
            super.remove_standard(beat_key, position)
        }
    }

    open fun decache_overlapping_leaf(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                this.decache_overlapping_leaf(beat_key, List(position.size + 1) { j: Int ->
                    if (j == position.size) {
                        i
                    } else {
                        position[j]
                    }
                })
            }
        }
        val cache_key = Pair(beat_key, position)

        if (!this._cache_blocked_tree_map.containsKey(cache_key)) {
            return
        }
        for ((blocked_beat_key, blocked_position, _) in this._cache_blocked_tree_map.remove(cache_key)!!) {
            val overlapped_key = Pair(blocked_beat_key, blocked_position)
            this._cache_inv_blocked_tree_map.remove(overlapped_key)
            this.on_overlap_removed(cache_key, overlapped_key)
        }
    }


    override fun remove_beat(beat_index: Int) {
        val decache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        val needs_recache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        val needs_decrement = mutableListOf<Pair<BeatKey, List<Int>>>()

        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            if (head.first.beat == beat_index) {
                decache.add(Pair(head.first, head.second))
            } else if (tail.first.beat >= beat_index && head.first.beat < beat_index) {
                needs_recache.add(Pair(head.first, head.second))
            } else if (head.first.beat > beat_index && !needs_decrement.contains(Pair(head.first, head.second))) {
                needs_decrement.add(Pair(head.first, head.second))
            }
        }

        for (before in needs_recache) {
            var working_beat_key = BeatKey(before.first.channel, before.first.line_offset, beat_index + 1)
            var working_position = this.get_first_position(working_beat_key)

            if (!this.get_tree(working_beat_key, working_position).is_event()) {
                val next = this.get_proceding_event_position(working_beat_key, working_position) ?: continue
                working_beat_key = next.first
                working_position = next.second
            }

            val (before_offset, before_width) = this.get_leaf_offset_and_width(before.first, before.second)
            var duration = this.get_tree(before.first, before.second).get_event()?.duration ?: 1

            var (after_offset, _) = this.get_leaf_offset_and_width(working_beat_key, working_position)
            after_offset -= 1

            if (after_offset >= before_offset && after_offset < before_offset + Rational(duration, before_width)) {
                throw BlockedTreeException(working_beat_key, working_position, before.first, before.second)
            }

            for (after in needs_decrement) {
                if (!(before.first.channel == after.first.channel && before.first.line_offset == after.first.line_offset)) {
                    continue
                }
            }
        }

        for (cache_key in decache) {
            this.decache_overlapping_leaf(cache_key.first, cache_key.second)
        }

        decache.clear()

        super.remove_beat(beat_index)

        val new_cache = Array(needs_decrement.size) { i: Int ->
            val original_blocked = this._cache_blocked_tree_map.remove(needs_decrement[i])!!
            var (beat_key, position) = needs_decrement[i]

            val new_beat_key = BeatKey(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat - 1
            )

            Pair(
                Pair(new_beat_key, position.toList()),
                MutableList(original_blocked.size) { j: Int ->
                    this._cache_inv_blocked_tree_map.remove(
                        Pair(
                            original_blocked[j].first,
                            original_blocked[j].second
                        )
                    )

                    Triple(
                        BeatKey(
                            original_blocked[j].first.channel,
                            original_blocked[j].first.line_offset,
                            original_blocked[j].first.beat - 1
                        ),
                        original_blocked[j].second.toList(),
                        original_blocked[j].third.copy()
                    )
                }
            )
        }

        for ((cache_key, blocked_map) in new_cache) {
            for ((working_key, working_position, amount) in blocked_map) {
                this._assign_to_inv_cache(working_key, working_position, cache_key.first, cache_key.second, amount)
            }
            this._cache_blocked_tree_map[cache_key] = blocked_map
        }

        for ((blocker_key, blocker_position) in needs_recache) {
            this._cache_tree_overlaps(blocker_key, blocker_position)
        }

    }

    private fun _assign_to_inv_cache(beat_key: BeatKey, position: List<Int>, blocker_key: BeatKey, blocker_position: List<Int>, amount: Rational) {
        this._cache_inv_blocked_tree_map[Pair(beat_key.copy(), position.toList())] = Triple(
            blocker_key.copy(),
            blocker_position.toList(),
            amount
        )
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<InstrumentEvent>>?) {
        val needs_recache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        val needs_inc = mutableListOf<Pair<BeatKey, List<Int>>>()

        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            if (tail.first.beat < beat_index) {
                continue
            } else if (head.first.beat < beat_index) {
                needs_recache.add(Pair(head.first, head.second))
            } else if (!needs_inc.contains(Pair(head.first, head.second))) {
                needs_inc.add(Pair(head.first, head.second))
            }
        }

        super.insert_beat(beat_index, beats_in_column)

        val new_cache = Array(needs_inc.size) { i: Int ->
            val original_blocked = this._cache_blocked_tree_map.remove(needs_inc[i])!!
            var (beat_key, position) = needs_inc[i]

            val new_beat_key = BeatKey(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat + 1
            )

            Pair(
                Pair(new_beat_key, position),
                MutableList(original_blocked.size) { j: Int ->
                    this._cache_inv_blocked_tree_map.remove(
                        Pair(
                            original_blocked[j].first,
                            original_blocked[j].second
                        )
                    )

                    Triple(
                        BeatKey(
                            original_blocked[j].first.channel,
                            original_blocked[j].first.line_offset,
                            original_blocked[j].first.beat + 1
                        ),
                        original_blocked[j].second.toList(),
                        original_blocked[j].third.copy()
                    )
                }
            )
        }

        for ((cache_key, blocked_map) in new_cache) {
            for ((working_key, working_position, amount) in blocked_map) {
                this._assign_to_inv_cache(working_key, working_position, cache_key.first, cache_key.second, amount)
            }
            this._cache_blocked_tree_map[cache_key] = blocked_map
        }

        for (cache_key in needs_recache) {
            this._cache_tree_overlaps(cache_key.first, cache_key.second)
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.split_tree(beat_key, position, splits, move_event_to_end)
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        val blocked_pair = this.is_blocked_set_event(beat_key, position, duration)
        if (blocked_pair != null) {
            throw BlockedTreeException(beat_key, position, blocked_pair.first, blocked_pair.second)
        }

        super.set_duration(beat_key, position, duration)
        this.decache_overlapping_leaf(beat_key, position)
        this._cache_tree_overlaps(beat_key, position)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        this.decache_overlapping_leaf(beat_key, position)
        super.unset(beat_key, position)
    }

    override fun clear() {
        super.clear()
        this._cache_blocked_tree_map.clear()
        this._cache_inv_blocked_tree_map.clear()
    }

    // Get all blocked positions connected, regardless of if the given position is an event or a blocked tree
    fun get_all_blocked_positions(beat_key: BeatKey, position: List<Int>): List<Pair<BeatKey, List<Int>>> {
        val original = this.get_original_position(beat_key, position)
        val output = mutableListOf<Pair<BeatKey, List<Int>>>(original)
        val blocked_trees = this._cache_blocked_tree_map[original] ?: listOf()
        for ((blocked_beat_key, blocked_position, _) in blocked_trees) {
            output.add(Pair(blocked_beat_key.copy(), blocked_position.toList()))
        }
        return output
    }

    fun get_original_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>> {
        return this.get_blocking_position(beat_key, position) ?: Pair(beat_key, position)
    }

    // ----------------------------- Layer Specific functions ---------------------
    private fun calculate_blocking_leafs(beat_key: BeatKey, position: List<Int>): MutableList<Triple<BeatKey, List<Int>, Rational>> {
        val (target_offset, target_width) = this.get_leaf_offset_and_width(beat_key, position)
        val target_tree = this.get_tree(beat_key, position)

        if (!target_tree.is_event() || target_tree.get_event()!!.duration == 1) {
            return mutableListOf()
        }

        val duration_width = Rational(target_tree.get_event()!!.duration, target_width)

        var next_beat_key = beat_key
        var next_position = position

        val output = mutableListOf<Triple<BeatKey, List<Int>, Rational>>() // BeatKey, Position, Width
        val end = target_offset + duration_width

        while (true) {
            val next = this.get_proceding_leaf_position(next_beat_key, next_position) ?: break
            next_beat_key = next.first
            next_position = next.second

            val (next_offset, next_width) = this.get_leaf_offset_and_width(next_beat_key, next_position)
            val adj_offset = next_offset + Rational(1, next_width)

            if (end > adj_offset) {
                output.add(Triple(next_beat_key, next_position, Rational(1, 1)))
            } else if (end > next_offset) {
                output.add(Triple(next_beat_key, next_position, (end - adj_offset) * -1))
                break
            } else {
                break
            }
        }

        /*
         * WARNING!!! this may eat up memory if abused.
         * Currently, it's prevented by the 99 duration limit
         */
        // calculate overflowing values if the duration is longer than the song
        if (next_beat_key.beat == this.beat_count - 1) {
            next_position = listOf()
            var next_beat = next_beat_key.beat + 1
            while (true) {
                if (end > next_beat + 1) {
                    output.add(
                        Triple(
                            BeatKey(
                                next_beat_key.channel,
                                next_beat_key.line_offset,
                                next_beat
                            ),
                            next_position,
                            Rational(1, 1)
                        )
                    )
                } else if (end > next_beat) {
                    output.add(
                        Triple(
                            BeatKey(
                                next_beat_key.channel,
                                next_beat_key.line_offset,
                                next_beat
                            ),
                            next_position,
                            (Rational(next_beat, 1) - end) * -1
                        )
                    )
                    break
                } else {
                    break
                }
                next_beat += 1
            }
        }

        return output
    }

    open fun on_overlap(overlapper: Pair<BeatKey, List<Int>>, overlappee: Pair<BeatKey, List<Int>>) { }
    open fun on_overlap_removed(overlapper: Pair<BeatKey, List<Int>>, overlappee: Pair<BeatKey, List<Int>>) { }

    private fun _cache_tree_overlaps(beat_key: BeatKey, position: List<Int>) {
        val tree = this.get_tree(beat_key, position)
        val stack = mutableListOf<Triple<OpusTree<*>, BeatKey, List<Int>>>(Triple(tree, beat_key, position))
        while (stack.isNotEmpty()) {
            val (working_tree, working_beat_key, working_position) = stack.removeFirst()
            if (working_tree.is_leaf()) {
                val cache_key = Pair(working_beat_key.copy(), working_position.toList())
                this._cache_blocked_tree_map[cache_key] = this.calculate_blocking_leafs(working_beat_key, working_position)
                for ((blocked_beat_key, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
                    this._assign_to_inv_cache(blocked_beat_key, blocked_position, working_beat_key, working_position, blocked_amount)
                }
                for ((blocked_beat_key, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
                    this.on_overlap(cache_key, Pair(blocked_beat_key.copy(), blocked_position.toList()))
                }
            } else {
                for (i in 0 until working_tree.size) {
                    stack.add(
                        Triple(
                            working_tree[i],
                            working_beat_key,
                            List(working_position.size + 1) { j: Int ->
                                if (j == working_position.size) {
                                    i
                                } else {
                                    working_position[j]
                                }
                            }
                        )
                    )
                }
            }
        }
    }

    internal fun get_leaf_offset_and_width(beat_key: BeatKey, position: List<Int>, mod_position: List<Int>? = null, mod_amount: Int = 0): Pair<Rational, Int> {
        /* use mod amount/mod_position to calculate size if a leaf were removed or added */

        var target_tree = this.get_tree(beat_key)
        var output = Rational(0, 1)
        var width_denominator = 1

        for (i in position.indices) {
            width_denominator *= if (mod_position != null && mod_position.size == i && position.subList(0, i) == mod_position) {
                target_tree.size + mod_amount
            } else {
                target_tree.size
            }

            val p = position[i]

            output += Rational(p, width_denominator)

            target_tree = target_tree[p]
        }
        output += beat_key.beat
        return Pair(output, width_denominator)
    }

    private fun recache_blocked_tree(beat_key: BeatKey, position: List<Int>) {
        val hash_key = Pair(beat_key, position)
        val (original_key, original_position, blocked_amount) = this._cache_inv_blocked_tree_map[hash_key] ?: return

        val tree = this.get_tree(beat_key, position)
        val chunk_amount = Rational(1, tree.size)
        for (i in 0 until tree.size) {
            val new_position = List(position.size + 1) { j: Int ->
                if (j == position.size) {
                    i
                } else {
                    position[j]
                }
            }

            var new_blocked_amount = (blocked_amount - (chunk_amount * (i + 1))) * tree.size
            new_blocked_amount = if (new_blocked_amount > 1) {
                Rational(1,1)
            } else {
                new_blocked_amount
            }

            this._cache_blocked_tree_map[Pair(original_key, original_position)]!!.add(Triple(
                beat_key,
                new_position,
                new_blocked_amount
            ))

            this._assign_to_inv_cache(beat_key, new_position, original_key, original_position, new_blocked_amount)

            this.on_overlap(Pair(original_key.copy(), original_position.toList()), Pair(beat_key.copy(), new_position))
        }
        // TODO: I think this is missing on_overlap_removed calls
    }

    private fun <T> recache_blocked_tree_wrapper(beat_key: BeatKey, position: List<Int>, callback: () -> T): T {
        val need_recache = mutableSetOf<Pair<BeatKey, List<Int>>>(Pair(beat_key, position))
        val tree = try {
            this.get_tree(beat_key, position)
        } catch (e: OpusTree.InvalidGetCall) {
            null
        }

        if (tree != null) {
            // Decache Existing overlap
            val stack = mutableListOf<Pair<OpusTree<*>, List<Int>>>(Pair(tree, position))
            while (stack.isNotEmpty()) {
                var (working_tree, working_position) = stack.removeFirst()
                if (working_tree.is_event()) {
                    this.decache_overlapping_leaf(beat_key, working_position)
                } else if (working_tree.is_leaf()) {
                    val block_key = Pair(beat_key, working_position)
                    if (this._cache_inv_blocked_tree_map.containsKey(block_key)) {
                        val (a,b,_) = this._cache_inv_blocked_tree_map[block_key]!!
                        this.decache_overlapping_leaf(a, b)
                        need_recache.add(Pair(a, b))
                    }
                } else {
                    for (i in 0 until working_tree.size) {
                        stack.add(Pair(working_tree[i], working_position + i))
                    }
                }
            }
        }

        val output = callback()

        for (needs_recache in need_recache) {
            this._cache_tree_overlaps(needs_recache.first, needs_recache.second)
        }

        return output
    }

    // -------------------------------------------------------

    private fun is_blocked_remove(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>>? {
        val blocker = this.get_blocking_position(beat_key, position) ?: return null

        val (blocker_key, blocker_position) = blocker
        var (next_beat_key, next_position) = this.get_proceding_event_position(beat_key, position) ?: return null

        val blocker_tree = this.get_tree(blocker_key, blocker_position)
        val (head_offset, head_width) = if (blocker_key == beat_key) {
            this.get_leaf_offset_and_width(blocker_key, blocker_position, position, -1)
        } else {
            this.get_leaf_offset_and_width(blocker_key, blocker_position)
        }

        val (target_offset, target_width) = if (next_beat_key == beat_key) {
            this.get_leaf_offset_and_width(next_beat_key, next_position, position, -1)
        } else {
            this.get_leaf_offset_and_width(next_beat_key, next_position)
        }

        return if (target_offset >= head_offset && target_offset < head_offset + Rational(blocker_tree.get_event()!!.duration, head_width)) {
            Pair(next_beat_key.copy(), next_position.toList())
        } else {
            null
        }
    }

    private fun is_blocked_set_event(beat_key: BeatKey, position: List<Int>, duration: Int): Pair<BeatKey, List<Int>>? {
        val blocker = this.get_blocking_position(beat_key, position)
        if (blocker != null) {
            println("SET BLOCKED!(A)")
            return blocker
        }

        var (next_beat_key, next_position) = this.get_proceding_event_position(beat_key, position) ?: return null

        val (head_offset, head_width) = this.get_leaf_offset_and_width(beat_key, position)
        val (target_offset, target_width) = this.get_leaf_offset_and_width(next_beat_key, next_position)
        return if (target_offset >= head_offset && target_offset < head_offset + Rational(duration, head_width)) {
            Pair(next_beat_key.copy(), next_position.toList())
        } else {
            null
        }
    }
}
