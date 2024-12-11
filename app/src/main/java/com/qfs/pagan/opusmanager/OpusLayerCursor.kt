package com.qfs.pagan.opusmanager
import com.qfs.apres.Midi
import com.qfs.json.JSONHashMap
import com.qfs.pagan.structure.OpusTree
import java.lang.Integer.max
import java.lang.Integer.min

open class OpusLayerCursor: OpusLayerBase() {
    class InvalidCursorState: Exception()
    var cursor = OpusManagerCursor()

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    // BASE FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    /* ------------------- 1st Order Functions ---------------------------------- */
    override fun insert(beat_key: BeatKey, position: List<Int>) {
        super.insert(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    override fun controller_line_insert(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_insert(type, beat_key, position)
        this.cursor_select_ctl_at_line(type, beat_key, position)
    }

    override fun controller_channel_insert(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_insert(type, channel, beat, position)
        this.cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    override fun controller_global_insert(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_insert(type, beat, position)
        this.cursor_select_ctl_at_global(type, beat, position)
    }

    override fun insert_after(beat_key: BeatKey, position: List<Int>) {
        // Cursor handled in insert()
        super.insert_after(beat_key, position)
    }

    override fun controller_channel_insert_after(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        // Cursor handled in controller_channel_insert()
        super.controller_channel_insert_after(type, channel, beat, position)
    }

    override fun controller_global_insert_after(type: ControlEventType, beat: Int, position: List<Int>) {
        // Cursor handled in controller_global_insert()
        super.controller_global_insert_after(type, beat, position)
    }

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        super.percussion_set_event(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    override fun percussion_set_instrument(line_offset: Int, instrument: Int) {
        super.percussion_set_instrument(line_offset, instrument)
        //this.cursor_select_line(this.channels.size, line_offset)
    }

    override fun channel_set_instrument(channel: Int, instrument: Pair<Int, Int>) {
        super.channel_set_instrument(channel, instrument)
        this.cursor_select_channel(channel)
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: OpusTree<out InstrumentEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.cursor_select(beat_key, this.get_first_position(beat_key, position))
    }

    override fun <T : OpusControlEvent> controller_line_replace_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>?, tree: OpusTree<T>) {
        super.controller_line_replace_tree(type, beat_key, position, tree)
        val new_position = this.get_first_position_line_ctl(type, beat_key, position ?: listOf())
        if (this.is_line_ctl_visible(type, beat_key.channel, beat_key.line_offset)) {
            this.cursor_select_ctl_at_line(type, beat_key, new_position)
        }
    }

    override fun <T : OpusControlEvent> controller_channel_replace_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        super.controller_channel_replace_tree(type, channel, beat, position, tree)
        val new_position = this.get_first_position_channel_ctl(type, channel, beat, position ?: listOf())
        this.cursor_select_ctl_at_channel(type, channel, beat, new_position)
    }

    override fun <T : OpusControlEvent> controller_global_replace_tree(type: ControlEventType, beat: Int, position: List<Int>?, tree: OpusTree<T>) {
        super.controller_global_replace_tree(type, beat, position, tree)
        val new_position = this.get_first_position_global_ctl(type, beat, position ?: listOf())
        this.cursor_select_ctl_at_global(type, beat, new_position)
    }

    override fun <T : OpusControlEvent> controller_line_set_event(type: ControlEventType, beat_key: BeatKey, position: List<Int>, event: T) {
        super.controller_line_set_event(type, beat_key, position, event)

        if (this.is_line_ctl_visible(type, beat_key.channel, beat_key.line_offset)) {
            this.cursor_select_ctl_at_line(type, beat_key, position)
        }
    }

    override fun <T : OpusControlEvent> controller_channel_set_event(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, event: T) {
        super.controller_channel_set_event(type, channel, beat, position, event)
        if (this.is_channel_ctl_visible(type, channel)) {
            this.cursor_select_ctl_at_channel(type, channel, beat, position)
        }
    }

    override fun <T : OpusControlEvent> controller_global_set_event(type: ControlEventType, beat: Int, position: List<Int>, event: T) {
        super.controller_global_set_event(type, beat, position, event)
        if (this.is_global_ctl_visible(type)) {
            this.cursor_select_ctl_at_global(type, beat, position)
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    override fun controller_line_unset(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_unset(type, beat_key, position)
        this.cursor_select_ctl_at_line(type, beat_key, position)
    }

    override fun controller_channel_unset(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_unset(type, channel, beat, position)
        this.cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    override fun controller_global_unset(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_unset(type, beat, position)
        this.cursor_select_ctl_at_global(type, beat, position)
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.split_tree(beat_key, position, splits, move_event_to_end)

        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select(beat_key, new_position)
    }

    override fun controller_global_split_tree(type: ControlEventType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.controller_global_split_tree(type, beat, position, splits, move_event_to_end)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_global(type, beat, new_position)
    }

    override fun controller_channel_split_tree(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.controller_channel_split_tree(type, channel, beat, position, splits, move_event_to_end)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_channel(type, channel, beat, new_position)
    }

    override fun controller_line_split_tree(type: ControlEventType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.controller_line_split_tree(type, beat_key, position, splits, move_event_to_end)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_line(type, beat_key, new_position)
    }

    override fun <T : InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        super.set_event(beat_key, position, event)
        this.cursor_select(beat_key, position)
    }

    override fun swap_lines(channel_a: Int, line_a: Int, channel_b: Int, line_b: Int) {
        super.swap_lines(channel_a, line_a, channel_b, line_b)
        this.cursor_select_line(channel_a, line_a)
    }

    override fun remove_line(channel: Int, line_offset: Int): OpusLineAbstract<*> {
        val output = try {
            super.remove_line(channel, line_offset)
        } catch (e: OpusChannelAbstract.LastLineException) {
            throw e
        }

        val channels = this.get_all_channels()
        val next_line = max(0, min(line_offset, channels[channel].size - 1))
        this.cursor_select_line(channel, next_line)

        return output
    }

    override fun set_project_name(new_name: String?) {
        super.set_project_name(new_name)
    }

    override fun set_transpose(new_transpose: Int) {
        super.set_transpose(new_transpose)
    }

    override fun set_tuning_map(new_map: Array<Pair<Int, Int>>, mod_events: Boolean) {
        super.set_tuning_map(new_map, mod_events)
    }

    override fun new_channel_controller(type: ControlEventType, channel_index: Int) {
        super.new_channel_controller(type, channel_index)
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?) {
        super.new_channel(channel, lines, uuid)
        this.cursor_select_channel(channel ?: this.channels.size - 1)
    }

    override fun new_line(channel: Int, line_offset: Int?) {
        super.new_line(channel, line_offset)
        this.cursor_select_line(channel, line_offset ?: (this.get_all_channels()[channel].lines.size - 1))
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        super.insert_line(channel, line_offset, line)
        this.cursor_select_line(channel, line_offset)
    }

    override fun set_channel_visibility(channel_index: Int, visibility: Boolean) {
        super.set_channel_visibility(channel_index, visibility)

        if (visibility) {
            this.cursor_select_channel(channel_index)
        } else {
            var next_channel_index = channel_index
            val channels = this.get_all_channels()
            while (next_channel_index < channels.size && !channels[next_channel_index].visible) {
                next_channel_index += 1
            }

            if (next_channel_index >= channels.size) {
                next_channel_index -= 1
                while ((next_channel_index > 0) && !channels[next_channel_index].visible) {
                    next_channel_index -= 1
                }
            }

            if (channels.indices.contains(next_channel_index)) {
                this.cursor_select_channel(next_channel_index)
            }
        }
    }

    override fun remove_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.remove_line_controller(type, channel_index, line_offset)
        this.cursor_select_line(channel_index, line_offset)
    }

    override fun new_line_controller(type: ControlEventType, channel_index: Int, line_offset: Int) {
        super.new_line_controller(type, channel_index, line_offset)
    }

    override fun remove_channel_controller(type: ControlEventType, channel_index: Int) {
        super.remove_channel_controller(type, channel_index)
        this.cursor_select_channel(channel_index)
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)

        var next_channel_index = channel
        val channels = this.get_all_channels()
        while (next_channel_index < channels.size && !channels[next_channel_index].visible) {
            next_channel_index += 1
        }

        if (next_channel_index >= channels.size) {
            next_channel_index -= 1
            while ((next_channel_index > 0) && !channels[next_channel_index].visible) {
                next_channel_index -= 1
            }
        }

        if (channels.indices.contains(next_channel_index)) {
            this.cursor_select_channel(next_channel_index)
        }
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        super.remove_beat(beat_index, count)
        this.cursor_select_column(max(0, min(this.beat_count - 1, beat_index)))
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        super.remove_standard(beat_key, position)

        val tree = this.get_tree(beat_key, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select(beat_key, new_position)
    }

    override fun controller_line_remove_standard(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove_standard(type, beat_key, position)

        val tree = this.get_line_ctl_tree<OpusControlEvent>(type, beat_key, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select_ctl_at_line(type, beat_key, new_position)
    }

    override fun controller_channel_remove_standard(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove_standard(type, channel, beat, position)

        val tree = this.get_channel_ctl_tree<OpusControlEvent>(type, channel, beat, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select_ctl_at_channel(type, channel, beat, new_position)
    }

    override fun controller_global_remove_standard(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_remove_standard(type, beat, position)

        val tree = this.get_global_ctl_tree<OpusControlEvent>(type, beat, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select_ctl_at_global(type, beat, new_position)
    }

    /* ------------------- 2nd Order Functions ---------------------------------- */
    override fun insert_beats(beat_index: Int, count: Int) {
        super.insert_beats(beat_index, count)
        this.cursor_select_column(beat_index)
    }

    override fun new_line_repeat(channel: Int, line_offset: Int, count: Int) {
        super.new_line_repeat(channel, line_offset, count)
        // Cursor Handled in new_line
    }

    override fun move_leaf(beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        super.move_leaf(beatkey_from, position_from, beatkey_to, position_to)
        // Cursor Handled in replace_tree
    }

    // ----- Cursor handled in lower order functions VVVVVVVVVVVV //
    override fun controller_global_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        super.controller_global_move_leaf(type, beat_from, position_from, beat_to, position_to)
        // Cursor Handled in replace_tree
    }

    override fun controller_channel_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_channel_move_leaf(type, channel_from, beat_from, position_from, channel_to, beat_to, position_to)
        // Cursor Handled in replace_tree
    }

    override fun controller_line_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        super.controller_line_move_leaf(type, beatkey_from, position_from, beatkey_to, position_to)
        // Cursor Handled in replace_tree
    }

    override fun controller_global_to_channel_move_leaf(type: ControlEventType, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_global_to_channel_move_leaf(type, beat_from, position_from, channel_to, beat_to, position_to)
        // Cursor Handled in replace_tree
    }

    override fun controller_global_to_line_move_leaf(type: ControlEventType, beat: Int, position: List<Int>, target_key: BeatKey, target_position: List<Int>) {
        super.controller_global_to_line_move_leaf(type, beat, position, target_key, target_position)
        // Cursor Handled in replace_tree
    }

    override fun controller_channel_to_global_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_channel_to_global_move_leaf(type, channel_from, beat_from, position_from, target_beat, target_position)
        // Cursor handled in replace_tree()
    }

    override fun controller_channel_to_line_move_leaf(type: ControlEventType, channel_from: Int, beat_from: Int, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        super.controller_channel_to_line_move_leaf(type, channel_from, beat_from, position_from, beat_key_to, position_to)
        // Cursor Handled in replace_tree
    }

    override fun controller_line_to_global_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_line_to_global_move_leaf(type, beatkey_from, position_from, target_beat, target_position)
        // Cursor Handled in replace_tree
    }

    override fun controller_line_to_channel_move_leaf(type: ControlEventType, beatkey_from: BeatKey, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_line_to_channel_move_leaf(type, beatkey_from, position_from, channel_to, beat_to, position_to)
        // Cursor Handled in replace_tree
    }

    // ----- Cursor handled in lower order functions ^^^^^^^^^^^^ //

    override fun _controller_global_copy_range(type: ControlEventType, target: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_global_copy_range(type, target, point_a, point_b, unset_original)
        val position = this.get_first_position_global_ctl(type, target)
        this.cursor_select_ctl_at_global(type, target, position)
    }

    override fun _controller_global_to_channel_copy_range(type: ControlEventType, target_channel: Int, target_beat: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_global_to_channel_copy_range(type, target_channel, target_beat, point_a, point_b, unset_original)
        this.cursor_select_ctl_at_channel(type, target_channel, target_beat, this.get_first_position_channel_ctl(type, target_channel, target_beat, listOf()))
    }

    override fun _controller_global_to_line_copy_range(type: ControlEventType, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        super._controller_global_to_line_copy_range(type, beat_a, beat_b, target_key, unset_original)
        this.cursor_select_ctl_at_line(type, target_key, this.get_first_position_line_ctl(type, target_key))
    }

    override fun _controller_channel_to_global_copy_range(type: ControlEventType, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_channel_to_global_copy_range(type, target_beat, original_channel, point_a, point_b, unset_original)
        this.cursor_select_ctl_at_global(type, target_beat, this.get_first_position_global_ctl(type, target_beat, listOf()))
    }

    override fun _controller_channel_copy_range(type: ControlEventType, target_channel: Int, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_channel_copy_range(type, target_channel, target_beat, original_channel, point_a, point_b, unset_original)
        val position = this.get_first_position_channel_ctl(type, target_channel, target_beat)
        this.cursor_select_ctl_at_channel(type, target_channel, target_beat, position)
    }

    override fun _controller_channel_to_line_copy_range(type: ControlEventType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        super._controller_channel_to_line_copy_range(type, channel_from, beat_a, beat_b, target_key, unset_original)
        this.cursor_select_ctl_at_line(type, target_key, this.get_first_position_line_ctl(type, target_key))
    }

    override fun _controller_line_to_global_copy_range(type: ControlEventType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int, unset_original: Boolean) {
        super._controller_line_to_global_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_beat, unset_original)
        this.cursor_select_ctl_at_global(type, target_beat, this.get_first_position_global_ctl(type, target_beat, listOf()))
    }

