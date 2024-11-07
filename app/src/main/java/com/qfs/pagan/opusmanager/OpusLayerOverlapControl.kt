package com.qfs.pagan.opusmanager

import com.qfs.pagan.Rational

open class OpusLayerOverlapControl: OpusLayerBase() {
    class BlockedTreeException(beat_key: BeatKey, position: List<Int>, var blocker_key: BeatKey, var blocker_position: List<Int>): Exception("$beat_key | $position is blocked by event @ $blocker_key $blocker_position")

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
