package com.qfs.pagan.opusmanager

import com.qfs.pagan.Rational
import com.qfs.pagan.opusmanager.OpusLayerBase.BadInsertPosition
import com.qfs.pagan.opusmanager.OpusLayerBase.Companion.next_position
import com.qfs.pagan.structure.OpusTree

abstract class OpusTreeArray<T: OpusEvent>(var beats: MutableList<OpusTree<T>>) {
    class BlockedTreeException(var beat: Int, var position: List<Int>, var blocker_beat: Int, var blocker_position: List<Int>): Exception("$beat | $position is blocked by event @ $blocker_beat $blocker_position")
    private val _cache_blocked_tree_map = HashMap<Pair<Int, List<Int>>, MutableList<Triple<Int, List<Int>, Rational>>>()
    private val _cache_inv_blocked_tree_map = HashMap<Pair<Int, List<Int>>, Triple<Int, List<Int>, Rational>>()

    var flag_ignore_blocking = false

    var overlap_callback: ((Pair<Int, List<Int>>, Pair<Int, List<Int>>) -> Unit)? = null
    var overlap_removed_callback: ((Pair<Int, List<Int>>, Pair<Int, List<Int>>) -> Unit)? = null

    init {
        this.init_blocked_tree_caches()
    }

    open fun insert_beat(index: Int) {
        this.beats.add(index, OpusTree())
        val new_cache_pairs = mutableListOf<Pair<Pair<Int, List<Int>>, MutableList<Triple<Int, List<Int>, Rational>>>>()
        val need_recache = mutableListOf<Pair<Int, List<Int>>>()
        for ((overlapper, overlapped) in this._cache_blocked_tree_map) {
            if (overlapper.first >= index) {
                val adj_overlapped = mutableListOf<Triple<Int, List<Int>, Rational>>()
                for ((beat, position, amount) in overlapped) {
                    adj_overlapped.add(
                        Triple(
                            beat + 1,
                            position,
                            amount
                        )
                    )
                }

                new_cache_pairs.add(
                    Pair(
                        Pair(
                            overlapper.first + 1,
                            overlapper.second
                        ),
                        adj_overlapped
                    )
                )
            } else {
                val adj_overlapped = mutableListOf<Triple<Int, List<Int>, Rational>>()
                var crossed_index = false
                for ((beat, position, amount) in overlapped) {
                    if (beat >= overlapper.first) {
                        crossed_index = true
                        break
                    }
                }
                if (crossed_index) {
                    need_recache.add(overlapper)
                } else {
                    new_cache_pairs.add(Pair(overlapper, overlapped))
                }
            }
        }
        this._clear_block_caches()
        for ((key, value) in new_cache_pairs) {
            this._cache_blocked_tree_map[key] = value
            for ((beat, position, amount) in value) {
                this._cache_inv_blocked_tree_map[Pair(beat, position.toList())] = Triple(key.first, key.second.toList(), amount)
            }
        }
        for (key in need_recache) {
            this.cache_tree_overlaps(key.first, key.second)
        }
    }

    open fun set_beat_count(new_beat_count: Int) {
        val original_size = this.beats.size
        if (new_beat_count > original_size) {
            for (i in original_size until new_beat_count) {
                this.beats.add(OpusTree())
            }
        } else if (new_beat_count < original_size) {
            for (i in new_beat_count until original_size) {
                this.beats.removeAt(this.beats.size - 1)
            }
        }
    }

    open fun remove_beat(index: Int, count: Int = 1) {
        //this.blocked_check_remove_beat_throw(index, count)

        val decache = mutableSetOf<Pair<Int, List<Int>>>()
        val needs_recache = mutableSetOf<Pair<Int, List<Int>>>()
        val needs_decrement = mutableListOf<Pair<Int, List<Int>>>()

        val del_range = (index until index + count)
        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            if (del_range.contains(head.first)) {
                decache.add(Pair(head.first, head.second))
            } else if (tail.first >= index + count && head.first <= index) {
                needs_recache.add(Pair(head.first, head.second))
            } else if (head.first >= index + count && !needs_decrement.contains(Pair(head.first, head.second))) {
                needs_decrement.add(Pair(head.first, head.second))
            }
        }

