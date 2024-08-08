package com.qfs.pagan.opusmanager

import com.qfs.pagan.Rational
import com.qfs.pagan.structure.OpusTree
import kotlin.math.max

open class OpusLayerOverlapControl: OpusLayerBase() {
    class BlockedTreeException(beat_key: BeatKey, position: List<Int>, blocker_key: BeatKey, blocker_position: List<Int>): Exception("$beat_key | $position is blocked by event @ $blocker_key $blocker_position")

    private val _cache_blocked_tree_map = HashMap<Pair<BeatKey, List<Int>>, MutableList<Triple<BeatKey, List<Int>, Rational>>>()
    private val _cache_inv_blocked_tree_map = HashMap<Pair<BeatKey, List<Int>>, Triple<BeatKey, List<Int>, Rational>>()

    fun _blocked_tree_check(beat_key: BeatKey, position: List<Int>) {
        return
        val (blocker_key, blocker_position, amount) = this._cache_inv_blocked_tree_map[Pair(beat_key, position)] ?: return

        throw BlockedTreeException(beat_key, position, blocker_key, blocker_position)
    }

    fun is_tree_blocked(beat_key: BeatKey, position: List<Int>): Boolean {
        return this._cache_inv_blocked_tree_map.containsKey(Pair(beat_key, position))
    }

    override fun set_event(beat_key: BeatKey, position: List<Int>, event: InstrumentEvent) {
        this._blocked_tree_check(beat_key, position)

        super.set_event(beat_key, position, event)

        this.update_blocked_tree_cache(beat_key, position)
    }

    override fun insert(beat_key: BeatKey, position: List<Int>) {
        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.insert(beat_key, position)
        }
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.remove(beat_key, position)
        }
    }


    override fun remove_beat(beat_index: Int) {
        val decache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        for ((beat_key, position) in this._cache_blocked_tree_map.keys) {
            if (beat_key.beat == beat_index) {
                decache.add(Pair(beat_key, position))
            }
        }

        for (cache_key in decache) {
            for ((blocked_beat_key, blocked_position, _) in this._cache_blocked_tree_map.remove(cache_key)!!) {
                this._cache_inv_blocked_tree_map.remove(Pair(blocked_beat_key, blocked_position))
            }
        }
        decache.clear()

        for ((beat_key, position) in this._cache_blocked_tree_map.keys) {
            if (beat_key.beat > beat_index) {
                val new_beat_key = BeatKey(
                    beat_key.channel,
                    beat_key.line_offset,
                    beat_key.beat - 1
                )
                this._cache_blocked_tree_map[Pair(new_beat_key, position)] = this._cache_blocked_tree_map[Pair(beat_key, position)]!!
            }
        }

        val needs_recache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        for ((blocked_beat_key, blocked_position) in this._cache_inv_blocked_tree_map.keys) {
            if (blocked_beat_key.beat != beat_index) {
                continue
            }
            val (blocker_key, blocker_position, _) = this._cache_inv_blocked_tree_map[Pair(blocked_beat_key, blocked_position)]!!
            needs_recache.add(Pair(blocker_key, blocker_position))
        }

        super.remove_beat(beat_index)

        for ((blocker_key, blocker_position) in needs_recache) {
            this.update_blocked_tree_cache(blocker_key, blocker_position)
        }
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<InstrumentEvent>>?) {
        val needs_recache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        for ((blocked_beat_key, blocked_position) in this._cache_inv_blocked_tree_map.keys) {
            if (blocked_beat_key.beat != beat_index) {
                continue
            }
            val (blocker_key, blocker_position, _) = this._cache_inv_blocked_tree_map[Pair(blocked_beat_key, blocked_position)]!!
            needs_recache.add(Pair(blocker_key, blocker_position))
        }

        super.insert_beat(beat_index, beats_in_column)

        for (cache_key in needs_recache) {
            this.recache_blocked_tree(cache_key.first, cache_key.second)
        }

        val keys = this._cache_blocked_tree_map.keys.toList()
        for ((beat_key, position) in keys) {
            if (beat_key.beat <= beat_index) {
                continue
            }

            val new_beat_key = BeatKey(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat + 1
            )

            this._cache_blocked_tree_map[Pair(new_beat_key, position)] = this._cache_blocked_tree_map.remove(Pair(beat_key, position))!!
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

    // ----------------------------- Layer Specific functions ---------------------

    fun calculate_blocking_leafs(beat_key: BeatKey, position: List<Int>): MutableList<Triple<BeatKey, List<Int>, Rational>> {
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
            val adj_offset = next_offset + next_width

            if (end > adj_offset) {
                output.add(Triple(next_beat_key, next_position, Rational(1,1)))
            } else if (end > next_offset) {
                output.add(Triple(next_beat_key, next_position, (end - adj_offset) * -1))
                break
            } else {
                break
            }
        }
        return output
    }

    fun get_leaf_offset_and_width(beat_key: BeatKey, position: List<Int>): Pair<Rational, Int> {
        var target_tree = this.get_tree(beat_key)
        var output = Rational(0, 1)
        var width_denominator = 1

        for (p in position) {
            output = Rational(
                output.n * (target_tree.size * target_tree.size) + (output.d * p),
                output.d * (target_tree.size * target_tree.size)
            )
            width_denominator *= target_tree.size

            target_tree = target_tree[p]
        }

        output += beat_key.beat

        return Pair(output, width_denominator)
    }

    fun update_blocked_tree_cache(beat_key: BeatKey, position: List<Int>) {
        val cache_key = Pair(beat_key, position)

        if (this._cache_blocked_tree_map.containsKey(cache_key)) {
            for ((blocked_beat_key, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
                this._cache_inv_blocked_tree_map.remove(Pair(blocked_beat_key, blocked_position))
            }
        }

        this._cache_blocked_tree_map[cache_key] = this.calculate_blocking_leafs(beat_key, position)
        for ((blocked_beat_key, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
            this._cache_inv_blocked_tree_map[Pair(blocked_beat_key, blocked_position)] = Triple(beat_key, position, blocked_amount)
        }
    }

    fun recache_blocked_tree(beat_key: BeatKey, position: List<Int>) {
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
        }
    }

    private fun <T> recache_blocked_tree_wrapper(beat_key: BeatKey, position: List<Int>, callback: () -> T): T {
        val block_key = Pair(beat_key, position)
        val tree = this.get_tree(beat_key, position)

        val needs_recache: Pair<BeatKey, List<Int>>? = if (tree.is_event()) {
            Pair(beat_key, position)
        } else if (this._cache_inv_blocked_tree_map.containsKey(block_key)) {
            val (a,b,_) = this._cache_inv_blocked_tree_map[block_key]!!
            Pair(a, b)
        } else {
            null
        }

        if (needs_recache != null) {
            if (this._cache_blocked_tree_map.containsKey(needs_recache)) {
                for ((blocked_beat_key, blocked_position, blocked_amount) in this._cache_blocked_tree_map[needs_recache]!!) {
                    this._cache_inv_blocked_tree_map.remove(Pair(blocked_beat_key, blocked_position))
                }
            }
        }

        val output = callback()

        if (needs_recache != null) {
            val recache_position = needs_recache.second.toMutableList()
            while (true) {
                try {
                    this.get_tree(needs_recache.first, recache_position)
                    break
                } catch (e: OpusTree.InvalidGetCall) {
                    recache_position.removeLast()
                }
            }
            this.update_blocked_tree_cache(needs_recache.first, recache_position)
        }

        return output
    }
}
