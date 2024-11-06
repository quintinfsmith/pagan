package com.qfs.pagan.opusmanager

import com.qfs.pagan.Rational
import com.qfs.pagan.structure.OpusTree
import com.qfs.pagan.opusmanager.ControlEventType

open class OpusLayerOverlapControl: OpusLayerBase() {
    class BlockedTreeException(beat_key: BeatKey, position: List<Int>, var blocker_key: BeatKey, var blocker_position: List<Int>): Exception("$beat_key | $position is blocked by event @ $blocker_key $blocker_position")

    /* Insert extra lines to fit overlapping events (happens on import midi or old savve file versions) */
    private fun _reshape_lines_from_blocked_trees() {
        var channels = this.get_all_channels()
        for (i in 0 until channels.size) {
            val remap_trees = mutableListOf<Pair<Int, MutableList<Triple<BeatKey, List<Int>, Int>>>>() // BeatKey, Position, New Line Offset
            for (j in 0 until channels[i].size) {
                var beat_key = BeatKey(i, j, 0)
                var position = this.get_first_position(beat_key, listOf())

                if (!this.get_tree(beat_key, position).is_event()) {
                    val pair = this.get_proceding_event_position(beat_key, position) ?: continue
                    beat_key = BeatKey(i, j, pair.first)
                    position = pair.second
                }

                val current_remap = mutableListOf<Triple<BeatKey, List<Int>, Int>>()
                val overlap_lanes = mutableListOf<Rational?>()

                while (true) {
                    val (offset, width) = this.get_leaf_offset_and_width(beat_key, position)
                    val end_position = offset + Rational(this.get_tree(beat_key, position).get_event()!!.duration, width)

                    var lane_index = 0
                    while (lane_index < overlap_lanes.size) {
                        val check_position = overlap_lanes[lane_index]
                        if (check_position == null) {
                            break
                        } else if (check_position <= offset) {
                            overlap_lanes[lane_index] = null
                            break
                        }

                        lane_index += 1
                    }

                    if (lane_index == overlap_lanes.size) {
                        overlap_lanes.add(null)
                    }
                    overlap_lanes[lane_index] = end_position

                    if (lane_index != 0) {
                        current_remap.add(
                            Triple(
                                beat_key,
                                position,
                                lane_index
                            )
                        )
                    }

                    val pair = this.get_proceding_event_position(beat_key, position) ?: break
                    beat_key.beat = pair.first
                    position = pair.second
                }
                remap_trees.add(Pair(overlap_lanes.size, current_remap))
            }

            val working_channel = if (i < this.channels.size) {
                this.channels[i]
            } else {
                this.percussion_channel
            }

            for (j in remap_trees.size - 1 downTo 0) {
                val (lines_to_insert, remaps) = remap_trees[j]
                for (k in 0 until lines_to_insert - 1) {
                    this.new_line(i, j + 1)
                    for ((type, controller) in working_channel.lines[j].controllers.get_all()) {

                        working_channel.lines[j + 1].controllers.new_controller(type)
                        working_channel.lines[j + 1].controllers.get_controller<OpusControlEvent>(type).set_initial_event(
                            controller.initial_event.copy()
                        )
                    }
                    if (i == this.channels.size) {
                        this.set_percussion_instrument(
                            j + 1,
                            this.get_percussion_instrument(j)
                        )
                    }
                }

                val replaced_beat_keys = mutableSetOf<BeatKey>()
                for ((working_beat_key, working_position, new_index) in remaps) {
                    val new_key = BeatKey(
                        working_beat_key.channel,
                        working_beat_key.line_offset + new_index,
                        working_beat_key.beat
                    )

                    if (!replaced_beat_keys.contains(new_key)) {
                        val new_tree = this.get_tree(working_beat_key).copy() // TODO: Needs copy function

                        new_tree.traverse { working_tree: OpusTree<*>, event: InstrumentEvent? ->
                            if (event != null) {
                                working_tree.unset_event()
                            }
                        }

                        super.replace_tree(new_key, listOf(), new_tree)
                        replaced_beat_keys.add(new_key)
                    }

                    super.replace_tree(
                        new_key,
                        working_position,
                        this.get_tree_copy(working_beat_key, working_position)
                    )
                    super.unset(working_beat_key, working_position)
                }
            }
        }
    }