        for (cache_key in decache) {
            this._decache_overlapping_leaf(cache_key.first, cache_key.second)
        }

        decache.clear()
        for (i in 0 until count) {
            this.beats.removeAt(index)
        }

        val new_cache = Array(needs_decrement.size) { i: Int ->
            val original_blocked = this._cache_blocked_tree_map.remove(needs_decrement[i])!!
            var (beat, position) = needs_decrement[i]
            val new_beat = beat - count

            Pair(
                Pair(new_beat, position.toList()),
                MutableList(original_blocked.size) { j: Int ->
                    this._cache_inv_blocked_tree_map.remove(
                        Pair(
                            original_blocked[j].first,
                            original_blocked[j].second
                        )
                    )

                    Triple(
                        original_blocked[j].first - count,
                        original_blocked[j].second.toList(),
                        original_blocked[j].third.copy()
                    )
                }
            )
        }

        for ((cache_key, blocked_map) in new_cache) {
            for ((working_beat, working_position, amount) in blocked_map) {
                this._assign_to_inv_cache(working_beat, working_position, cache_key.first, cache_key.second, amount)
            }
            this._cache_blocked_tree_map[cache_key] = blocked_map
        }

        for ((blocker_key, blocker_position) in needs_recache) {
            this._decache_overlapping_leaf(blocker_key, blocker_position)
            this.cache_tree_overlaps(blocker_key, blocker_position)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is OpusLineAbstract<*> || this.beats.size != other.beats.size) {
            return false
        }

