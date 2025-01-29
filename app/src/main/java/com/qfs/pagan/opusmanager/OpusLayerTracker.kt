package com.qfs.pagan.opusmanager

import com.qfs.json.JSONHashMap

open class OpusLayerTracker: OpusLayerHistory() {
    private var _action_count: Int = 0
    private var tracker_queue = mutableListOf<Pair<TrackerToken, List<Int>>>()

    enum class TrackerToken {
        NewLine,
        InsertAfter,
        Insert,
        InsertBeat,
        LineCtlSplitTree,
        LineCtlInsertAfter,
        LineCtlInsert,
        ChannelCtlSplitTree,
        ChannelCtlInsertAfter,
        ChannelCtlInsert,
        GlobalCtlSplitTree,
        GlobalCtlInsertAfter,
        GlobalCtlInsert,
        Remove,
        ChannelCtlRemove,
        LineCtlRemove,
        GlobalCtlRemove,
        RemoveLine,
        RemoveBeat,
        SetTuningMapAndTranspose,
        SwapLines,
        SplitTree,
        MoveLeaf
    }

    private fun <T> track_action(callback: () -> T): Pair<T, Boolean> {
        this._action_count += 1
        val output = try {
            callback()
        } catch (e: Exception) {
            this._action_count -= 1
            throw e
        }

        this._action_count -= 1

        return Pair(output, this._action_count == 0)
    }

    fun apply_tracked_actions(action_list: List<Pair<TrackerToken, List<Int>>>) {
        for ((token, ints) in action_list) {
            this.apply_tracked_token(token, ints)
        }
    }