    override fun on_project_changed() {
        super.on_project_changed()
        this._reshape_lines_from_blocked_trees()
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        val parent_position = if (position.isNotEmpty()) {
            position.subList(0,  position.size - 1)
        } else {
            position
        }

        this.recache_blocked_tree_wrapper(beat_key, parent_position) {
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

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        val other_position = List(position.size) { i: Int -> 
            if (i == position.size - 1) {
                if (position[i] == 0) {
                    1
                } else {
                    0
                }
            } else {
                position[i]
            }
        }

        val blocked_pair = this.is_blocked_replace_tree(beat_key, position.subList(0, position.size - 1), this.get_tree_copy(beat_key, other_position))
        if (blocked_pair != null) {
            throw BlockedTreeException(beat_key, position, blocked_pair.first, blocked_pair.second)
        }

        super.remove_one_of_two(beat_key, position)
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        val decache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        val needs_recache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        val needs_decrement = mutableListOf<Pair<BeatKey, List<Int>>>()

        val del_range = (beat_index until beat_index + count)
        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            val head_key = BeatKey(
                tail.first.channel,
                tail.first.line_offset,
                head.first
            )
            if (del_range.contains(head.first)) {
                decache.add(Pair(head_key, head.second))
            } else if (tail.first.beat >= beat_index && head.first < beat_index) {
                needs_recache.add(Pair(head_key, head.second))
            } else if (head.first >= beat_index + count && !needs_decrement.contains(Pair(head_key, head.second))) {
                needs_decrement.add(Pair(head_key, head.second))
            }
        }

        if (beat_index < this.beat_count - count) {
            for (before in needs_recache) {
                var working_beat_key = BeatKey(before.first.channel, before.first.line_offset, beat_index + count)
                var working_position = this.get_first_position(working_beat_key)

                if (!this.get_tree(working_beat_key, working_position).is_event()) {
                    val next = this.get_proceding_event_position(working_beat_key, working_position) ?: continue
                    working_beat_key.beat = next.first
                    working_position = next.second
                }

                val (before_offset, before_width) = this.get_leaf_offset_and_width(before.first, before.second)
                var duration = this.get_tree(before.first, before.second).get_event()?.duration ?: 1

                var (after_offset, _) = this.get_leaf_offset_and_width(working_beat_key, working_position)
                after_offset -= count

                if (after_offset >= before_offset && after_offset < before_offset + Rational(duration, before_width)) {
                    throw BlockedTreeException(working_beat_key, working_position, before.first, before.second)
                }

                for (after in needs_decrement) {
                    if (!(before.first.channel == after.first.channel && before.first.line_offset == after.first.line_offset)) {
                        continue
                    }
                }
            }
        }

        for (cache_key in decache) {
            this.decache_overlapping_leaf(cache_key.first, cache_key.second)
        }

        decache.clear()

        super.remove_beat(beat_index, count)

        val new_cache = Array(needs_decrement.size) { i: Int ->
            val original_blocked = this._cache_blocked_tree_map.remove(needs_decrement[i])!!
            var (beat_key, position) = needs_decrement[i]

            val new_beat_key = BeatKey(
                beat_key.channel,
                beat_key.line_offset,
                beat_key.beat - count
            )

            Pair(
                Pair(new_beat_key, position.toList()),
                MutableList(original_blocked.size) { j: Int ->
                    this._cache_inv_blocked_tree_map.remove(
                        Pair(
                            BeatKey(
                                beat_key.channel,
                                beat_key.line_offset,
                                original_blocked[j].first,
                            ),
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
                val working_beat_key = BeatKey(
                    cache_key.first.channel,
                    cache_key.first.line_offset,
                    working_beat
                )
                this._assign_to_inv_cache(working_beat_key, working_position, cache_key.first, cache_key.second, amount)
            }
            this._cache_blocked_tree_map[cache_key] = blocked_map
        }

        for ((blocker_key, blocker_position) in needs_recache) {
            this.decache_overlapping_leaf(blocker_key, blocker_position)
            this._cache_tree_overlaps(blocker_key, blocker_position)
        }
    }


    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<InstrumentEvent>>?) {
        val needs_recache = mutableSetOf<Pair<BeatKey, List<Int>>>()
        val needs_inc = mutableListOf<Pair<BeatKey, List<Int>>>()

        for ((tail, head) in this._cache_inv_blocked_tree_map) {
            val head_key = BeatKey(
                tail.first.channel,
                tail.first.line_offset,
                head.first
            )
            if (tail.first.beat < beat_index) {
                continue
            } else if (head_key.beat < beat_index) {
                needs_recache.add(Pair(head_key, head.second))
            } else if (!needs_inc.contains(Pair(head_key, head.second))) {
                needs_inc.add(Pair(head_key, head.second))
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
                            BeatKey(
                                beat_key.channel,
                                beat_key.line_offset,
                                original_blocked[j].first,
                            ),
                            original_blocked[j].second
                        )
                    )

                    Triple(
                        original_blocked[j].first + 1,
                        original_blocked[j].second.toList(),
                        original_blocked[j].third.copy()
                    )
                }
            )
        }

        val all_channels = this.get_all_channels()
        var y = 0
        for (i in all_channels.indices) {
            val channel = all_channels[i]
            for (j in 0 until channel.lines.size) {
                val line = channel.lines[j]
                var working_beat_key = BeatKey(i, j, beat_index)
                var working_position = this.get_first_position(working_beat_key)
                while (true) {
                    val working_tree = this.get_tree(working_beat_key, working_position)
                    if (working_tree.is_event()) {
                        this._cache_tree_overlaps(working_beat_key, working_position)
                    }
                    val next_pair = this.get_proceding_event_position(working_beat_key, working_position) ?: break
                    if (next_pair.first != working_beat_key.beat) {
                        break
                    }
                    working_position = next_pair.second
                }
            }
        }