        for (i in 0 until this.beats.size) {
            if (this.beats[i] != other.beats[i]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var output = this.javaClass.hashCode()
        for (i in 0 until this.beats.size) {
            // Circular Shift
            output = (output shl 1).xor(this.beats[i].hashCode()) + (output shr 31)
        }

        return output
    }

    private fun _assign_to_inv_cache(beat: Int, position: List<Int>, blocker: Int, blocker_position: List<Int>, amount: Rational) {
        this._cache_inv_blocked_tree_map[Pair(beat, position.toList())] = Triple(blocker, blocker_position.toList(), amount)
    }

    private fun _calculate_blocking_leafs(beat: Int, position: List<Int>): MutableList<Triple<Int, List<Int>, Rational>> {
        val (target_offset, target_width) = this.get_leaf_offset_and_width(beat, position)
        val target_tree = this.get_tree(beat, position)

        if (!target_tree.is_event() || target_tree.get_event()!!.duration == 1) {
            return mutableListOf()
        }

        val duration_width = Rational(target_tree.get_event()!!.duration, target_width)

        var next_beat = beat
        var next_position = position

        val output = mutableListOf<Triple<Int, List<Int>, Rational>>() // BeatKey, Position, Width
        val end = target_offset + duration_width

        while (true) {
            val next = this.get_proceding_leaf_position(next_beat, next_position) ?: break
            next_beat = next.first
            next_position = next.second

            val (next_offset, next_width) = this.get_leaf_offset_and_width(next_beat, next_position)
            val adj_offset = next_offset + Rational(1, next_width)
            if (end >= adj_offset) {
                output.add(
                    Triple(
                        next_beat,
                        next_position,
                        Rational(1, 1)
                    )
                )
                if (end == adj_offset) {
                    break
                }
            } else if (end > next_offset) {
                val new_amt = (end - adj_offset) * -1
                output.add(Triple(next_beat, next_position, new_amt))
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
        if (next_beat == this.beats.size - 1) {
            next_position = listOf()
            next_beat += 1
            while (true) {
                if (end > next_beat + 1) {
                    output.add(Triple(next_beat, next_position, Rational(1, 1)))
                } else if (end > next_beat) {
                    output.add(Triple(next_beat, next_position, (Rational(next_beat, 1) - end) * -1))
                    break
                } else {
                    break
                }
                next_beat += 1
            }
        }

        return output
    }

    /* Check if a subdivision can be removed without causing an overlap */
    private fun is_blocked_remove(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        val blocker = this.get_blocking_position(beat, position) ?: return null

        val (blocker_beat, blocker_position) = blocker
        val (next_beat, next_position) = this.get_proceding_event_position(blocker_beat, blocker_position) ?: return null
        val blocker_tree = this.get_tree(blocker_beat, blocker_position)

        val (head_offset, head_width) = if (blocker_beat == beat) {
            this.get_leaf_offset_and_width(blocker_beat, blocker_position, position, -1)
        } else {
            this.get_leaf_offset_and_width(blocker_beat, blocker_position)
        }

        val (target_offset, _) = if (next_beat == beat) {
            this.get_leaf_offset_and_width(next_beat, next_position, position, -1)
        } else {
            this.get_leaf_offset_and_width(next_beat, next_position)
        }

        return if (target_offset >= head_offset && target_offset < head_offset + Rational(blocker_tree.get_event()!!.duration, head_width)) {
            Pair(next_beat, next_position.toList())
        } else {
            null
        }
    }

    private fun is_blocked_insert(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        // NOTE: No real need to check position isn't empty since insert() *shouldn't* ever be called at that position
        val parent_position = position.subList(0, position.size - 1)
        val blocker = this.get_blocking_position(beat, parent_position) ?: return null
        val (blocker_beat, blocker_position) = blocker
        val (next_beat, next_position) = this.get_proceding_event_position(blocker_beat, blocker_position) ?: return null
        val blocker_tree = this.get_tree(blocker_beat, blocker_position)

        val (head_offset, head_width) = this.get_leaf_offset_and_width(blocker.first, blocker.second)
        val (target_offset) = if (next_beat == beat) {
            this.get_leaf_offset_and_width(next_beat, next_position, position, 1)
        } else {
            this.get_leaf_offset_and_width(next_beat, next_position)
        }
        return if (target_offset >= head_offset && target_offset < head_offset + Rational(blocker_tree.get_event()!!.duration, head_width)) {
            Pair(next_beat, next_position.toList())
        } else {
            null
        }

    }

    /* Check if setting an event would cause overlap */
    private fun is_blocked_set_event(beat: Int, position: List<Int>, duration: Int): Pair<Int, List<Int>>? {
        val blocker = this.get_blocking_position(beat, position)
        if (blocker != null) {
            return blocker
        }

        val (next_beat, next_position) = this.get_proceding_event_position(beat, position) ?: return null

        val (head_offset, head_width) = this.get_leaf_offset_and_width(beat, position)
        val (target_offset, target_width) = this.get_leaf_offset_and_width(next_beat, next_position)
        return if (target_offset >= head_offset && target_offset < head_offset + Rational(duration, head_width)) {
            Pair(next_beat, next_position.toList())
        } else {
            null
        }
    }

    /* Check if a beat can be removed without causing an overlap */
    private fun _is_blocked_remove_beat(beat_index: Int, count: Int = 1): Pair<Pair<Int, List<Int>>, Pair<Int, List<Int>>>? {
        val needs_recache = mutableSetOf<Pair<Int, List<Int>>>()

        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            if (tail.first >= beat_index && head.first < beat_index) {
                needs_recache.add(Pair(head.first, head.second))
            }
        }

        if (beat_index < this.beats.size - 1) {
            for (before in needs_recache) {
                var working_beat = beat_index + 1
                var working_position = this.get_first_position(working_beat)

                if (!this.get_tree(working_beat, working_position).is_event()) {
                    val next = this.get_proceding_event_position(working_beat, working_position) ?: continue
                    working_beat = next.first
                    working_position = next.second
                }

                val (before_offset, before_width) = this.get_leaf_offset_and_width(before.first, before.second)
                var duration = this.get_tree(before.first, before.second).get_event()?.duration ?: 1

                var (after_offset, _) = this.get_leaf_offset_and_width(working_beat, working_position)
                after_offset -= count

                if (after_offset >= before_offset && after_offset < before_offset + Rational(duration, before_width)) {
                    return Pair(Pair(working_beat, working_position), before)
                }
            }
        }

        return null
    }

    /* Check if replacing a tree would cause overlap */
    private fun <T: OpusEvent> is_blocked_replace_tree(beat: Int, position: List<Int>, new_tree: OpusTree<T>): Pair<Int, List<Int>>? {
        val original_position = this.get_blocking_position(beat, position) ?: Pair(beat, position)
        if (original_position != Pair(beat, position) && !new_tree.is_eventless()) {
            val (head_offset, head_width) = this.get_leaf_offset_and_width(original_position.first, original_position.second)
            val tail_end = head_offset + Rational(this.get_tree(original_position.first, original_position.second).get_event()!!.duration, head_width)
            val stack = mutableListOf(Triple(Rational(beat,1), 1, new_tree))
            while (stack.isNotEmpty()) {
                val (working_position, working_width, working_tree) = stack.removeAt(0)
                if (working_tree.is_event()) {
                    if (head_offset <= working_position && working_position < tail_end) {
                        return original_position
                    }
                } else if (working_tree.is_leaf()) {
                    continue
                } else {
                    val new_width = working_tree.size * working_width
                    for ((o, child) in working_tree.divisions) {
                        stack.add(
                            Triple(
                                working_position + Rational(o, new_width),
                                new_width,
                                child
                            )
                        )
                    }
                }
            }
        }

        val (next_beat, next_position) = this.get_proceding_event_position(beat, position) ?: return null

        val (target_offset, target_width) = this.get_leaf_offset_and_width(next_beat, next_position)

        val (direct_offset, direct_width) = this.get_leaf_offset_and_width(beat, position)
        val stack = mutableListOf<Triple<Rational, Int, OpusTree<T>>>(Triple(direct_offset, direct_width, new_tree))
        while (stack.isNotEmpty()) {
            val (working_offset, working_width, working_tree) = stack.removeAt(0)
            if (working_tree.is_event()) {
                if (target_offset >= working_offset && target_offset < working_offset + Rational(working_tree.get_event()!!.duration, working_width)) {
                    return Pair(next_beat, next_position.toList())
                }
                // CHECK
            } else if (!working_tree.is_leaf()) {
                val new_width = working_width * working_tree.size
                for ((i, subtree) in working_tree.divisions) {
                    stack.add(
                        Triple(
                            direct_offset + Rational(i, new_width),
                            new_width,
                            subtree
                        )
                    )
                }
            }
        }

        return null
    }

    private fun _on_overlap_removed(blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>>) {
        if (this.overlap_removed_callback != null && this._tree_exists(blocked.first, blocked.second)) {
            this.overlap_removed_callback!!(blocker, blocked)
        }
    }

    private fun _on_overlap(blocker: Pair<Int, List<Int>>, blocked: Pair<Int, List<Int>>) {
        if (this.overlap_callback != null && this._tree_exists(blocked.first, blocked.second)) {
            this.overlap_callback!!(blocker, blocked)
        }
    }

    private fun _clear_block_caches() {
        this._cache_blocked_tree_map.clear()
        this._cache_inv_blocked_tree_map.clear()
    }

    private fun _tree_exists(beat: Int, position: List<Int>): Boolean {
        return try {
            this.get_tree(beat, position)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun _decache_overlapping_leaf(beat: Int, position: List<Int>): List<Pair<Int, List<Int>>> {
        val cache_key = Pair(beat, position)
        val output: MutableList<Pair<Int, List<Int>>> = mutableListOf()
        val blocker = this._cache_inv_blocked_tree_map.remove(Pair(beat, position))
        if (blocker != null) {
            output.add(Pair(blocker.first, blocker.second))
        }

        val tree = try {
            this.get_tree(beat, position)
        } catch (e: OpusTree.InvalidGetCall) {
            return output
        }

        if (!tree.is_leaf()) {
            for (i in 0 until tree.size) {
                output.addAll(
                    this._decache_overlapping_leaf(beat, next_position(position, i))
                )
            }
        }

        if (!this._cache_blocked_tree_map.containsKey(cache_key)) {
            return output
        }

        output.add(Pair(beat, position))

        for ((blocked_beat_key, blocked_position, _) in this._cache_blocked_tree_map.remove(cache_key)!!) {
            val overlapped_key = Pair(blocked_beat_key, blocked_position)
            this._cache_inv_blocked_tree_map.remove(overlapped_key)

            this._on_overlap_removed(cache_key, overlapped_key)
        }

        return output
    }

    fun init_blocked_tree_caches() {
        this._clear_block_caches()
        if (this.beats.isEmpty()) {
            return
        }

        var beat = 0
        var position = this.get_first_position(beat, listOf())
        while (true) {
            val working_tree = this.get_tree(beat, position)
            if (working_tree.is_event()) {
                this.cache_tree_overlaps(beat, position)
            }

            val pair = this.get_proceding_leaf_position(beat, position) ?: break
            beat = pair.first
            position = pair.second
        }
    }

    fun get_first_position(beat: Int, start_position: List<Int>? = null): List<Int> {
        val output = start_position?.toMutableList() ?: mutableListOf()
        var tree = this.get_tree(beat, output)
        while (! tree.is_leaf()) {
            output.add(0)
            tree = tree[0]
        }
        return output
    }

    fun get_latest_event(beat: Int, position: List<Int>): T? {
        var current_tree = this.get_tree(beat)
        val adj_position = mutableListOf<Int>()
        for (p in position) {
            // Allow invalid positions. we only need to event that *would* be here.
            if (current_tree.is_leaf() || p >= current_tree.size) {
                break
            }
            adj_position.add(p)
            current_tree = current_tree[p]
        }

        if (current_tree.is_event()) {
            return current_tree.get_event()!!
        }

        var working_beat = beat
        var working_position = adj_position.toList()
        var output: T? = null

        while (true) {
            val pair = this.get_preceding_leaf_position(working_beat, working_position) ?: return output
            working_beat = pair.first
            working_position = pair.second

            val working_tree = this.get_tree(working_beat, working_position).copy()
            if (working_tree.is_event()) {
                val working_event = working_tree.get_event()!!
                output = working_event.copy() as T
                break
            }
        }
        return output
    }

    fun cache_tree_overlaps(beat: Int, position: List<Int>) {
        val tree = this.get_tree(beat, position)
        val stack = mutableListOf<Triple<OpusTree<*>, Int, List<Int>>>(Triple(tree, beat, position))
        while (stack.isNotEmpty()) {
            val (working_tree, working_beat, working_position) = stack.removeAt(0)
            if (working_tree.is_leaf()) {
                val cache_key = Pair(working_beat, working_position.toList())
                this._cache_blocked_tree_map[cache_key] = this._calculate_blocking_leafs(working_beat, working_position)

                for ((blocked_beat, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
                    this._assign_to_inv_cache(blocked_beat, blocked_position, working_beat, working_position, blocked_amount)
                }
                for ((blocked_beat, blocked_position, blocked_amount) in this._cache_blocked_tree_map[cache_key]!!) {
                    val overlappee_pair = Pair(blocked_beat, blocked_position.toList())
                    this._on_overlap(cache_key, overlappee_pair)
                }
            } else {
                for (i in 0 until working_tree.size) {
                    stack.add(
                        Triple(
                            working_tree[i],
                            working_beat,
                            next_position(working_position, i)
                        )
                    )
                }
            }
        }
    }

    fun squish(factor: Int) {
        val new_beats = mutableListOf<OpusTree<T>>()
        for (b in 0 until this.beats.size) {
            if (b % factor == 0) {
                new_beats.add(OpusTree<T>())
            }
            val working_beat = new_beats.last()
            working_beat.insert(b % factor, this.beats[b])
        }

        if (this.beats.size % factor != 0) {
            while (new_beats.last().size < factor) {
                new_beats.last().insert(
                    new_beats.last().size,
                    OpusTree()
                )
            }
        }

        for (beat in new_beats) {
            var is_empty = true

            for (i in 0 until beat.size) {
                if (!(beat[i].is_leaf() && beat[i].is_eventless())) {
                    is_empty = false
                    break
                }
            }

            if (is_empty) {
                beat.set_size(0)
            }
        }
        this.beats = new_beats
    }

    fun get_tree(beat: Int, position: List<Int>? = null): OpusTree<T> {
        var tree = this.beats[beat]
        if (position != null) {
            for (i in position) {
                tree = tree[i]
            }
        }

        return tree
    }

    fun replace_tree(beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        val old_tree = this.get_tree(beat, position)
        if (old_tree == tree) {
            return // Don't waste the cycles
        }

        val working_position = position ?: listOf()
        val overlapper = this.get_blocking_position(beat, working_position)
        val tree_is_eventless = tree.is_eventless()

        val blocker_pair = this.is_blocked_replace_tree(beat, working_position, tree)
        if (blocker_pair != null && !tree_is_eventless) {
            throw BlockedTreeException(beat, working_position, blocker_pair.first, blocker_pair.second)
        }
        this._decache_overlapping_leaf(beat, working_position)

        // -------------------------------
        if (tree.parent != null) {
            tree.detach()
        }

        if (old_tree.parent != null) {
            old_tree.replace_with(tree)
        } else {
            tree.parent = null
        }

        if (position?.isEmpty() != false) {
            this.beats[beat] = tree
        }
        // -------------------------------

        if (overlapper != null) {
            this.cache_tree_overlaps(overlapper.first, overlapper.second)
        }

        this.cache_tree_overlaps(beat, working_position)
    }

    fun split_tree(beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        this.recache_blocked_tree_wrapper(beat, position) {
            val tree = this.get_tree(beat, position)
            if (tree.is_event()) {
                var working_tree = tree
                val event = working_tree.get_event()!!

                working_tree.unset_event()
                working_tree.set_size(splits)

                if (splits > 1) {
                    working_tree = if (move_event_to_end) {
                        working_tree[working_tree.size - 1]
                    } else {
                        working_tree[0]
                    }
                }

                working_tree.set_event(event)
            } else {
                tree.set_size(splits)
            }
        }
    }

    fun get_proceding_leaf_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        var working_position = position.toMutableList()
        var working_beat = beat

        var working_tree = this.get_tree(working_beat, working_position)

        // Move right/up
        while (true) {
            if (working_tree.parent != null) {
                if (working_tree.parent!!.size - 1 > working_position.last()) {
                    working_position[working_position.size - 1] += 1
                    working_tree = this.get_tree(working_beat, working_position)
                    break
                } else {
                    working_position.removeAt(working_position.size - 1)
                    working_tree = working_tree.parent!!
                }
            } else if (working_beat < this.beats.size - 1) {
                working_beat += 1
                working_position = mutableListOf()
                working_tree = this.get_tree(working_beat, working_position)
                break
            } else {
                return null
            }
        }
        // Move left/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(0)
            working_tree = working_tree[0]
        }
        return Pair(working_beat, working_position)
    }

    fun get_preceding_leaf_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        val working_position = position.toMutableList()
        var working_beat = beat

        // Move left/up
        while (true) {
            if (working_position.isNotEmpty()) {
                if (working_position.last() > 0) {
                    working_position[working_position.size - 1] -= 1
                    break
                } else {
                    working_position.removeAt(working_position.size - 1)
                }
            } else if (working_beat > 0) {
                working_beat -= 1
                break
            } else {
                return null
            }
        }

        var working_tree = this.get_tree(working_beat, working_position)

        // Move right/down to leaf
        while (!working_tree.is_leaf()) {
            working_position.add(working_tree.size - 1)
            working_tree = working_tree[working_tree.size - 1]
        }

        return Pair(working_beat, working_position)
    }

    fun beat_count(): Int {
        return this.beats.size
    }

    fun get_leaf_offset_and_width(beat: Int, position: List<Int>, mod_position: List<Int>? = null, mod_amount: Int = 0): Pair<Rational, Int> {
        /* use mod amount/mod_position to calculate size if a leaf were removed or added */
        var beat_tree = this.get_tree(beat).copy()
        var working_tree = beat_tree
        val adj_position = Array(position.size) { position[it] }
        if (mod_position != null && mod_amount != 0) {
            for (p in mod_position.subList(0, mod_position.size - 1)) {
                working_tree = working_tree[p]
            }

            val last_p = mod_position.last()
            if (mod_position.subList(0, mod_position.size - 1) == position.subList(0, mod_position.size - 1)) {
                if (last_p <= position[mod_position.size - 1]) {
                    adj_position[mod_position.size - 1] += mod_amount
                }

                if (mod_amount == -1) {
                    working_tree[last_p].detach()
                } else if (mod_amount == 1) {
                    working_tree.insert(last_p, OpusTree())
                }
            }

            working_tree = beat_tree
        }

        var output = Rational(0, 1)
        var width_denominator = 1
        for (i in adj_position.indices) {
            var p = adj_position[i]
            var new_width_factor = working_tree.size
            width_denominator *= new_width_factor
            output += Rational(p, width_denominator)
            working_tree = working_tree[adj_position[i]]
        }

        output += beat
        return Pair(output, width_denominator)
    }

    fun <T> recache_blocked_tree_wrapper(beat: Int, position: List<Int>, callback: () -> T): T {
        if (this.flag_ignore_blocking) {
            return callback()
        }

        val need_recache = mutableSetOf(beat)

        val tree = try {
            this.get_tree(beat, position)
        } catch (e: OpusTree.InvalidGetCall) {
            null
        }

        if (tree != null) {
            // Decache Existing overlap
            val stack = mutableListOf<Pair<OpusTree<*>, List<Int>>>(Pair(tree, position))
            while (stack.isNotEmpty()) {
                val (working_tree, working_position) = stack.removeAt(0)
                if (working_tree.is_event()) {
                    for (pair in this._decache_overlapping_leaf(beat, working_position)) {
                        need_recache.add(pair.first)
                    }
                } else if (working_tree.is_leaf()) {
                    val block_key = Pair(beat, working_position)
                    if (this._cache_inv_blocked_tree_map.containsKey(block_key)) {
                        val (a,b,_) = this._cache_inv_blocked_tree_map[block_key]!!
                        need_recache.add(a)
                        for (pair in this._decache_overlapping_leaf(a, listOf())) {
                            need_recache.add(pair.first)
                        }
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
            this.cache_tree_overlaps(needs_recache, listOf())
        }

        return output
    }

    fun insert_after(beat: Int, position: List<Int>) {
        val check_position = position.subList(0, position.size - 1) + listOf(position.last() + 1)
        val blocked_pair = this.is_blocked_insert(beat, check_position)
        if (blocked_pair != null) {
            throw BlockedTreeException(beat, check_position, blocked_pair.first, blocked_pair.second)
        }

        val parent_position = if (position.isNotEmpty()) {
            position.subList(0,  position.size - 1)
        } else {
            position
        }

        this.recache_blocked_tree_wrapper(beat, parent_position) {
            val parent = this.get_tree(beat, parent_position)
            val index = position.last()
            parent.insert(index + 1)
        }
    }

    fun insert(beat: Int, position: List<Int>) {
        if (position.isEmpty()) {
            throw BadInsertPosition()
        }

        val blocked_pair = this.is_blocked_insert(beat, position)
        if (blocked_pair != null) {
            throw BlockedTreeException(beat, position, blocked_pair.first, blocked_pair.second)
        }

        val parent_position = position.subList(0, position.size - 1)
        this.recache_blocked_tree_wrapper(beat, position) {
            val tree = this.get_tree(beat, parent_position)

            val index = position.last()
            if (index > tree.size) {
                throw BadInsertPosition()
            }
            tree.insert(index)
        }
    }

    fun unset(beat: Int, position: List<Int>) {
        val overlap = this.get_blocking_position(beat, position)
        this._decache_overlapping_leaf(beat, position)

        val tree = this.get_tree(beat, position)
        tree.unset_event()

        if (tree.parent != null) {
            val index = tree.get_index()
            tree.parent!!.divisions.remove(index)
        }

        if (overlap != null) {
            this.cache_tree_overlaps(overlap.first, overlap.second)
        }
    }

    fun get_blocking_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        val tree = this.get_tree(beat, position)
        val stack = mutableListOf(Pair(tree, position))

        while (stack.isNotEmpty()) {
            val (working_tree, working_position) = stack.removeAt(0)
            if (working_tree.is_leaf()) {
                if (this._cache_inv_blocked_tree_map.containsKey(Pair(beat, working_position))) {
                    val entry = this._cache_inv_blocked_tree_map[Pair(beat, working_position)]!!
                    if (entry.first == beat && position.size < entry.second.size && entry.second.subList(0, position.size) == position) {
                        continue
                    }
                    return Pair(entry.first, entry.second.toList())
                }
            } else {
                for (i in 0 until working_tree.size) {
                    stack.add(
                        Pair(working_tree[i], next_position(working_position, i))
                    )
                }
            }
        }
        return null
    }

    // Get all blocked positions connected, regardless of if the given position is an event or a blocked tree
    fun get_all_blocked_positions(beat: Int, position: List<Int>): List<Pair<Int, List<Int>>> {
        val original = this.get_blocking_position(beat, position) ?: Pair(beat, position)
        val output = mutableListOf(original)
        val blocked_trees = this._cache_blocked_tree_map[original] ?: listOf()
        for ((blocked_beat, blocked_position, _) in blocked_trees) {
            output.add(Pair(blocked_beat, blocked_position.toList()))
        }
        return output
    }

    fun get_proceding_event_position(beat: Int, position: List<Int>): Pair<Int, List<Int>>? {
        val next = this.get_proceding_leaf_position(beat, position) ?: return null
        var working_beat = next.first
        var working_position = next.second
        var found_position: Pair<Int, List<Int>>? = null
        while (found_position == null) {
            val working_tree = this.get_tree(working_beat, working_position)
            if (working_tree.is_event()) {
                found_position = Pair(working_beat, working_position)
            } else {
                val tmp = this.get_proceding_leaf_position(working_beat, working_position) ?: break
                working_beat = tmp.first
                working_position = tmp.second
            }
        }
        return if (found_position != null) {
            Pair(working_beat, working_position)
        } else {
            null
        }
    }

    /* Throw an error if beat_index can't be removed without overlap */
    fun blocked_check_remove_beat_throw(beat_index: Int, count: Int = 1) {
        val result = this._is_blocked_remove_beat(beat_index, count) ?: return
        throw BlockedTreeException(result.first.first, result.first.second, result.second.first, result.second.second)
    }

    fun remove_node(beat: Int, position: List<Int>) {
        val blocked_pair = this.is_blocked_remove(beat, position)
        if (blocked_pair != null) {
            throw BlockedTreeException(beat, position, blocked_pair.first, blocked_pair.second)
        }
        // Check that removing this leaf won't cause proceding events to overlap
        var working_beat = beat
        var working_position = position
        var next_position_pair = this.get_proceding_event_position(working_beat, working_position)
        if (next_position_pair != null) {
            working_beat = next_position_pair.first
            working_position = next_position_pair.second
            while (working_beat == beat) {
                val (offset, width) = this.get_leaf_offset_and_width(working_beat, working_position, position, -1)
                val check_pair = this.get_proceding_event_position(working_beat, working_position) ?: break
                val (check_offset, _) = if (check_pair.first == working_beat) {
                    this.get_leaf_offset_and_width(check_pair.first, check_pair.second, position, -1)
                } else {
                    this.get_leaf_offset_and_width(check_pair.first, check_pair.second)
                }

                var duration = this.get_tree(working_beat, working_position).get_event()?.duration ?: 1

                if (check_offset >= offset && check_offset < offset + Rational(duration, width)) {
                    throw BlockedTreeException(working_beat, working_position, check_pair.first, check_pair.second)
                }

                next_position_pair = this.get_proceding_event_position(working_beat, working_position) ?: break
                working_beat = next_position_pair.first
                working_position = next_position_pair.second

            }
        }

        this.recache_blocked_tree_wrapper(beat, position.subList(0, position.size - 1)) {
            val tree = this.get_tree(beat, position)
            tree.detach()
        }
    }

    fun get_blocking_amount(beat: Int, position: List<Int>): Rational? {
        val tree = this.get_tree(beat, position)
        val stack = mutableListOf(Pair(tree, position))

        while (stack.isNotEmpty()) {
            val (working_tree, working_position) = stack.removeAt(0)
            if (working_tree.is_leaf()) {
                if (this._cache_inv_blocked_tree_map.containsKey(Pair(beat, working_position))) {
                    val entry = this._cache_inv_blocked_tree_map[Pair(beat, working_position)]!!
                    return entry.third
                }
            } else {
                for (i in 0 until working_tree.size) {
                    stack.add(Pair(working_tree[i], next_position(position, i)))
                }
            }
        }
        return null
    }

    fun set_event(beat: Int, position: List<Int>, event: T) {
        val blocked_pair = this.is_blocked_set_event(beat, position, event.duration)
        if (blocked_pair != null) {
            throw BlockedTreeException(beat, position, blocked_pair.first, blocked_pair.second)
        }

        this.recache_blocked_tree_wrapper(beat, position) {
            val tree = this.get_tree(beat, position)
            tree.set_event(event)
        }
    }
}