    fun apply_tracked_token(token: TrackerToken, int_list: List<Int>) {
        when (token) {
            TrackerToken.NewLine -> {
                this.new_line_repeat(int_list[0], int_list[1], int_list[2])
            }
            TrackerToken.InsertAfter -> {
                this.insert_after_repeat(
                    BeatKey(int_list[1], int_list[2], int_list[3]),
                    List(int_list[4]) { i: Int -> int_list[5 + i] },
                    int_list[0]
                )
            }
            TrackerToken.Insert -> {
                this.insert_repeat(
                    BeatKey(int_list[1], int_list[2], int_list[3]),
                    List(int_list[4]) { i: Int -> int_list[5 + i] },
                    int_list[0]
                )
            }
            TrackerToken.LineCtlInsertAfter -> {
                this.controller_line_insert_after(
                    ControlEventType.values()[int_list[0]],
                    BeatKey(int_list[1], int_list[2], int_list[3]),
                    List<Int>(int_list[4]) { i: Int -> int_list[5 + i] }
                )
            }
            TrackerToken.LineCtlInsert -> {
                this.controller_line_insert(
                    ControlEventType.values()[int_list[0]],
                    BeatKey(int_list[1], int_list[2], int_list[3]),
                    List<Int>(int_list[4]) { i: Int -> int_list[5 + i] }
                )
            }
            TrackerToken.ChannelCtlInsertAfter -> {
                this.controller_channel_insert_after(
                    ControlEventType.values()[int_list[0]],
                    int_list[1],
                    int_list[2],
                    List<Int>(int_list[3]) { i: Int -> int_list[4 + i] }
                )
            }
            TrackerToken.ChannelCtlInsert -> {
                this.controller_channel_insert(
                    ControlEventType.values()[int_list[0]],
                    int_list[1],
                    int_list[2],
                    List<Int>(int_list[3]) { i: Int -> int_list[4 + i] }
                )
            }
            TrackerToken.GlobalCtlInsertAfter -> {
                this.controller_global_insert_after(
                    ControlEventType.values()[int_list[0]],
                    int_list[1],
                    List<Int>(int_list[2]) { i: Int -> int_list[3 + i] }
                )
            }
            TrackerToken.GlobalCtlInsert -> {
                this.controller_global_insert_after(
                    ControlEventType.values()[int_list[0]],
                    int_list[1],
                    List<Int>(int_list[2]) { i: Int -> int_list[3 + i] }
                )
            }
            TrackerToken.Remove -> {
                this.remove_repeat(
                    BeatKey(int_list[1], int_list[2], int_list[3]),
                    List<Int>(int_list[4]) { i: Int -> int_list[5 + i] },
                    int_list[0]
                )
            }
            TrackerToken.ChannelCtlRemove -> {
                this.repeat_controller_channel_remove(
                    ControlEventType.values()[int_list[1]],
                    int_list[2],
                    int_list[3],
                    List<Int>(int_list[4]) { i: Int -> int_list[5 + i] },
                    int_list[0]
                )
            }

            TrackerToken.LineCtlRemove -> {
                this.repeat_controller_line_remove(
                    ControlEventType.values()[int_list[1]],
                    BeatKey(
                        int_list[2],
                        int_list[3],
                        int_list[4]
                    ),
                    List<Int>(int_list[5]) { i: Int -> int_list[6 + i] },
                    int_list[0]
                )
            }

            TrackerToken.GlobalCtlRemove -> {
                this.repeat_controller_global_remove(
                    ControlEventType.values()[int_list[1]],
                    int_list[2],
                    List<Int>(int_list[3]) { i: Int -> int_list[4 + i] },
                    int_list[0]
                )
            }

            TrackerToken.RemoveLine -> {
                this.remove_line_repeat(int_list[0], int_list[1], int_list[2])
            }
            TrackerToken.SetTuningMapAndTranspose -> {
                this.set_tuning_map_and_transpose(
                    Array<Pair<Int, Int>>((int_list.size - 2) / 2) { i: Int ->
                        Pair(int_list[2 + (i * 2)], int_list[3 + (i * 2)])
                    },
                    Pair(int_list[0], int_list[1])
                )
            }
            TrackerToken.SwapLines -> {
                this.swap_lines(int_list[0], int_list[1], int_list[2], int_list[3])
            }

            TrackerToken.SplitTree -> {
                this.split_tree(
                    BeatKey(int_list[0], int_list[1], int_list[2]),
                    int_list.subList(5, int_list.size),
                    int_list[3],
                    int_list[4] == 1
                )
            }

            TrackerToken.LineCtlSplitTree -> {
                this.controller_line_split_tree(
                    ControlEventType.values()[int_list[0]],
                    BeatKey(int_list[1], int_list[2], int_list[3]),
                    int_list.subList(6, int_list.size),
                    int_list[4],
                    int_list[5] == 1
                )
            }
            TrackerToken.ChannelCtlSplitTree -> {
                this.controller_channel_split_tree(
                    ControlEventType.values()[int_list[0]],
                    int_list[1],
                    int_list[2],
                    int_list.subList(5, int_list.size),
                    int_list[3],
                    int_list[4] == 1
                )
            }
            TrackerToken.GlobalCtlSplitTree -> {
                this.controller_global_split_tree(
                    ControlEventType.values()[int_list[0]],
                    int_list[1],
                    int_list.subList(4, int_list.size),
                    int_list[2],
                    int_list[3] == 1
                )
            }

            TrackerToken.InsertBeat -> {
                this.insert_beats(int_list[0], int_list[1])
            }
            TrackerToken.RemoveBeat -> {
                this.remove_beat(int_list[0], int_list[1])
            }

            TrackerToken.MoveLeaf -> {
                this.move_leaf(
                    BeatKey(
                        int_list[0],
                        int_list[1],
                        int_list[2]
                    ),
                    List<Int>(int_list[3]) { i: Int -> int_list[8 + i] },
                    BeatKey(
                        int_list[4],
                        int_list[5],
                        int_list[6]
                    ),
                    List<Int>(int_list[7]) { i: Int -> int_list[8 + int_list[3] + i] },
                )
            }
        }
    }