        for ((cache_key, blocked_map) in new_cache) {
            for ((working_beat, working_position, amount) in blocked_map) {
                val working_key = BeatKey(
                    cache_key.first.channel,
                    cache_key.first.line_offset,
                    working_beat
                )
                this._assign_to_inv_cache(working_key, working_position, cache_key.first, cache_key.second, amount)
            }
            this._cache_blocked_tree_map[cache_key] = blocked_map
        }

        for (cache_key in needs_recache) {
            this._cache_tree_overlaps(cache_key.first, cache_key.second)
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        val current_tree_position = this.get_original_position(beat_key, position)
        val current_event_tree = this.get_tree(current_tree_position.first, current_tree_position.second)
        val blocked_amount = this.get_blocking_amount(beat_key, position)
        if (current_event_tree != this.get_tree(beat_key, position) && blocked_amount!! >= 1) {
            throw BlockedTreeException(beat_key, position, current_tree_position.first, current_tree_position.second)
        }

        this.recache_blocked_tree_wrapper(beat_key, position) {
            super.split_tree(beat_key, position, splits, move_event_to_end)
        }
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        if (this.get_tree(beat_key, position).is_event()) {
            val blocked_pair = this.is_blocked_set_event(beat_key, position, duration)
            if (blocked_pair != null) {
                throw BlockedTreeException(beat_key, position, blocked_pair.first, blocked_pair.second)
            }
        }

        super.set_duration(beat_key, position, duration)

        this.decache_overlapping_leaf(beat_key, position)
        this._cache_tree_overlaps(beat_key, position)
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

    fun get_original_position(beat_key: BeatKey, position: List<Int>): Pair<Int, List<Int>> {
        return this.get_blocking_position(beat_key, position) ?: Pair(beat_key.beat, position)
    }
    fun get_original_position_global_ctl(ctl_type: ControlEventType, beat: Int, position: List<Int>): Pair<Int, List<Int>> {
        return this.get_blocking_position_global_ctl(ctl_type, beat, position) ?: Pair(beat, position)
    }
    fun get_original_position_channel_ctl(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>): Pair<Int, List<Int>> {
        return this.get_blocking_position_channel_ctl(ctl_type, channel, beat, position) ?: Pair(beat, position)
    }
    fun get_original_position_line_ctl(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>): Pair<Int, List<Int>> {
        return this.get_blocking_position_line_ctl(ctl_type, beat_key, position) ?: Pair(beat_key.beat, position)
    }

    // ----------------------------- Layer Specific functions ---------------------
    open fun decache_overlapping_leaf(beat_key: BeatKey, position: List<Int>): List<Pair<Int, List<Int>>> {
        return this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].decache_overlapping_leaf(beat_key.beat, position)
    }

    private fun calculate_blocking_leafs(beat_key: BeatKey, position: List<Int>): MutableList<Triple<Int, List<Int>, Rational>> {
        return this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].calculate_blocking_leafs(beat_key.beat, position)
    }

    /*
     * Wrapper around on_overlap that includes a check if the overlapped position exists.
     */
    private fun _on_overlap(overlapper: Pair<BeatKey, List<Int>>, overlappee: Pair<BeatKey, List<Int>>) {
        if (!this.is_valid(overlappee.first, overlappee.second)) {
            return
        }

        this.on_overlap(overlapper, overlappee)
    }
    /*
     * Wrapper around on_overlap_removed that includes a check if the overlapped position exists.
     */
    private fun _on_overlap_removed(overlapper: Pair<BeatKey, List<Int>>, overlappee: Pair<BeatKey, List<Int>>) {
        if (!this.is_valid(overlappee.first, overlappee.second)) {
            return
        }
        this.on_overlap_removed(overlapper, overlappee)
    }
    open fun on_overlap(overlapper: Pair<BeatKey, List<Int>>, overlappee: Pair<BeatKey, List<Int>>) { }
    open fun on_overlap_removed(overlapper: Pair<BeatKey, List<Int>>, overlappee: Pair<BeatKey, List<Int>>) { }

    private fun _cache_tree_overlaps(beat_key: BeatKey, position: List<Int>) {
        this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].cache_tree_overlaps(beat_key.beat, position)
    }

    private fun _cache_global_ctl_tree_overlaps(ctl_type: ControlEventType, beat: Int, position: List<Int>) {
        this.controllers.get_controller<OpusControlEvent>(ctl_type).cache_tree_overlaps(beat, position)
    }

    private fun _cache_channel_ctl_tree_overlaps(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        this.get_all_channels()[channel].controllers.get_controller<OpusControlEvent>(ctl_type).cache_tree_overlaps(beat, position)
    }

    private fun _cache_line_ctl_tree_overlaps(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        val channel = this.get_all_channels()[beat_key.channel]
        val line = channel.lines[beat_key.line_offset]
        val controller = line.controllers.get_controller<OpusControlEvent>(ctl_type)
        controller.cache_tree_overlaps(beat_key.beat, position)
    }

    private fun <T> recache_blocked_tree_wrapper(beat_key: BeatKey, position: List<Int>, callback: () -> T): T {
        return this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].recache_blocked_tree_wrapper(beat_key.beat, position, callback)
    }

    // -------------------------------------------------------


}
