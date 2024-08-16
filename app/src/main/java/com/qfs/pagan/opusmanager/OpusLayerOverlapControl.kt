package com.qfs.pagan.opusmanager

import com.qfs.pagan.Rational
import com.qfs.pagan.structure.OpusTree
import kotlin.math.max

open class OpusLayerOverlapControl: OpusLayerBase() {
    class BlockedTreeException(beat_key: BeatKey, position: List<Int>, blocker_key: BeatKey, blocker_position: List<Int>): Exception("$beat_key | $position is blocked by event @ $blocker_key $blocker_position")

    private val _cache_blocked_tree_map = HashMap<Pair<BeatKey, List<Int>>, MutableList<Triple<BeatKey, List<Int>, Rational>>>()
    private val _cache_inv_blocked_tree_map = HashMap<Pair<BeatKey, List<Int>>, Triple<BeatKey, List<Int>, Rational>>()

    private fun _init_blocked_tree_caches() {
        var channels = this.get_all_channels()
        for (i in 0 until channels.size) {
            for (j in 0 until channels[i].size) {
                val line = channels[i].lines[j]
                var beat_key = BeatKey(i, j, 0)
                var position = this.get_first_position(beat_key, listOf())
                while (true) {
                    var working_tree = this.get_tree(beat_key, position)
                    if (working_tree.is_event()) {
                        this.update_blocked_tree_cache(beat_key, position)
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

    fun is_tree_blocked(beat_key: BeatKey, position: List<Int>): Boolean {
        return this._cache_inv_blocked_tree_map.containsKey(Pair(beat_key, position))
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        this.recache_blocked_tree_wrapper(beat_key, position ?: listOf()) {
            super.replace_tree(beat_key, position, tree)
        }
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: InstrumentEvent) {
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

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.remove(beat_key, position)
        }
    }

    open fun decache_overlapping_leaf(beat_key: BeatKey, position: List<Int>) {
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
        val needs_decrement = mutableSetOf<Pair<BeatKey, List<Int>>>()
        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            if (head.first.beat == beat_index) {
                decache.add(Pair(head.first, head.second))
            } else if (tail.first.beat >= beat_index && head.first.beat < beat_index) {
                needs_recache.add(Pair(head.first, head.second))
            } else if (head.first.beat > beat_index) {
                needs_decrement.add(Pair(head.first, head.second))
            }
        }

        for (cache_key in decache) {
            this.decache_overlapping_leaf(cache_key.first, cache_key.second)
        }
        decache.clear()

        for ((blocked_beat_key, blocked_position) in this._cache_inv_blocked_tree_map.keys) {
            if (blocked_beat_key.beat != beat_index) {
                continue
            }
            val (blocker_key, blocker_position, _) = this._cache_inv_blocked_tree_map[Pair(blocked_beat_key, blocked_position)]!!
            needs_recache.add(Pair(blocker_key, blocker_position))
        }

        super.remove_beat(beat_index)

        for ((beat_key, position) in needs_decrement) {
            val original_blocked = this._cache_blocked_tree_map.remove(Pair(beat_key, position)) ?: continue // Already updated

            val new_beat_key = BeatKey(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat - 1
            )

            this._cache_blocked_tree_map[Pair(new_beat_key, position)] = MutableList(original_blocked.size) { i: Int ->
                Triple(
                    BeatKey(
                        original_blocked[i].first.channel,
                        original_blocked[i].first.line_offset,
                        original_blocked[i].first.beat - 1
                    ),
                    original_blocked[i].second,
                    original_blocked[i].third
                )
            }
        }

        for ((blocker_key, blocker_position) in needs_recache) {
            this.update_blocked_tree_cache(blocker_key, blocker_position)
        }


    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<InstrumentEvent>>?) {
        val needs_recache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        val needs_inc = mutableSetOf<Pair<BeatKey, List<Int>>>()
        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            if (tail.first.beat < beat_index) {
                continue
            } else if (head.first.beat <= beat_index) {
                needs_recache.add(Pair(head.first, head.second))
            } else {
                needs_inc.add(Pair(head.first, head.second))
            }
        }
        super.insert_beat(beat_index, beats_in_column)

        for ((beat_key, position) in needs_inc) {
            val original_blocked = this._cache_blocked_tree_map.remove(Pair(beat_key, position)) ?: continue // Already updated
            val new_beat_key = BeatKey(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat + 1
            )

            this._cache_blocked_tree_map[Pair(new_beat_key, position)] = MutableList(original_blocked.size) { i: Int ->
                Triple(
                    BeatKey(
                        original_blocked[i].first.channel,
                        original_blocked[i].first.line_offset,
                        original_blocked[i].first.beat + 1
                    ),
                    original_blocked[i].second,
                    original_blocked[i].third
                )
            }
        }

        for (cache_key in needs_recache) {
            this.update_blocked_tree_cache(cache_key.first, cache_key.second)
        }

    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.split_tree(beat_key, position, splits, move_event_to_end)
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        super.set_duration(beat_key, position, duration)
        this.update_blocked_tree_cache(beat_key, position)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this.update_blocked_tree_cache(beat_key, position)
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
        if (this._cache_blocked_tree_map.containsKey(original)) {
            for ((blocked_beat_key, blocked_position, _) in this._cache_blocked_tree_map[original]!!) {
                output.add(Pair(blocked_beat_key, blocked_position))
            }
        }
        return output
    }

    fun get_original_position(beat_key: BeatKey, position: List<Int>): Pair<BeatKey, List<Int>> {
        return if (!this.is_tree_blocked(beat_key, position)) {
            Pair(beat_key, position)
        } else {
            val entry = this._cache_inv_blocked_tree_map[Pair(beat_key, position)]!!
            Pair(entry.first, entry.second)
        }
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

    private fun update_blocked_tree_cache(beat_key: BeatKey, position: List<Int>) {
        this.decache_overlapping_leaf(beat_key, position)

        val cache_key = Pair(beat_key, position)

        this._cache_blocked_tree_map[cache_key] = this.calculate_blocking_leafs(beat_key, position)
        for ((blocked_beat_key, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
            this._cache_inv_blocked_tree_map[Pair(blocked_beat_key, blocked_position)] = Triple(beat_key, position, blocked_amount)
        }

        for ((blocked_beat_key, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
            this.on_overlap(cache_key, Pair(blocked_beat_key, blocked_position))
        }
    }

    internal fun get_leaf_offset_and_width(beat_key: BeatKey, position: List<Int>): Pair<Rational, Int> {
        var target_tree = this.get_tree(beat_key)
        var output = Rational(0, 1)
        var width_denominator = 1

        for (p in position) {
            width_denominator *= target_tree.size

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

            this._cache_inv_blocked_tree_map[Pair(beat_key, new_position)] = Triple(
                original_key,
                original_position,
                new_blocked_amount
            )
            this.on_overlap(Pair(original_key, original_position), Pair(beat_key, new_position))
        }
        // TODO: I think this is missing on_overlap_removed calls
    }

    private fun <T> recache_blocked_tree_wrapper(beat_key: BeatKey, position: List<Int>, callback: () -> T): T {
        val need_recaches = mutableListOf<Pair<BeatKey, List<Int>>>()
        val tree = try {
            this.get_tree(beat_key, position)
        } catch (e: OpusTree.InvalidGetCall) {
            null
        }

        if (tree != null) {
            val stack = mutableListOf<Pair<OpusTree<*>, List<Int>>>(Pair(tree, position))
            while (stack.isNotEmpty()) {
                var (working_tree, working_position) = stack.removeFirst()
                if (working_tree.is_event()) {
                    this.decache_overlapping_leaf(beat_key, working_position)
                    need_recaches.add(Pair(beat_key, working_position))
                } else if (working_tree.is_leaf()) {
                    val block_key = Pair(beat_key, working_position)
                    if (this._cache_inv_blocked_tree_map.containsKey(block_key)) {
                        val (a,b,_) = this._cache_inv_blocked_tree_map[block_key]!!
                        this.decache_overlapping_leaf(a, b)
                        need_recaches.add(Pair(a, b))
                    }
                } else {
                    for (i in 0 until working_tree.size) {
                        stack.add(Pair(working_tree[i], working_position + i))
                    }
                }
            }
        }

        val output = callback()
        for (needs_recache in need_recaches) {
            val recache_position = try {
                this.get_first_position(needs_recache.first, needs_recache.second)
            } catch (e: OpusTree.InvalidGetCall) {
                val new_position = mutableListOf<Int>()
                var working_tree = this.get_tree(beat_key)
                for (p in needs_recache.second) {
                    if (working_tree.is_leaf()) {
                        break
                    } else {
                        new_position.add(p)
                        working_tree = working_tree[p]
                    }
                }
                new_position
            }
            this.update_blocked_tree_cache(needs_recache.first, recache_position)
        }

        return output
    }
}