    override fun _controller_line_to_channel_copy_range(type: ControlEventType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int, unset_original: Boolean) {
        super._controller_line_to_channel_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_channel, target_beat, unset_original)
        this.cursor_select_ctl_at_channel(type, target_channel, target_beat, this.get_first_position_channel_ctl(type, target_channel, target_beat, listOf()))
    }

    override fun _controller_line_copy_range(type: ControlEventType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey, unset_original: Boolean) {
        super._controller_line_copy_range(type, beat_key, first_corner, second_corner, unset_original)
        this.cursor_select_ctl_at_line(type, beat_key, this.get_first_position_line_ctl(type, beat_key))
    }

    override fun controller_line_unset_line(type: ControlEventType, channel: Int, line_offset: Int) {
        super.controller_line_unset_line(type, channel, line_offset)
        this.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    override fun controller_channel_unset_line(type: ControlEventType, channel: Int) {
        super.controller_channel_unset_line(type, channel)
        this.cursor_select_channel_ctl_line(type, channel)
    }

    override fun controller_global_unset_line(type: ControlEventType) {
        super.controller_global_unset_line(type)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_global_unset_range(type: ControlEventType, first_beat: Int, second_beat: Int) {
        super.controller_global_unset_range(type, first_beat, second_beat)
        val new_position = this.get_first_position_global_ctl(type, first_beat)
        this.cursor_select_ctl_at_global(type, first_beat, new_position)
    }

    override fun controller_channel_unset_range(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_unset_range(type, channel, first_beat, second_beat)
        val new_position = this.get_first_position_channel_ctl(type, channel, first_beat)
        this.cursor_select_ctl_at_channel(type, channel, first_beat, new_position)
    }

    override fun controller_line_unset_range(type: ControlEventType, first_corner: BeatKey, second_corner: BeatKey) {
        super.controller_line_unset_range(type, first_corner, second_corner)
        val new_position = this.get_first_position_line_ctl(type, first_corner)
        this.cursor_select_ctl_at_line(type, first_corner, new_position)
    }

    override fun controller_line_remove_one_of_two(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove_one_of_two(type, beat_key, position)
        // Cursor Handled in replace_tree()
    }

    override fun controller_channel_remove_one_of_two(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove_one_of_two(type, channel, beat, position)
        // Cursor Handled in replace_tree()
    }

    override fun controller_global_remove_one_of_two(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_remove_one_of_two(type, beat, position)
        // Cursor Handled in replace_tree()
    }

    override fun controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove(type, beat_key, position)
        // Cursor handled in remove_one_of_two or remove_standard
    }

    override fun controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove(type, channel, beat, position)
        // Cursor handled in remove_one_of_two or remove_standard
    }

    override fun controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>) {
        super.controller_global_remove(type, beat, position)
        // Cursor handled in remove_one_of_two or remove_standard
    }

    override fun controller_global_overwrite_range_horizontally(type: ControlEventType, first_beat: Int, second_beat: Int) {
        super.controller_global_overwrite_range_horizontally(type, first_beat, second_beat)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_global_to_line_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, target_line_offset: Int, first_beat: Int, second_beat: Int) {
        super.controller_global_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, first_beat, second_beat)
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_line_to_channel_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_key: BeatKey, second_key: BeatKey) {
        super.controller_line_to_channel_overwrite_range_horizontally(type, channel, first_key, second_key)
        this.cursor_select_channel_ctl_line(type, channel)
    }

    override fun controller_global_to_channel_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_global_to_channel_overwrite_range_horizontally(type, channel, first_beat, second_beat)
        this.cursor_select_channel_ctl_line(type, channel)
    }

    override fun controller_line_overwrite_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        super.controller_line_overwrite_range_horizontally(type, channel, line_offset, first_key, second_key)
        this.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    override fun controller_line_to_global_overwrite_range_horizontally(type: ControlEventType, channel: Int, line_offset: Int, first_beat: Int, second_beat: Int) {
        super.controller_line_to_global_overwrite_range_horizontally(type, channel, line_offset, first_beat, second_beat)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_channel_to_global_overwrite_range_horizontally(type: ControlEventType, channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_to_global_overwrite_range_horizontally(type, channel, first_beat, second_beat)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_channel_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, from_channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_overwrite_range_horizontally(type, target_channel, from_channel, first_beat, second_beat)
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_channel_to_line_overwrite_range_horizontally(type: ControlEventType, target_channel: Int, target_line_offset: Int, from_channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, from_channel, first_beat, second_beat)
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_global_overwrite_line(type: ControlEventType, beat: Int) {
        super.controller_global_overwrite_line(type, beat)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_channel_to_global_overwrite_line(type: ControlEventType, channel: Int, beat: Int) {
        super.controller_channel_to_global_overwrite_line(type, channel, beat)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_line_to_global_overwrite_line(type: ControlEventType, beat_key: BeatKey) {
        super.controller_line_to_global_overwrite_line(type, beat_key)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_global_to_line_overwrite_line(type: ControlEventType, from_beat: Int, target_channel: Int, target_line_offset: Int) {
        super.controller_global_to_line_overwrite_line(type, from_beat, target_channel, target_line_offset)
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_channel_to_line_overwrite_line(type: ControlEventType, target_channel: Int, target_line_offset: Int, original_channel: Int, original_beat: Int) {
        super.controller_channel_to_line_overwrite_line(type, target_channel, target_line_offset, original_channel, original_beat)
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_channel_overwrite_line(type: ControlEventType, target_channel: Int, original_channel: Int, original_beat: Int) {
        super.controller_channel_overwrite_line(type, target_channel, original_channel, original_beat)
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_line_to_channel_overwrite_line(type: ControlEventType, target_channel: Int, original_key: BeatKey) {
        super.controller_line_to_channel_overwrite_line(type, target_channel, original_key)
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_global_to_channel_overwrite_line(type: ControlEventType, target_channel: Int, beat: Int) {
        super.controller_global_to_channel_overwrite_line(type, target_channel, beat)
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_line_overwrite_line(type: ControlEventType, channel: Int, line_offset: Int, beat_key: BeatKey) {
        super.controller_line_overwrite_line(type, channel, line_offset, beat_key)
        this.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    override fun <T : OpusControlEvent> controller_global_set_initial_event(type: ControlEventType, event: T) {
        super.controller_global_set_initial_event(type, event)
        if (this.is_global_ctl_visible(type)) {
            this.cursor_select_global_ctl_line(type)
        } else {
            this.cursor_clear()
        }
    }

    override fun <T : OpusControlEvent> controller_channel_set_initial_event(type: ControlEventType, channel: Int, event: T) {
        super.controller_channel_set_initial_event(type, channel, event)
        if (this.is_channel_ctl_visible(type, channel)) {
            this.cursor_select_channel_ctl_line(type, channel)
        } else {
            this.cursor_select_channel(channel)
        }
    }

    override fun <T : OpusControlEvent> controller_line_set_initial_event(type: ControlEventType, channel: Int, line_offset: Int, event: T) {
        super.controller_line_set_initial_event(type, channel, line_offset, event)
        if (this.is_line_ctl_visible(type, channel, line_offset)) {
            this.cursor_select_line_ctl_line(type, channel, line_offset)
        } else {
            this.cursor_select_line(channel, line_offset)
        }
    }

    override fun overwrite_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        super.overwrite_beat_range(beat_key, first_corner, second_corner)
        this.cursor_select(beat_key, this.get_first_position(beat_key))
    }

    override fun move_beat_range(beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey) {
        super.move_beat_range(beat_key, first_corner, second_corner)
        this.cursor_select(beat_key, this.get_first_position(beat_key))
    }

    override fun unset_line(channel: Int, line_offset: Int) {
        super.unset_line(channel, line_offset)
        this.cursor_select_line(channel, line_offset)
    }

    override fun unset_range(first_corner: BeatKey, second_corner: BeatKey) {
        super.unset_range(first_corner, second_corner)
        val new_position = this.get_first_position(first_corner)
        this.cursor_select(first_corner, new_position)
    }

    override fun unset_beat(beat: Int) {
        super.unset_beat(beat)
        this.cursor_select_column(beat)
    }

    override fun insert_after_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        super.insert_after_repeat(beat_key, position, repeat)
        // Cursor handled in insert_after()
    }

    override fun remove_one_of_two(beat_key: BeatKey, position: List<Int>) {
        super.remove_one_of_two(beat_key, position)
        // Cursor Handled in replace_tree()
    }

    override fun remove_line_repeat(channel: Int, line_offset: Int, count: Int) {
        super.remove_line_repeat(channel, line_offset, count)
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        super.set_duration(beat_key, position, duration)
    }

    override fun remove(beat_key: BeatKey, position: List<Int>) {
        super.remove(beat_key, position)
        // Cursor handled in remove_one_of_two or remove_standard
    }

    override fun remove_repeat(beat_key: BeatKey, position: List<Int>, count: Int) {
        super.remove_repeat(beat_key, position, count)
        // Cursor handled in remove()
    }

    override fun repeat_controller_line_remove(type: ControlEventType, beat_key: BeatKey, position: List<Int>, count: Int) {
        super.repeat_controller_line_remove(type, beat_key, position, count)
        // Cursor handled in remove()
    }

    override fun repeat_controller_channel_remove(type: ControlEventType, channel: Int, beat: Int, position: List<Int>, repeat: Int) {
        super.repeat_controller_channel_remove(type, channel, beat, position, repeat)
        // Cursor handled in remove()
    }

    override fun repeat_controller_global_remove(type: ControlEventType, beat: Int, position: List<Int>, count: Int) {
        super.repeat_controller_global_remove(type, beat, position, count)
        // Cursor handled in remove()
    }

    override fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey) {
        super.overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key)
        this.cursor_select_line(channel, line_offset)
    }

    override fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey) {
        super.overwrite_line(channel, line_offset, beat_key)
        this.cursor_select_line(channel, line_offset)
    }

    override fun insert_beat(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>?) {
        super.insert_beat(beat_index, beats_in_column)
        this.cursor_select_column(beat_index)
    }

    override fun clear() {
        this.cursor_clear()
        super.clear()
    }

    override fun on_project_changed() {
        super.on_project_changed()
        this.cursor_clear()
    }

    override fun load(bytes: ByteArray, new_path: String?) {
        super.load(bytes, new_path)
    }

    override fun project_change_wrapper(callback: () -> Unit) {
        super.project_change_wrapper(callback)
    }

    override fun set_global_controller_visibility(type: ControlEventType, visibility: Boolean) {
        super.set_global_controller_visibility(type, visibility)
        if (visibility) {
            this.cursor_select_global_ctl_line(type)
        } else {
            this.cursor_clear()
        }
    }

    override fun set_channel_controller_visibility(type: ControlEventType, channel_index: Int, visibility: Boolean) {
        super.set_channel_controller_visibility(type, channel_index, visibility)
        if (visibility) {
            this.cursor_select_channel_ctl_line(type, channel_index)
        } else {
            this.cursor_select_channel(channel_index)
        }
    }

    override fun remove_global_controller(type: ControlEventType) {
        super.remove_global_controller(type)
        this.cursor_clear()
    }

    override fun new_global_controller(type: ControlEventType) {
        super.new_global_controller(type)
    }

    override fun set_line_controller_visibility(type: ControlEventType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        super.set_line_controller_visibility(type, channel_index, line_offset, visibility)
        if (visibility) {
            this.cursor_select_line_ctl_line(type, channel_index, line_offset)
        } else {
            this.cursor_select_line(channel_index, line_offset)
        }
    }

    override fun _apply_column_trees(beat_index: Int, beats_in_column: List<OpusTree<OpusEvent>>) {
        super._apply_column_trees(beat_index, beats_in_column)
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

    override fun _project_change_json(json_data: JSONHashMap) {
        super._project_change_json(json_data)
    }

    override fun _project_change_midi(midi: Midi) {
        super._project_change_midi(midi)
    }

    override fun recache_line_maps() {
        super.recache_line_maps()
    }

    override fun <T : OpusLayerBase> import_from_other(other: T) {
        super.import_from_other(other)
    }

    override fun on_action_blocked(blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked(blocker_key, blocker_position)

        this.cursor_select(blocker_key, blocker_position)
    }

    override fun on_action_blocked_global_ctl(type: ControlEventType, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_global_ctl(type, blocker_beat, blocker_position)
        this.cursor_select_ctl_at_global(type, blocker_beat, blocker_position)
    }

    override fun on_action_blocked_channel_ctl(type: ControlEventType, blocker_channel: Int, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
        this.cursor_select_ctl_at_channel(type, blocker_channel, blocker_beat, blocker_position)
    }

    override fun on_action_blocked_line_ctl(type: ControlEventType, blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked_line_ctl(type, blocker_key, blocker_position)
        this.cursor_select_ctl_at_line(type, blocker_key, blocker_position)
    }

    override fun _project_change_new() {
        super._project_change_new()
    }

    override fun convert_event_to_absolute(beat_key: BeatKey, position: List<Int>) {
        // Cursor handled in replace_tree()
        super.convert_event_to_absolute(beat_key, position)
    }

    override fun convert_event_to_relative(beat_key: BeatKey, position: List<Int>) {
        // Cursor handled in replace_tree()
        super.convert_event_to_relative(beat_key, position)
    }

    override fun convert_events_in_beat_to_absolute(beat: Int) {
        // Cursor handled in replace_tree()
        super.convert_events_in_beat_to_absolute(beat)
    }

    override fun convert_events_in_beat_to_relative(beat: Int) {
        // Cursor handled in replace_tree()
        super.convert_events_in_beat_to_relative(beat)
    }

    override fun convert_events_in_line_to_absolute(channel: Int, line_offset: Int) {
        // Cursor handled in replace_tree()
        super.convert_events_in_line_to_absolute(channel, line_offset)
    }

    override fun convert_events_in_line_to_relative(channel: Int, line_offset: Int) {
        // Cursor handled in replace_tree()
        super.convert_events_in_line_to_relative(channel, line_offset)
    }

    override fun convert_events_in_tree_to_absolute(beat_key: BeatKey, position: List<Int>) {
        // Cursor handled in replace_tree()
        super.convert_events_in_tree_to_absolute(beat_key, position)
    }

    override fun convert_events_in_tree_to_relative(beat_key: BeatKey, position: List<Int>) {
        // Cursor handled in replace_tree()
        super.convert_events_in_tree_to_relative(beat_key, position)
    }

    override fun insert_repeat(beat_key: BeatKey, position: List<Int>, repeat: Int) {
        // Cursor handled in insert()
        super.insert_repeat(beat_key, position, repeat)
    }

    override fun controller_line_insert_after(type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        // Cursor handled in controller_line_insert()
        super.controller_line_insert_after(type, beat_key, position)
    }
    // BASE FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    open fun cursor_apply(cursor: OpusManagerCursor) {
        if (this._block_cursor_selection()) {
            return
        }
        this.cursor.clear()
        this.cursor = cursor
    }
    open fun cursor_clear() {
        if (this._block_cursor_selection()) {
            return
        }

        this.cursor.clear()
    }
    open fun cursor_select_channel(channel: Int) {
        this.cursor.select_channel(channel)
    }
    open fun cursor_select_line(channel: Int, line_offset: Int) {
        this.cursor.select_line(channel, line_offset)
    }
    open fun cursor_select_line_ctl_line(ctl_type: ControlEventType, channel: Int, line_offset: Int) {
        this.cursor.select_line_ctl_line(channel, line_offset, ctl_type)
    }
    open fun cursor_select_channel_ctl_line(ctl_type: ControlEventType, channel: Int) {
        this.cursor.select_channel_ctl_line(channel, ctl_type)
    }
    open fun cursor_select_global_ctl_line(ctl_type: ControlEventType) {
        this.cursor.select_global_ctl_line(ctl_type)
    }
    open fun cursor_select_column(beat: Int) {
        if (beat >= this.beat_count) {
            return
        }

        this.cursor.select_column(beat)
    }
    open fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        this.cursor.select(beat_key, position)
    }
    open fun cursor_select_ctl_at_line(ctl_type: ControlEventType, beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        if (!this.is_line_ctl_visible(ctl_type, beat_key.channel, beat_key.line_offset)) {
            return
        }
        this.cursor.select_ctl_at_line(beat_key, position, ctl_type)
    }
    open fun cursor_select_ctl_at_channel(ctl_type: ControlEventType, channel: Int, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        if (!this.is_channel_ctl_visible(ctl_type, channel)) {
            return
        }
        this.cursor.select_ctl_at_channel(channel, beat, position, ctl_type)
    }
    open fun cursor_select_ctl_at_global(ctl_type: ControlEventType, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) {
            return
        }
        if (!this.is_global_ctl_visible(ctl_type)) {
            return
        }
        this.cursor.select_ctl_at_global(beat, position, ctl_type)
    }
    open fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) {
            return
        }
        this.cursor.select_range(beat_key_a, beat_key_b)
    }
    open fun cursor_select_global_ctl_range(type:ControlEventType, first: Int, second: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.cursor.select_global_ctl_range(type, first, second)
    }
    open fun cursor_select_channel_ctl_range(type:ControlEventType, channel: Int, first: Int, second: Int) {
        if (this._block_cursor_selection()) {
            return
        }
        this.cursor.select_channel_ctl_range(type, channel, first, second)
    }
    open fun cursor_select_line_ctl_range(type: ControlEventType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) {
            return
        }
        this.cursor.select_line_ctl_range(type, beat_key_a, beat_key_b)
    }

    fun set_event_at_cursor(event: OpusControlEvent) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            null -> {
                // TODO: SPECIFY Exception
                throw Exception()
            }
            CtlLineLevel.Global -> {
                val (actual_beat, actual_position) = this.controller_global_get_actual_position<OpusControlEvent>(cursor.ctl_type!!, cursor.beat, cursor.get_position())
                this.controller_global_set_event(
                    cursor.ctl_type!!,
                    actual_beat,
                    actual_position,
                    event
                )
            }
            CtlLineLevel.Channel -> {
                val (actual_beat, actual_position) = this.controller_channel_get_actual_position<OpusControlEvent>(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position())
                this.controller_channel_set_event(
                    cursor.ctl_type!!,
                    cursor.channel,
                    actual_beat,
                    actual_position,
                    event
                )
            }
            CtlLineLevel.Line -> {
                val (actual_beat_key, actual_position) = this.controller_line_get_actual_position<OpusControlEvent>(cursor.ctl_type!!, cursor.get_beatkey(), cursor.get_position())
                this.controller_line_set_event(
                    cursor.ctl_type!!,
                    actual_beat_key,
                    actual_position,
                    event
                )
            }
        }
    }
    fun set_event_at_cursor(event: InstrumentEvent) {
        when (this.cursor.ctl_level) {
            null -> {
                val original = this.get_actual_position(
                    this.cursor.get_beatkey(),
                    this.cursor.get_position()
                )
                this.set_event(
                    original.first,
                    original.second,
                    event as InstrumentEvent
                )
            }
            else -> {
                // TODO: Specifiy Exception
                throw Exception()
            }
        }
    }
    fun set_percussion_event_at_cursor() {
        this.percussion_set_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }
    open fun remove_at_cursor(count: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            null -> {
                val beat_key = cursor.get_beatkey()
                val position = cursor.get_position().toMutableList()

                var working_tree = this.get_tree(beat_key).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, position, count)

                this.remove_repeat(beat_key, position, real_count)
            }

            CtlLineLevel.Global -> {
                val working_tree = this.get_global_ctl_tree<OpusControlEvent>(cursor.ctl_type!!, cursor.beat).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.repeat_controller_global_remove(cursor.ctl_type!!, cursor.beat, cursor.get_position(), real_count)

                this.cursor_select_ctl_at_global(
                    cursor.ctl_type!!,
                    cursor.beat,
                    this.get_first_position_global_ctl(cursor.ctl_type!!, cursor.beat, cursor_position)
                )
            }

            CtlLineLevel.Channel -> {
                val working_tree = this.get_channel_ctl_tree<OpusControlEvent>(cursor.ctl_type!!, cursor.channel, cursor.beat).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.repeat_controller_channel_remove(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position(), real_count)

                this.cursor_select_ctl_at_channel(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.beat,
                    this.get_first_position_channel_ctl(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor_position)
                )
            }

            CtlLineLevel.Line -> {
                val beat_key = cursor.get_beatkey()
                val working_tree = this.get_line_ctl_tree<OpusControlEvent>(cursor.ctl_type!!, beat_key).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)

                this.repeat_controller_line_remove(cursor.ctl_type!!, beat_key, cursor.get_position(), real_count)

                this.cursor_select_ctl_at_line(
                    cursor.ctl_type!!,
                    beat_key,
                    this.get_first_position_line_ctl(cursor.ctl_type!!, beat_key, cursor_position)
                )
            }
        }
    }

    private fun _post_new_line(channel: Int, line_offset: Int) {
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.channel == channel) {
                    if (this.cursor.line_offset >= line_offset) {
                        this.cursor.line_offset += 1
                    }
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.range!!
                if (first.channel == channel) {
                    if (first.line_offset >= line_offset) {
                        first.line_offset += 1
                    }
                }
                if (second.channel == channel) {
                    if (second.line_offset >= line_offset) {
                        second.line_offset += 1
                    }
                }
                this.cursor.range = Pair(first, second)
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset -> return
        }
        this.cursor_apply(this.cursor.copy())
    }

    fun get_tree(): OpusTree<out InstrumentEvent> {
        return this.get_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }
    fun unset() {
        when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Range -> {
                when (this.cursor.ctl_level) {
                    null -> {
                        val (first_key, second_key) = this.cursor.get_ordered_range()!!
                        this.unset_range(first_key, second_key)
                        this.cursor_select(first_key, listOf())
                    }
                    CtlLineLevel.Global -> {
                        val (key_a, key_b) = this.cursor.range!!
                        val start = min(key_a.beat, key_b.beat)
                        val end = max(key_a.beat, key_b.beat)
                        this.controller_global_unset_range(this.cursor.ctl_type!!, start, end)
                        this.cursor_select_ctl_at_global(this.cursor.ctl_type!!, start, listOf())
                    }
                    CtlLineLevel.Channel -> {
                        val (key_a, key_b) = this.cursor.range!!
                        val start = min(key_a.beat, key_b.beat)
                        val end = max(key_a.beat, key_b.beat)
                        this.controller_channel_unset_range(this.cursor.ctl_type!!, key_a.channel, start, end)
                        this.cursor_select_ctl_at_channel(this.cursor.ctl_type!!, key_a.channel, start, listOf())
                    }
                    CtlLineLevel.Line -> {
                        val (first_key, second_key) = this.cursor.get_ordered_range()!!
                        this.controller_line_unset_range(this.cursor.ctl_type!!, first_key, second_key)
                        this.cursor_select_ctl_at_line(this.cursor.ctl_type!!, first_key, listOf())
                    }
                }
            }
            OpusManagerCursor.CursorMode.Single -> {
                when (this.cursor.ctl_level) {
                    null -> {
                        val beat_key = this.cursor.get_beatkey()
                        val position = this.cursor.get_position()
                        val real_position = this.get_actual_position(beat_key, position)

                        this.unset(real_position.first, real_position.second)
                    }
                    CtlLineLevel.Global -> {
                        val beat = this.cursor.beat
                        val position = this.cursor.get_position()
                        val real_position = this.controller_global_get_actual_position<OpusControlEvent>(this.cursor.ctl_type!!, beat, position)

                        this.controller_global_unset(this.cursor.ctl_type!!, real_position.first, real_position.second)
                    }
                    CtlLineLevel.Channel -> {
                        val channel = this.cursor.channel
                        val beat = this.cursor.beat
                        val position = this.cursor.get_position()
                        val real_position = this.controller_channel_get_actual_position<OpusControlEvent>(this.cursor.ctl_type!!, channel, beat, position)

                        this.controller_channel_unset(this.cursor.ctl_type!!, channel, real_position.first, real_position.second)
                    }
                    CtlLineLevel.Line -> {
                        val beat_key = this.cursor.get_beatkey()
                        val position = this.cursor.get_position()
                        val real_position = this.controller_line_get_actual_position<OpusControlEvent>(this.cursor.ctl_type!!, beat_key, position)

                        this.controller_line_unset(this.cursor.ctl_type!!, real_position.first, real_position.second)
                    }
                }

            }
            OpusManagerCursor.CursorMode.Column -> {
                this.unset_beat(this.cursor.beat)
            }
            OpusManagerCursor.CursorMode.Line -> {
                when (this.cursor.ctl_level) {
                    null -> {
                        this.unset_line(this.cursor.channel, this.cursor.line_offset)
                    }
                    CtlLineLevel.Global -> {
                        this.controller_global_unset_line(this.cursor.ctl_type!!)
                    }
                    CtlLineLevel.Channel -> {
                        val channel = this.cursor.channel
                        this.controller_channel_unset_line(this.cursor.ctl_type!!, channel)
                    }
                    CtlLineLevel.Line -> {
                        this.controller_line_unset_line(this.cursor.ctl_type!!, this.cursor.channel, this.cursor.line_offset)
                    }
                }
            }
            OpusManagerCursor.CursorMode.Channel -> {
                TODO()
            }
            OpusManagerCursor.CursorMode.Unset -> {}
        }
    }
    fun convert_event_to_absolute() {
        this.convert_event_to_absolute(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }
    fun convert_event_to_relative() {
        this.convert_event_to_relative(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }
    fun set_percussion_instrument(instrument: Int) {
        this.percussion_set_instrument(
            this.cursor.line_offset,
            instrument
        )
    }
    fun split_tree_at_cursor(splits: Int, move_event_to_end: Boolean = false) {
        this.split_tree(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            splits,
            move_event_to_end
        )
    }
    fun insert_after_cursor(count: Int) {
        this.insert_after_repeat(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            count
        )
    }
    fun insert_at_cursor(repeat: Int) {
        this.insert_repeat(
            this.cursor.get_beatkey(),
            this.cursor.get_position(),
            repeat
        )
    }

    fun <T> _calculate_new_position_after_remove(working_tree: OpusTree<T>, position: List<Int>, count: Int): Pair<Int, List<Int>> {
        val cursor_position = position.toMutableList()
        var real_count = 0
        for (i in 0 until count) {
            if (cursor_position.isEmpty()) {
                break
            }

            val parent = working_tree.get(cursor_position.subList(0, cursor_position.size - 1))
            if (parent.size == 2) {
                parent.set_event(null)
                cursor_position.removeLast()
            } else if (cursor_position.last() == parent.size - 1) {
                parent[cursor_position.last()].detach()
                cursor_position[cursor_position.size - 1] -= 1
            } else {
                parent[cursor_position.last()].detach()
            }
            real_count += 1
        }
        return Pair(real_count, cursor_position)
    }

    //fun controller_global_remove(count: Int) {
    //    val cursor = this.cursor
    //    val beat_key = cursor.get_beatkey()
    //    val position = cursor.get_position().toMutableList()

    //    val tree = this.get_tree()
    //    val cursor_position = position.toMutableList()
    //    if (tree.parent!!.size <= 2) { // Will be pruned
    //        cursor_position.removeLast()
    //    } else if (position.last() == tree.parent!!.size - 1) {
    //        cursor_position[cursor_position.size - 1] -= 1
    //    }

    //    this.remove(beat_key, position, count)

    //    this.cursor_select(beat_key, this.get_first_position(beat_key, cursor_position))
    //}
    fun insert_line_at_cursor(count: Int) {
        this.new_line_repeat(
            this.cursor.channel,
            this.cursor.line_offset + 1,
            count
        )
    }
    fun remove_line_at_cursor(count: Int) {
        this.remove_line_repeat(
            this.cursor.channel,
            this.cursor.line_offset,
            count
        )
    }

    fun remove_beat_at_cursor(count: Int) {
        this.remove_beat(this.cursor.beat, count)
    }
    fun insert_beat_after_cursor(count: Int) {
        this.insert_beats(this.cursor.beat + 1, count)
    }
    fun insert_beat_at_cursor(count: Int) {
        this.insert_beats(this.cursor.beat, count)
    }
    fun merge_into_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                TODO()
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.merge_leafs(first, listOf(), beat_key, listOf())
            }
        } else {
            throw InvalidCursorState()
        }
    }
    fun move_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.move_beat_range(beat_key, first, second)
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.move_leaf(first, listOf(), beat_key, listOf())
            }
        } else {
            throw InvalidCursorState()
        }
    }
    fun copy_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            if (first != second) {
                this.overwrite_beat_range(beat_key, first, second)
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.replace_tree(
                    beat_key,
                    listOf(),
                    this.get_tree_copy(
                        first,
                        listOf()
                    )
                )
            }
        } else {
            throw InvalidCursorState()
        }
    }
    fun copy_line_ctl_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            when (this.cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first != second) {
                        this.controller_line_overwrite_range(this.cursor.ctl_type!!, beat_key, first, second)
                    } else {
                        this.controller_line_replace_tree(this.cursor.ctl_type!!, beat_key, listOf(), this.get_line_ctl_tree_copy(this.cursor.ctl_type!!, first, listOf()))
                    }
                }
                CtlLineLevel.Channel -> {
                    if (first != second) {
                        this.controller_channel_to_line_overwrite_range(this.cursor.ctl_type!!, first.channel, first.beat, second.beat, beat_key)
                    } else {
                        this.controller_line_replace_tree(
                            this.cursor.ctl_type!!,
                            beat_key,
                            listOf(),
                            this.get_channel_ctl_tree_copy(this.cursor.ctl_type!!, first.channel, first.beat, listOf())
                        )
                    }
                }
                CtlLineLevel.Global -> {
                    if (first != second) {
                        this.controller_global_to_line_overwrite_range(this.cursor.ctl_type!!, first.beat, second.beat, beat_key)
                    } else {
                        this.controller_line_replace_tree(this.cursor.ctl_type!!, beat_key, listOf(), this.get_global_ctl_tree_copy(this.cursor.ctl_type!!, first.beat, listOf()))
                    }
                }
                null -> TODO()
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_line_ctl_tree<OpusControlEvent>(
            this.cursor.ctl_type!!,
            beat_key,
            listOf()
        )

        this.cursor_select_ctl_at_line(
            this.cursor.ctl_type!!,
            beat_key,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }
    fun move_line_ctl_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            when (this.cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first != second) {
                        this.controller_line_move_range(this.cursor.ctl_type!!, beat_key, first, second)
                    } else {
                        this.controller_line_move_leaf(this.cursor.ctl_type!!, first, listOf(), beat_key, listOf())
                    }
                }
                CtlLineLevel.Channel -> {
                    if (first != second) {
                        this.controller_channel_to_line_move_range(this.cursor.ctl_type!!, first.channel, first.beat, second.beat, beat_key)
                    } else {
                        this.controller_channel_to_line_move_leaf(this.cursor.ctl_type!!, first.channel, first.beat, listOf(), beat_key, listOf())
                    }
                }

                CtlLineLevel.Global -> {
                    if (first != second) {
                        this.controller_global_to_line_move_range(this.cursor.ctl_type!!, first.beat, second.beat, beat_key)
                    } else {
                        this.controller_global_to_line_move_leaf(this.cursor.ctl_type!!, first.beat, listOf(), beat_key, listOf())
                    }
                }
                null -> TODO()
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_line_ctl_tree<OpusControlEvent>(
            this.cursor.ctl_type!!,
            beat_key,
            listOf()
        )

        this.cursor_select_ctl_at_line(
            this.cursor.ctl_type!!,
            beat_key,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }

    fun copy_channel_ctl_to_beat(channel: Int, beat: Int) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            when (this.cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first != second) {
                        this.controller_line_to_channel_overwrite_range(this.cursor.ctl_type!!, first.channel, first.line_offset, first.beat, second.beat, channel, beat)
                    } else {
                        this.controller_channel_replace_tree(this.cursor.ctl_type!!, channel, beat, listOf(), this.get_line_ctl_tree_copy(this.cursor.ctl_type!!, first))
                    }
                }
                CtlLineLevel.Channel -> {
                    if (first != second) {
                        this.controller_channel_overwrite_range(this.cursor.ctl_type!!, channel, beat, this.cursor.channel, first.beat, second.beat)
                    } else {
                        this.controller_channel_replace_tree(this.cursor.ctl_type!!, channel, beat, listOf(), this.get_channel_ctl_tree_copy(this.cursor.ctl_type!!, this.cursor.channel, first.beat, listOf()))
                    }
                }
                CtlLineLevel.Global -> {
                    if (first != second) {
                        this.controller_global_to_channel_overwrite_range(this.cursor.ctl_type!!, first.beat, second.beat, channel, beat)
                    } else {
                        this.controller_channel_replace_tree(this.cursor.ctl_type!!, channel, beat, listOf(), this.get_global_ctl_tree_copy(this.cursor.ctl_type!!, first.beat, listOf()))
                    }
                }
                null -> {}
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_channel_ctl_tree<OpusControlEvent>(this.cursor.ctl_type!!, channel, beat, listOf())
        this.cursor_select_ctl_at_channel(this.cursor.ctl_type!!, channel, beat, tree.get_first_event_tree_position() ?: listOf())
    }

    fun move_channel_ctl_to_beat(channel: Int, beat: Int) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            when (this.cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first != second) {
                        this.controller_line_to_channel_move_range(this.cursor.ctl_type!!, first.channel, first.line_offset, first.beat, second.beat, channel, beat)
                    } else {
                        this.controller_line_to_channel_move_leaf(this.cursor.ctl_type!!, first, listOf(), channel, beat, listOf())
                    }
                }
                CtlLineLevel.Channel -> {
                    if (first != second) {
                        this.controller_channel_move_range(this.cursor.ctl_type!!, channel, beat, first.channel, first.beat, second.beat)
                    } else {
                        this.controller_channel_move_leaf(this.cursor.ctl_type!!, first.channel, first.beat, listOf(), channel, beat, listOf())
                    }
                }
                CtlLineLevel.Global -> {
                    if (first != second) {
                        this.controller_global_to_channel_move_range(this.cursor.ctl_type!!, beat, first.channel, first.beat, second.beat)
                    } else {
                        this.controller_global_to_channel_move_leaf(this.cursor.ctl_type!!, beat, listOf(), first.channel, first.beat, listOf())
                    }

                }
                null -> TODO()
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_channel_ctl_tree<OpusControlEvent>(
            this.cursor.ctl_type!!,
            channel,
            beat,
            listOf()
        )

        this.cursor_select_ctl_at_channel(
            this.cursor.ctl_type!!,
            channel,
            beat,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }

    fun copy_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) {
            throw InvalidOverwriteCall()
        }

        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            when (this.cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first != second) {
                        this.controller_line_to_global_overwrite_range(this.cursor.ctl_type!!, first.channel, first.line_offset, first.beat, second.beat, beat)
                    } else {
                        this.controller_global_replace_tree(this.cursor.ctl_type!!, beat, listOf(), this.get_line_ctl_tree_copy(this.cursor.ctl_type!!, first, listOf()))
                    }
                }
                CtlLineLevel.Channel -> {
                    if (first != second) {
                        this.controller_channel_to_global_overwrite_range(this.cursor.ctl_type!!, beat, first.channel, first.beat, second.beat)
                    } else {
                        this.controller_global_replace_tree(this.cursor.ctl_type!!, beat, listOf(), this.get_channel_ctl_tree_copy(this.cursor.ctl_type!!, first.channel, first.beat, listOf()))
                    }
                }
                CtlLineLevel.Global -> {
                    if (first != second) {
                        this.controller_global_overwrite_range(this.cursor.ctl_type!!, beat, first.beat, second.beat)
                    } else {
                        this.controller_global_replace_tree(this.cursor.ctl_type!!, beat, listOf(), this.get_global_ctl_tree(this.cursor.ctl_type!!, first.beat, listOf()))
                    }
                }
                null -> {}
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_global_ctl_tree<OpusControlEvent>(
            this.cursor.ctl_type!!,
            beat,
            listOf()
        )

        this.cursor_select_ctl_at_global(
            this.cursor.ctl_type!!,
            beat,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }
    fun move_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) {
            throw InvalidOverwriteCall()
        }

        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.range!!
            when (this.cursor.ctl_level) {
                CtlLineLevel.Line -> {
                    if (first != second) {
                        this.controller_line_to_global_move_range(this.cursor.ctl_type!!, first.channel, first.line_offset, first.beat, second.beat, beat)
                    } else {
                        this.controller_line_to_global_move_leaf(this.cursor.ctl_type!!, first, listOf(), beat, listOf())
                    }
                }
                CtlLineLevel.Channel -> {
                    if (first != second) {
                        this.controller_channel_to_global_move_range(this.cursor.ctl_type!!, beat, first.channel, first.beat, second.beat)
                    } else {
                        this.controller_channel_to_global_move_leaf(this.cursor.ctl_type!!, first.channel, first.beat, listOf(), beat, listOf())
                    }
                }
                CtlLineLevel.Global -> {
                    if (first != second) {
                        this.controller_global_move_range(this.cursor.ctl_type!!, beat, first.beat, second.beat)
                    } else {
                        this.controller_global_move_leaf(this.cursor.ctl_type!!, first.beat, listOf(), beat, listOf())
                    }
                }
                null -> TODO()
            }
        } else {
            throw InvalidCursorState()
        }

        val tree = this.get_global_ctl_tree<OpusControlEvent>(
            this.cursor.ctl_type!!,
            beat,
            listOf()
        )

        this.cursor_select_ctl_at_global(
            this.cursor.ctl_type!!,
            beat,
            tree.get_first_event_tree_position() ?: listOf()
        )
    }

    fun move_to_previous_visible_line(repeat: Int = 1) {
        val cursor = this.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw Exception("Incorrect Cursor Mode ${cursor.mode}")
        }

        var visible_row = when (cursor.ctl_level) {
            null -> {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )

            }
            CtlLineLevel.Line -> {
                this.get_visible_row_from_ctl_line_line(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset
                )
            }
            CtlLineLevel.Channel -> {
                this.get_visible_row_from_ctl_line_channel(
                    cursor.ctl_type!!,
                    cursor.channel
                )
            }
            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
        }!!

        visible_row = kotlin.math.max(0, visible_row - repeat)

        val (pointer, control_level, control_type) = this.get_ctl_line_info(
            this.get_ctl_line_from_row(visible_row)
        )

        when (control_level) {
            null -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line(new_channel, new_line_offset)

            }
            CtlLineLevel.Line -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line_ctl_line(
                    control_type!!,
                    new_channel,
                    new_line_offset,
                )
            }
            CtlLineLevel.Channel -> {
                this.cursor_select_channel_ctl_line(
                    control_type!!,
                    pointer
                )
            }
            CtlLineLevel.Global -> this.cursor_select_global_ctl_line(control_type!!)
        }
    }

    fun move_to_next_visible_line(repeat: Int = 1) {
        val cursor = this.cursor
        if (cursor.mode != OpusManagerCursor.CursorMode.Line) {
            throw Exception("Incorrect Cursor Mode ${cursor.mode}")
        }

        var visible_row = when (cursor.ctl_level) {
            null -> {
                this.get_visible_row_from_ctl_line(
                    this.get_actual_line_index(
                        this.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )

            }
            CtlLineLevel.Line -> {
                this.get_visible_row_from_ctl_line_line(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset
                )
            }
            CtlLineLevel.Channel -> {
                this.get_visible_row_from_ctl_line_channel(
                    cursor.ctl_type!!,
                    cursor.channel
                )
            }
            CtlLineLevel.Global -> this.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
        }!!

        visible_row = kotlin.math.max(0, kotlin.math.min(this.get_total_line_count() - 1, visible_row + repeat))

        val (pointer, control_level, control_type) = this.get_ctl_line_info(
            this.get_ctl_line_from_row(visible_row)
        )

        when (control_level) {
            null -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line(new_channel, new_line_offset)

            }
            CtlLineLevel.Line -> {
                val (new_channel, new_line_offset) = this.get_channel_and_line_offset(pointer)
                this.cursor_select_line_ctl_line(
                    control_type!!,
                    new_channel,
                    new_line_offset,
                )
            }
            CtlLineLevel.Channel -> {
                this.cursor_select_channel_ctl_line(
                    control_type!!,
                    pointer
                )
            }
            CtlLineLevel.Global -> this.cursor_select_global_ctl_line(control_type!!)
        }
    }

    fun select_next_leaf(repeat: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat_key,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }

            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.get_position()
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_proceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }

            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.get_position()
                for (i in 0 until repeat) {
                    val next_pair = this.get_global_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_proceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    fun select_previous_leaf(repeat: Int) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()
                val controller = this.channels[working_beat_key.channel].lines[working_beat_key.line_offset].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat_key.beat,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }

            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.get_position()
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }

            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.get_position()
                val controller = this.controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)
                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_preceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    fun select_first_leaf_in_previous_beat(repeat: Int = 1) {
        when (this.cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat = this.cursor.beat
                var working_position = this.cursor.get_position()
                val controller = this.channels[this.cursor.channel].lines[this.cursor.line_offset].controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(this.cursor.ctl_type!!, BeatKey(working_beat, this.cursor.channel, this.cursor.line_offset), working_position)
            }
            CtlLineLevel.Channel -> {
                var working_beat = this.cursor.beat
                val channel = this.cursor.channel
                var working_position = this.cursor.get_position()
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(this.cursor.ctl_type!!, channel, working_beat, working_position)
            }
            CtlLineLevel.Global -> {
                var working_beat = this.cursor.beat
                var working_position = this.cursor.get_position()
                val controller = this.controllers.get_controller<OpusControlEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(this.cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = this.cursor.get_beatkey()
                var working_position = this.cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_preceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    fun select_first_leaf_in_next_beat(repeat: Int = 1) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat_key,
                        working_position
                    ) ?: break

                    working_beat_key.beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
            }
            CtlLineLevel.Channel -> {
                var working_beat = cursor.beat
                val channel = cursor.channel
                var working_position = cursor.get_position()
                val controller = this.channels[channel].controllers.get_controller<OpusControlEvent>(cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_proceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
            }
            CtlLineLevel.Global -> {
                var working_beat = cursor.beat
                var working_position = cursor.get_position()
                for (i in 0 until repeat) {
                    val next_pair = this.get_global_ctl_proceding_leaf_position(
                        cursor.ctl_type!!,
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

            }
            null -> {
                var working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_proceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.cursor_select(working_beat_key, working_position)
            }
        }
    }
    // End Cursor Functions ////////////////////////////////////////////////////////////////////////

    fun is_selected(beat_key: BeatKey, position: List<Int>): Boolean {
        if (this.cursor.ctl_level != null) {
            return false
        }

        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line -> {
                false
            }
            OpusManagerCursor.CursorMode.Single -> {
                val cbeat_key = this.cursor.get_beatkey()
                val cposition = this.cursor.get_position()
                cbeat_key == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }

            OpusManagerCursor.CursorMode.Channel -> {
                false
            }
        }
    }
    fun is_secondary_selection(beat_key: BeatKey, position: List<Int>): Boolean {
        if (this.cursor.ctl_level != null) {
            return false
        }

        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val cbeat_key = this.cursor.get_beatkey()
                val cposition = this.cursor.get_position()
                if (cbeat_key == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                    return false
                }
                if (cbeat_key.channel != beat_key.channel || cbeat_key.line_offset != beat_key.line_offset) {
                    return false
                }

                var output = false
                val line = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset]
                for ((working_beat, working_position) in line.get_all_blocked_positions(beat_key.beat, position)) {
                    if (working_beat == beat_key.beat && position == working_position) {
                        continue
                    }
                    if (cbeat_key.beat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                        output = true
                        break
                    }
                }
                output
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                beat_key in this.get_beatkeys_in_range(first, second)
            }
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor.line_offset == beat_key.line_offset && this.cursor.channel == beat_key.channel
            }
            OpusManagerCursor.CursorMode.Channel -> {
                beat_key.channel == this.cursor.channel
            }
            else -> {
                false
            }
        }

    }
    fun is_global_control_selected(control_type: ControlEventType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) {
                    val cposition = this.cursor.get_position()
                    beat == this.cursor.beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
                } else {
                    false
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val first_beat = min(this.cursor.range!!.first.beat, this.cursor.range!!.second.beat)
                val second_beat = max(this.cursor.range!!.first.beat, this.cursor.range!!.second.beat)

                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) && (beat == second_beat || beat == first_beat)
            }
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Line -> {
                false
            }

        }
    }
    fun is_global_control_secondary_selected(control_type: ControlEventType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Global || control_type != this.cursor.ctl_type) {
                    false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    var output = false
                    val controller = this.controllers.get_controller<OpusControlEvent>(control_type)
                    for ((working_beat, working_position) in controller.get_all_blocked_positions(beat, position)) {
                        if (working_beat == beat && position == working_position) {
                            continue
                        }

                        if (cbeat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                            output = true
                            break
                        }
                    }
                    output
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) && (beat in first.beat + 1 until second.beat)
            }
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type)
            }
            else -> {
                false
            }
        }
    }
    fun is_channel_control_selected(control_type: ControlEventType, channel: Int, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
                        && beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (beat == first.beat || beat == second.beat) && (this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type)
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }

        }
    }
    fun is_channel_control_secondary_selected(control_type: ControlEventType, channel: Int, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                beat == this.cursor.beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor.channel == channel && this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type
            }
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Channel || this.cursor.ctl_type != control_type) {
                    false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    var output = false
                    val controller = this.get_all_channels()[channel].controllers.get_controller<OpusControlEvent>(control_type)
                    for ((working_beat, working_position) in controller.get_all_blocked_positions(beat, position)) {
                        if (working_beat == beat && position == working_position) {
                            continue
                        }

                        if (cbeat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                            output = true
                            break
                        }
                    }
                    output
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                beat in first.beat + 1 until second.beat && this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type
            }

            OpusManagerCursor.CursorMode.Channel -> {
                this.cursor.ctl_level != CtlLineLevel.Global && this.cursor.channel == channel
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }
        }
    }
    fun is_line_control_selected(control_type: ControlEventType, beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                this.cursor.channel == beat_key.channel
                        && control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
                        && this.cursor.line_offset == beat_key.line_offset
                        && beat_key.beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            OpusManagerCursor.CursorMode.Range -> {
                (beat_key == this.cursor.range!!.first || beat_key == this.cursor.range!!.second) && control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Line
            }
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Line -> {
                false
            }
        }
    }
    fun is_line_control_secondary_selected(control_type: ControlEventType, beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            OpusManagerCursor.CursorMode.Line -> {
                this.cursor.channel == beat_key.channel
                        && this.cursor.line_offset == beat_key.line_offset
                        && control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
            }
            OpusManagerCursor.CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Line || control_type != this.cursor.ctl_type) {
                     false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    val beat = beat_key.beat
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    var output = false
                    val controller = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].controllers.get_controller<OpusControlEvent>(control_type)
                    for ((working_beat, working_position) in controller.get_all_blocked_positions(beat, position)) {
                        if (working_beat == beat && position == working_position) {
                            continue
                        }

                        if (cbeat == working_beat && working_position.size >= cposition.size && working_position.subList(0, cposition.size) == cposition) {
                            output = true
                            break
                        }
                    }
                    output
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Line && beat_key in this.get_beatkeys_in_range(first, second)
            }
            OpusManagerCursor.CursorMode.Unset -> {
                false
            }

            OpusManagerCursor.CursorMode.Channel -> {
                this.cursor.channel == beat_key.channel
            }
        }
    }

    fun is_line_control_line_selected(control_type: ControlEventType, channel: Int, line_offset: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                val on_ctl_line = (this.cursor.ctl_level == CtlLineLevel.Line && control_type == this.cursor.ctl_type && this.cursor.line_offset == line_offset)
                val on_ctl_channel = this.cursor.ctl_level == CtlLineLevel.Channel
                this.cursor.channel == channel && (on_ctl_line || on_ctl_channel)

                        //&& !this.get_all_channels()[channel].lines[line_offset].controllers.has_controller(control_type)
            }
            OpusManagerCursor.CursorMode.Range -> {
                val target = this.get_instrument_line_index(channel, line_offset)
                val first = this.get_instrument_line_index(this.cursor.range!!.first.channel, this.cursor.range!!.first.line_offset)
                val second = this.get_instrument_line_index(this.cursor.range!!.second.channel, this.cursor.range!!.second.line_offset)
                (this.cursor.ctl_type == null || (control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Line)) && (first .. second).contains(target)
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> false
            OpusManagerCursor.CursorMode.Channel -> {
                channel == this.cursor.channel
            }

        }
    }

    fun is_channel_control_line_selected(control_type: ControlEventType, channel: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
            }
            OpusManagerCursor.CursorMode.Channel -> {
                channel == this.cursor.channel
            }
            OpusManagerCursor.CursorMode.Range -> {
                control_type == this.cursor.ctl_type && this.cursor.channel == channel && this.cursor.ctl_level == CtlLineLevel.Channel
            }
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column -> false
        }
    }
    fun is_global_control_line_selected(control_type: ControlEventType): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Range -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Global
            }
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Unset,
            OpusManagerCursor.CursorMode.Column -> false

        }

    }
    fun is_beat_selected(beat: Int): Boolean {
        return when (this.cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Column -> {
                this.cursor.beat == beat
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (min(first.beat, second.beat) .. max(first.beat, second.beat)).contains(beat)
            }
            else -> false
        }
    }
    fun select_first_in_beat(beat: Int) {
        when (this.cursor.ctl_level) {
            null -> {
                val new_beat_key = BeatKey(this.cursor.channel, cursor.line_offset, beat)
                val new_position = this.get_first_position(new_beat_key, listOf())
                this.cursor_select(
                    new_beat_key,
                    new_position
                )
            }
            CtlLineLevel.Line -> {
                val new_beat_key = BeatKey(this.cursor.channel, cursor.line_offset, beat)
                val new_position = this.get_first_position_line_ctl(this.cursor.ctl_type!!, new_beat_key, listOf())
                this.cursor_select_ctl_at_line(
                    this.cursor.ctl_type!!,
                    new_beat_key,
                    new_position
                )
            }
            CtlLineLevel.Channel -> {
                val new_position = this.get_first_position_channel_ctl(this.cursor.ctl_type!!, cursor.channel, beat, listOf())
                this.cursor_select_ctl_at_channel(
                    this.cursor.ctl_type!!,
                    this.cursor.channel,
                    beat,
                    new_position
                )
            }
            CtlLineLevel.Global -> {
                val new_position = this.get_first_position_global_ctl(this.cursor.ctl_type!!, beat, listOf())
                this.cursor_select_ctl_at_global(
                    this.cursor.ctl_type!!,
                    beat,
                    new_position
                )
            }
        }
    }

    fun <T: OpusControlEvent> set_initial_event(event: T) {
        when (this.cursor.ctl_level) {
            null -> return
            CtlLineLevel.Line -> this.controller_line_set_initial_event(this.cursor.ctl_type!!, this.cursor.channel, this.cursor.line_offset, event)
            CtlLineLevel.Channel -> this.controller_channel_set_initial_event(this.cursor.ctl_type!!, this.cursor.channel, event)
            CtlLineLevel.Global -> this.controller_global_set_initial_event(this.cursor.ctl_type!!, event)
        }
    }

    fun get_active_active_control_set(): ActiveControlSet? {
        val channels = this.get_all_channels()

        val cursor = this.cursor
        return when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                channels[cursor.channel].lines[cursor.line_offset].controllers
            }
            CtlLineLevel.Channel -> {
                val channel = cursor.channel
                channels[channel].controllers
            }
            CtlLineLevel.Global -> {
                this.controllers
            }
            else -> null
        }
    }

    fun get_nth_next_channel_at_cursor(n: Int): Int? {
        return when (cursor.mode) {
            OpusManagerCursor.CursorMode.Channel,
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                val start_channel = when (cursor.ctl_level) {
                    CtlLineLevel.Global -> 0
                    null,
                    CtlLineLevel.Line,
                    CtlLineLevel.Channel -> cursor.channel
                }

                kotlin.math.max(0, kotlin.math.min(start_channel + n, this.get_visible_channel_count() - 1))
            }

            OpusManagerCursor.CursorMode.Column -> {
                kotlin.math.max(0, kotlin.math.min(n - 1, this.get_visible_channel_count() - 1))
            }

            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Unset -> null
        }
    }

    internal fun _block_cursor_selection(): Boolean {
        return (this._blocked_action_catcher > 0)
    }
}