    override fun new_line_repeat(channel: Int, line_offset: Int, count: Int) {
        val (_, track) = this.track_action {
            super.new_line_repeat(channel, line_offset, count)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.NewLine,
                    listOf(channel, line_offset, count)
                )
            )
        }
    }

    override fun insert_after_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        val (_, track) = this.track_action {
            super.insert_after_repeat(beat_key, position, repeat)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.InsertAfter,
                    listOf(repeat, beat_key.channel, beat_key.line_offset, beat_key.beat, position.size) + position
                )
            )
        }
    }

    override fun insert_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        val (_, track) = this.track_action {
            super.insert_repeat(beat_key, position, repeat)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.Insert,
                    listOf(repeat, beat_key.channel, beat_key.line_offset, beat_key.beat, position.size) + position
                )
            )
        }

    }

    override fun controller_global_insert_after(type: ControlEventType, beat: Int, position: List<Int>) {
        val (_, track) = this.track_action {
            super.controller_global_insert_after(type, beat, position)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.GlobalCtlInsertAfter,
                    listOf(type.i, beat, position.size) + position
                )
            )
        }
    }

    override fun controller_channel_insert_after(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        val (_, track) = this.track_action {
            super.controller_channel_insert_after(type, channel, beat, position)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.ChannelCtlInsertAfter,
                    listOf(type.i, channel, beat, position.size) + position
                )
            )
        }
    }

    override fun controller_line_insert_after(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        val (_, track) = this.track_action {
            super.controller_line_insert_after(type, beat_key, position)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.LineCtlInsertAfter,
                    listOf(type.i, beat_key.channel, beat_key.line_offset, beat_key.beat, position.size) + position
                )
            )
        }
    }

    override fun remove_repeat(beat_key: BeatKey, position: List<Int>, count: Int) {
        val (_, track) = this.track_action {
            super.remove_repeat(beat_key, position, count)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.Remove,
                    listOf(count, beat_key.channel, beat_key.line_offset, beat_key.beat, position.size) + position
                )
            )
        }
    }

    override fun repeat_controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, repeat: Int) {
        val (_, track) = this.track_action {
            super.repeat_controller_channel_remove(type, channel, beat, position, repeat)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.ChannelCtlRemove,
                    listOf(repeat, type.i, channel, beat, position.size) + position
                )
            )
        }
    }

    override fun repeat_controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>, count: Int) {
        val (_, track) = this.track_action {
            super.repeat_controller_line_remove(type, beat_key, position, count)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.LineCtlRemove,
                    listOf(count, type.i, beat_key.channel, beat_key.line_offset, beat_key.beat, position.size) + position
                )
            )
        }
    }

    override fun repeat_controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>, count: Int) {
        val (_, track) = this.track_action {
            super.repeat_controller_global_remove(type, beat, position, count)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.GlobalCtlRemove,
                    listOf(count, type.i, beat, position.size) + position
                )
            )
        }
    }

    override fun remove_line_repeat(channel: Int, line_offset: Int, count: Int) {
        val (_, track) = this.track_action {
            super.remove_line_repeat(channel, line_offset, count)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.RemoveLine,
                    listOf(channel, line_offset, count)
                )
            )
        }
    }

    override fun set_tuning_map_and_transpose(tuning_map: Array<Pair<Int, Int>>, transpose: Pair<Int, Int>) {
        val (_, track) = this.track_action {
            super.set_tuning_map_and_transpose(tuning_map, transpose)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.SetTuningMapAndTranspose,
                    List(2 + (tuning_map.size * 2)) { i: Int ->
                        if (i == 0) {
                            transpose.first
                        } else if (i == 1) {
                            transpose.second
                        } else if (i % 2 == 0) {
                            tuning_map[(i - 2) / 2].first
                        } else {
                            tuning_map[(i - 2) / 2].second
                        }
                    }
                )
            )
        }
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        val (_, track) = this.track_action {
            super.swap_lines(channel_a, line_a, channel_b, line_b)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.SwapLines,
                    listOf(channel_a, line_a, channel_b, line_b)
                )
            )
        }
    }

    override fun controller_line_insert(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        val (_, track) = this.track_action {
            super.controller_line_insert(type, beat_key, position)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.LineCtlInsert,
                    listOf(type.i, beat_key.channel, beat_key.line_offset, beat_key.beat, position.size) + position
                )
            )
        }
    }

    override fun controller_channel_insert(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        val (_, track) = this.track_action {
            super.controller_channel_insert(type, channel, beat, position)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.ChannelCtlInsert,
                    listOf(type.i, channel, beat, position.size) + position
                )
            )
        }
    }

    override fun controller_global_insert(type: ControlEventType, beat: Int, position: List<Int>) {
        val (_, track) = this.track_action {
            super.controller_global_insert(type, beat, position)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.GlobalCtlInsert,
                    listOf(type.i, beat, position.size) + position
                )
            )
        }
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        val (_, track) = this.track_action {
            super.split_tree(beat_key, position, splits, move_event_to_end)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.SplitTree,
                    listOf(
                        beat_key.channel,
                        beat_key.line_offset,
                        beat_key.beat,
                        splits,
                        if (move_event_to_end) 1 else 0
                    ) + position
                )
            )
        }
    }

    override fun controller_channel_split_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        val (_, track) = this.track_action {
            super.controller_channel_split_tree(type, channel, beat, position, splits, move_event_to_end)
        }

        if (track) {
            if (track) {
                this.tracker_queue.add(
                    Pair(
                        TrackerToken.ChannelCtlSplitTree,
                        listOf(
                            type.i,
                            channel,
                            beat,
                            splits,
                            if (move_event_to_end) 1 else 0
                        ) + position
                    )
                )
            }
        }
    }

    override fun controller_global_split_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        val (_, track) = this.track_action {
            super.controller_global_split_tree(type, beat, position, splits, move_event_to_end)
        }
        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.GlobalCtlSplitTree,
                    listOf(
                        type.i,
                        beat,
                        splits,
                        if (move_event_to_end) 1 else 0
                    ) + position
                )
            )
        }
    }

    override fun controller_line_split_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        val (_, track) = this.track_action {
            super.controller_line_split_tree(type, beat_key, position, splits, move_event_to_end)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.LineCtlSplitTree,
                    listOf(
                        beat_key.channel,
                        beat_key.line_offset,
                        beat_key.beat,
                        splits,
                        if (move_event_to_end) 1 else 0
                    ) + position
                )
            )
        }
    }

    override fun insert_beats(beat_index: Int, count: Int) {
        val (_, track) = this.track_action {
            super.insert_beats(beat_index, count)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.InsertBeat,
                    listOf(beat_index, count)
                )
            )
        }
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        val (_, track) = this.track_action {
            super.remove_beat(beat_index, count)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.RemoveBeat,
                    listOf(beat_index, count)
                )
            )
        }
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        val (_, track) = this.track_action {
            super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        }

        if (track) {
            this.tracker_queue.add(
                Pair(
                    TrackerToken.MoveLeaf,
                    listOf(
                        beatkey_from.line_offset,
                        beatkey_from.beat,
                        position_from.size,
                        beatkey_to.channel,
                        beatkey_to.line_offset,
                        beatkey_to.beat,
                        position_to.size,
                    ) + position_from + position_to
                )
            )
        }
    }

    override fun controller_channel_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_channel_move_leaf(type, channel_from, beat_from, position_from, channel_to, beat_to, position_to)
    }

    override fun controller_channel_to_line_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        super.controller_channel_to_line_move_leaf(type, channel_from, beat_from, position_from, beat_key_to, position_to)
    }

    override fun controller_global_to_line_move_leaf(type: ControlEventType, beat: Int, position: List<Int>, target_key: BeatKey, target_position: List<Int>) {
        super.controller_global_to_line_move_leaf(type, beat, position, target_key, target_position)
    }

    override fun controller_global_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        super.controller_global_move_leaf(type, beat_from, position_from, beat_to, position_to)
    }

    override fun controller_line_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        super.controller_line_move_leaf(type, beatkey_from, position_from, beat_key_to, position_to)
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        super.move_beat_range(beat_key, first_corner, second_corner)
    }

    override fun controller_channel_to_global_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_channel_to_global_move_leaf(type, channel_from, beat_from, position_from, target_beat, target_position)
    }

    override fun controller_global_to_channel_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_global_to_channel_move_leaf(type, beat_from, position_from, channel_to, beat_to, position_to)
    }

    override fun controller_line_to_global_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_line_to_global_move_leaf(type, beatkey_from, position_from, target_beat, target_position)
    }

    override fun unset_line(channel: Int, line_offset: Int) {
        super.unset_line(channel, line_offset)
    }

    override fun controller_line_unset_line(type: ControlEventType, channel: Int, line_offset: Int) {
        super.controller_line_unset_line(type, channel, line_offset)
    }

    override fun controller_channel_unset_line(type: ControlEventType, channel: Int) {
        super.controller_channel_unset_line(type, channel)
    }

    override fun controller_global_unset_line(type: ControlEventType) {
        super.controller_global_unset_line(type)
    }

    override fun unset_range(first_corner: BeatKey, second_corner: BeatKey) {
        super.unset_range(first_corner, second_corner)
    }

    override fun controller_channel_unset_range(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_unset_range(type, channel, first_beat, second_beat)
    }

    override fun controller_global_unset_range(type: ControlEventType, first_beat: Int, second_beat: Int) {
        super.controller_global_unset_range(type, first_beat, second_beat)
    }

    override fun controller_line_unset_range(type: ControlEventType, first_corner: BeatKey, second_corner: BeatKey) {
        super.controller_line_unset_range(type, first_corner, second_corner)
    }

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        super.percussion_set_event(beat_key, position)
    }

    override fun unset_beat(beat: Int) {
        super.unset_beat(beat)
    }

    override fun controller_channel_unset(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_unset(type, channel, beat, position)
    }

    override fun controller_global_unset(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_unset(type, beat, position)
    }

    override fun controller_line_unset(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_unset(type, beat_key, position)
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
    }

    override fun clear() {
        super.clear()
    }

    override fun load(bytes: ByteArray, new_path: String?) {
        super.load(bytes, new_path)
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        super.new_channel(channel, lines, uuid)
    }

    override fun set_project_name(new_name: String?) {
        super.set_project_name(new_name)
    }

    override fun set_transpose(new_transpose: Pair<Int, Int>) {
        super.set_transpose(new_transpose)
    }

    override fun channel_set_instrument(channel: Int, instrument: Pair<Int, Int>) {
        super.channel_set_instrument(channel, instrument)
    }

    override fun percussion_set_instrument(line_offset: Int, instrument: Int) {
        super.percussion_set_instrument(line_offset, instrument)
    }

    override fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        super.overwrite_beat_range(beat_key, first_corner, second_corner)
    }

    override fun controller_line_to_channel_overwrite_line(type: ControlEventType, target_channel: Int, original_key: BeatKey, repeat: Int?) {
        super.controller_line_to_channel_overwrite_line(type, target_channel, original_key, repeat)
    }

    override fun controller_global_to_channel_overwrite_line(type: ControlEventType, target_channel: Int, beat: Int, repeat: Int?) {
        super.controller_global_to_channel_overwrite_line(type, target_channel, beat, repeat)
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        super.set_duration(beat_key, position, duration)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
    }

    override fun controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove(type, beat_key, position)
    }

    override fun controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove(type, channel, beat, position)
    }

    override fun controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_remove(type, beat, position)
    }

    override fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int?) {
        super.overwrite_line(channel, line_offset, beat_key, repeat)
    }

    override fun controller_line_overwrite_line(type: ControlEventType, channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int?) {
        super.controller_line_overwrite_line(type, channel, line_offset, beat_key, repeat)
    }

    override fun controller_channel_overwrite_line(type: ControlEventType, target_channel: Int, original_channel: Int, original_beat: Int, repeat: Int?) {
        super.controller_channel_overwrite_line(type, target_channel, original_channel, original_beat, repeat)
    }

    override fun controller_global_overwrite_line(type: ControlEventType, beat: Int, repeat: Int?) {
        super.controller_global_overwrite_line(type, beat, repeat)
    }

    override fun controller_channel_to_global_overwrite_line(type: ControlEventType, channel: Int, beat: Int, repeat: Int?) {
        super.controller_channel_to_global_overwrite_line(type, channel, beat, repeat)
    }

    override fun controller_line_to_global_overwrite_line(type: ControlEventType, beat_key: BeatKey, repeat: Int?) {
        super.controller_line_to_global_overwrite_line(type, beat_key, repeat)
    }

    override fun controller_global_to_line_overwrite_line(type: ControlEventType, from_beat: Int, target_channel: Int, target_line_offset: Int, repeat: Int?) {
        super.controller_global_to_line_overwrite_line(type, from_beat, target_channel, target_line_offset, repeat)
    }

    override fun controller_channel_to_line_overwrite_line(type: ControlEventType, target_channel: Int, target_line_offset: Int, original_channel: Int, original_beat: Int, repeat: Int?) {
        super.controller_channel_to_line_overwrite_line(type, target_channel, target_line_offset, original_channel, original_beat, repeat)
    }

    override fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        super.overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key, repeat)
    }

    override fun controller_global_overwrite_range_horizontally(type: ControlEventType, first_beat: Int, second_beat: Int, repeat: Int?) {
        super.controller_global_overwrite_range_horizontally(type, first_beat, second_beat, repeat)
    }

    override fun controller_global_to_line_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, target_line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        super.controller_global_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, first_beat, second_beat, repeat)
    }

    override fun controller_line_to_channel_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        super.controller_line_to_channel_overwrite_range_horizontally(type, channel, first_key, second_key, repeat)
    }

    override fun controller_global_to_channel_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        super.controller_global_to_channel_overwrite_range_horizontally(type, channel, first_beat, second_beat, repeat)
    }

    override fun controller_line_overwrite_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        super.controller_line_overwrite_range_horizontally(type, channel, line_offset, first_key, second_key, repeat)
    }

    override fun controller_line_to_global_overwrite_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        super.controller_line_to_global_overwrite_range_horizontally(type, channel, line_offset, first_beat, second_beat, repeat)
    }

    override fun controller_channel_to_global_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        super.controller_channel_to_global_overwrite_range_horizontally(type, channel, first_beat, second_beat, repeat)
    }

    override fun controller_channel_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, from_channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        super.controller_channel_overwrite_range_horizontally(type, target_channel, from_channel, first_beat, second_beat, repeat)
    }

    override fun controller_channel_to_line_overwrite_range_horizontally(
        type: ControlEventType,
        target_channel: Int,
        target_line_offset: Int,
        from_channel: Int,
        first_beat: Int,
        second_beat: Int,
        repeat: Int?
    ) {
        super.controller_channel_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, from_channel, first_beat, second_beat, repeat)
    }

    override fun merge_leafs(beat_key_from: BeatKey, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        super.merge_leafs(beat_key_from, position_from, beat_key_to, position_to)
    }

    override fun set_beat_count(new_count: Int) {
        super.set_beat_count(new_count)
    }

    override fun save(path: String?) {
        super.save(path)
    }

    override fun to_json(): JSONHashMap {
        return super.to_json()
    }

    override fun remove_global_controller(type: ControlEventType) {
        super.remove_global_controller(type)
    }

    override fun remove_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.remove_line_controller(type, channel_index, line_offset)
    }

    override fun remove_channel_controller(type: ControlEventType, channel_index: Int) {
        super.remove_channel_controller(type, channel_index)
    }

    override fun cursor_clear() {
        super.cursor_clear()
    }

    override fun cursor_select_channel(channel: Int) {
        super.cursor_select_channel(channel)
    }

    override fun cursor_select_line(channel: Int, line_offset: Int) {
        super.cursor_select_line(channel, line_offset)
    }

    override fun cursor_select_line_ctl_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        super.cursor_select_line_ctl_line(ctl_type, channel, line_offset)
    }

    override fun cursor_select_channel_ctl_line(ctl_type: ControlEventType, channel: Int) {
        super.cursor_select_channel_ctl_line(ctl_type, channel)
    }

    override fun cursor_select_global_ctl_line(ctl_type: ControlEventType) {
        super.cursor_select_global_ctl_line(ctl_type)
    }

    override fun cursor_select_column(beat: Int) {
        super.cursor_select_column(beat)
    }

    override fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        super.cursor_select(beat_key, position)
    }

    override fun cursor_select_ctl_at_line(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.cursor_select_ctl_at_line(ctl_type, beat_key, position)
    }

    override fun cursor_select_ctl_at_channel(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.cursor_select_ctl_at_channel(ctl_type, channel, beat, position)
    }

    override fun cursor_select_ctl_at_global(ctl_type: ControlEventType, beat: Int, position: List<Int>) {
        super.cursor_select_ctl_at_global(ctl_type, beat, position)
    }

    override fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        super.cursor_select_range(beat_key_a, beat_key_b)
    }

    override fun cursor_select_global_ctl_range(type: ControlEventType, first: Int, second: Int) {
        super.cursor_select_global_ctl_range(type, first, second)
    }

    override fun cursor_select_channel_ctl_range(type: ControlEventType, channel: Int, first: Int, second: Int) {
        super.cursor_select_channel_ctl_range(type, channel, first, second)
    }

    override fun cursor_select_line_ctl_range(type: ControlEventType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        super.cursor_select_line_ctl_range(type, beat_key_a, beat_key_b)
    }

    override fun new_channel_controller(type: ControlEventType, channel_index: Int) {
        super.new_channel_controller(type, channel_index)
    }

    override fun new_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.new_line_controller(type, channel_index, line_offset)
    }

    override fun new_global_controller(type: ControlEventType) {
        super.new_global_controller(type)
    }

    override fun set_channel_visibility(channel_index: Int, visibility: Boolean) {
        super.set_channel_visibility(channel_index, visibility)
    }

    override fun convert_events_in_line_to_absolute(channel: Int, line_offset: Int) {
        super.convert_events_in_line_to_absolute(channel, line_offset)
    }

    override fun convert_events_in_tree_to_absolute(beat_key: BeatKey, position: List<Int>) {
        super.convert_events_in_tree_to_absolute(beat_key, position)
    }

    override fun convert_events_in_beat_to_absolute(beat: Int) {
        super.convert_events_in_beat_to_absolute(beat)
    }

    override fun convert_events_in_line_to_relative(channel: Int, line_offset: Int) {
        super.convert_events_in_line_to_relative(channel, line_offset)
    }

    override fun convert_events_in_tree_to_relative(beat_key: BeatKey, position: List<Int>) {
        super.convert_events_in_tree_to_relative(beat_key, position)
    }

    override fun convert_events_in_beat_to_relative(beat: Int) {
        super.convert_events_in_beat_to_relative(beat)
    }

    override fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        super.convert_event_to_absolute(beat_key, position)
    }

    override fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        super.convert_event_to_relative(beat_key, position)
    }

    override fun toggle_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.toggle_line_controller_visibility(type, channel_index, line_offset)
    }

    override fun toggle_global_control_visibility(type: ControlEventType) {
        super.toggle_global_control_visibility(type)
    }

    override fun toggle_channel_controller_visibility(type: ControlEventType, channel_index: Int) {
        super.toggle_channel_controller_visibility(type, channel_index)
    }

    override fun apply_undo(repeat: Int) {
        super.apply_undo(repeat)
    }

}