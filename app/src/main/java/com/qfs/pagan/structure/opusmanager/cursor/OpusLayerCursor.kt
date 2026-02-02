package com.qfs.pagan.structure.opusmanager.cursor
import androidx.compose.ui.graphics.Color
import com.qfs.pagan.RelativeInputMode
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.InvalidOverwriteCall
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.OpusChannelAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLineAbstract
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectControlSet
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.abs

open class OpusLayerCursor: OpusLayerBase() {
    var cursor = OpusManagerCursor()
    private var _cursor_lock = 0

    internal fun <T> lock_cursor(callback: () -> T): T {
        this._cursor_lock += 1
        val output = try {
            callback()
        } catch (e: Exception) {
            this._cursor_lock -= 1
            throw e
        }

        this._cursor_lock -= 1

        return output
    }

    // BASE FUNCTIONS vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    /* ------------------- 1st Order Functions ---------------------------------- */
    override fun insert(beat_key: BeatKey, position: List<Int>) {
        super.insert(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    override fun controller_line_insert(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_insert(type, beat_key, position)
        this.cursor_select_ctl_at_line(type, beat_key, position)
    }

    override fun controller_channel_insert(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_insert(type, channel, beat, position)
        this.cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    override fun controller_global_insert(type: EffectType, beat: Int, position: List<Int>) {
        super.controller_global_insert(type, beat, position)
        this.cursor_select_ctl_at_global(type, beat, position)
    }

    override fun percussion_set_event(beat_key: BeatKey, position: List<Int>) {
        super.percussion_set_event(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    override fun channel_set_preset(channel: Int, instrument: Pair<Int, Int>) {
        super.channel_set_preset(channel, instrument)
        this.cursor_select_channel(channel)
    }

    override fun replace_tree(beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<out InstrumentEvent>) {
        super.replace_tree(beat_key, position, tree)
        this.cursor_select(beat_key, this.get_first_position(beat_key, position))
    }

    override fun <T : EffectEvent> controller_line_replace_tree(type: EffectType, beat_key: BeatKey, position: List<Int>?, tree: ReducibleTree<T>) {
        super.controller_line_replace_tree(type, beat_key, position, tree)
        val new_position = this.get_first_position_line_ctl(type, beat_key, position ?: listOf())
        if (this.is_line_ctl_visible(type, beat_key.channel, beat_key.line_offset)) {
            this.cursor_select_ctl_at_line(type, beat_key, new_position)
        }
    }

    override fun <T : EffectEvent> controller_channel_replace_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        super.controller_channel_replace_tree(type, channel, beat, position, tree)
        val new_position = this.get_first_position_channel_ctl(type, channel, beat, position ?: listOf())
        this.cursor_select_ctl_at_channel(type, channel, beat, new_position)
    }

    override fun <T : EffectEvent> controller_global_replace_tree(type: EffectType, beat: Int, position: List<Int>?, tree: ReducibleTree<T>) {
        super.controller_global_replace_tree(type, beat, position, tree)
        val new_position = this.get_first_position_global_ctl(type, beat, position ?: listOf())
        this.cursor_select_ctl_at_global(type, beat, new_position)
    }

    override fun <T : EffectEvent> controller_line_set_event(type: EffectType, beat_key: BeatKey, position: List<Int>, event: T) {
        super.controller_line_set_event(type, beat_key, position, event)

        if (this.is_line_ctl_visible(type, beat_key.channel, beat_key.line_offset)) {
            this.cursor_select_ctl_at_line(type, beat_key, position)
        }
    }

    override fun <T : EffectEvent> controller_channel_set_event(type: EffectType, channel: Int, beat: Int, position: List<Int>, event: T) {
        super.controller_channel_set_event(type, channel, beat, position, event)
        if (this.is_channel_ctl_visible(type, channel)) {
            this.cursor_select_ctl_at_channel(type, channel, beat, position)
        }
    }

    override fun <T : EffectEvent> controller_global_set_event(type: EffectType, beat: Int, position: List<Int>, event: T) {
        super.controller_global_set_event(type, beat, position, event)
        if (this.is_global_ctl_visible(type)) {
            this.cursor_select_ctl_at_global(type, beat, position)
        }
    }

    override fun unset(beat_key: BeatKey, position: List<Int>) {
        super.unset(beat_key, position)
        this.cursor_select(beat_key, position)
    }

    override fun controller_line_unset(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_unset(type, beat_key, position)
        this.cursor_select_ctl_at_line(type, beat_key, position)
    }

    override fun controller_channel_unset(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_unset(type, channel, beat, position)
        this.cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    override fun controller_global_unset(type: EffectType, beat: Int, position: List<Int>) {
        super.controller_global_unset(type, beat, position)
        this.cursor_select_ctl_at_global(type, beat, position)
    }

    override fun split_tree(beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.split_tree(beat_key, position, splits, move_event_to_end)

        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select(beat_key, new_position)
    }

    override fun controller_global_split_tree(type: EffectType, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.controller_global_split_tree(type, beat, position, splits, move_event_to_end)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_global(type, beat, new_position)
    }

    override fun controller_channel_split_tree(type: EffectType, channel: Int, beat: Int, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.controller_channel_split_tree(type, channel, beat, position, splits, move_event_to_end)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_channel(type, channel, beat, new_position)
    }

    override fun controller_line_split_tree(type: EffectType, beat_key: BeatKey, position: List<Int>, splits: Int, move_event_to_end: Boolean) {
        super.controller_line_split_tree(type, beat_key, position, splits, move_event_to_end)
        val new_position = position.toMutableList()
        new_position.add(0)
        this.cursor_select_ctl_at_line(type, beat_key, new_position)
    }

    override fun <T : InstrumentEvent> set_event(beat_key: BeatKey, position: List<Int>, event: T) {
        super.set_event(beat_key, position, event)
        this.cursor_select(beat_key, position)
    }

    override fun set_duration(beat_key: BeatKey, position: List<Int>, duration: Int) {
        super.set_duration(beat_key, position, duration)
        this.cursor_select(beat_key, position)
    }

    override fun set_duration(type: EffectType, beat: Int, position: List<Int>, duration: Int) {
        super.set_duration(type, beat, position, duration)
        this.cursor_select_ctl_at_global(type, beat, position)
    }

    override fun set_duration(type: EffectType, beat_key: BeatKey, position: List<Int>, duration: Int) {
        super.set_duration(type, beat_key, position, duration)
        this.cursor_select_ctl_at_line(type, beat_key, position)
    }

    override fun set_duration(type: EffectType, channel: Int, beat: Int, position: List<Int>, duration: Int) {
        super.set_duration(type, channel, beat, position, duration)
        this.cursor_select_ctl_at_channel(type, channel, beat, position)
    }

    override fun swap_lines(channel_index_a: Int, line_offset_a: Int, channel_index_b: Int, line_offset_b: Int) {
        super.swap_lines(channel_index_a, line_offset_a, channel_index_b, line_offset_b)
        this.cursor_select_line(channel_index_b, line_offset_b)
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

    override fun move_channel(channel_index: Int, new_channel_index: Int) {
        super.move_channel(channel_index, new_channel_index)
        this.cursor_select_channel(
            if (channel_index < new_channel_index) {
                new_channel_index - 1
            } else {
                new_channel_index
            }
        )
    }

    override fun new_channel(channel: Int?, lines: Int, uuid: Int?, is_percussion: Boolean) {
        super.new_channel(channel, lines, uuid, is_percussion)
        this.cursor_select_channel(channel ?: (this.channels.size - 1))
    }

    override fun new_line(channel: Int, line_offset: Int?) {
        super.new_line(channel, line_offset)
        this.cursor_select_line(channel, line_offset ?: (this.get_all_channels()[channel].lines.size - 1))
    }

    override fun insert_line(channel: Int, line_offset: Int, line: OpusLineAbstract<*>) {
        super.insert_line(channel, line_offset, line)
        this.cursor_select_line(channel, line_offset)
    }

    override fun remove_line_controller(type: EffectType, channel_index: Int, line_offset: Int) {
        super.remove_line_controller(type, channel_index, line_offset)
        this.cursor_select_line(channel_index, line_offset)
    }

    override fun remove_channel_controller(type: EffectType, channel_index: Int) {
        super.remove_channel_controller(type, channel_index)
        this.cursor_select_channel(channel_index)
    }

    override fun remove_channel(channel: Int) {
        super.remove_channel(channel)
        this.cursor_select_channel(max(0, min(channel, this.channels.size - 1)))
    }

    override fun remove_beat(beat_index: Int, count: Int) {
        super.remove_beat(beat_index, count)
        this.cursor_select_column(max(0, min(this.length - 1, beat_index)))
    }

    override fun remove_standard(beat_key: BeatKey, position: List<Int>) {
        super.remove_standard(beat_key, position)

        val tree = this.get_tree(beat_key, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select(beat_key, this.get_first_position(beat_key, new_position))
    }

    override fun controller_line_remove_standard(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        super.controller_line_remove_standard(type, beat_key, position)

        val tree = this.get_line_ctl_tree<EffectEvent>(type, beat_key, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select_ctl_at_line(type, beat_key, this.get_first_position_line_ctl(type, beat_key, new_position))
    }

    override fun controller_channel_remove_standard(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        super.controller_channel_remove_standard(type, channel, beat, position)

        val tree = this.get_channel_ctl_tree<EffectEvent>(type, channel, beat, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select_ctl_at_channel(type, channel, beat, this.get_first_position_channel_ctl(type, channel, beat, new_position))
    }

    override fun controller_global_remove_standard(type: EffectType, beat: Int, position: List<Int>) {
        super.controller_global_remove_standard(type, beat, position)

        val tree = this.get_global_ctl_tree<EffectEvent>(type, beat, position.subList(0, position.size - 1))
        val new_index = max(0, min(tree.size - 1, position.last()))
        val new_position = position.subList(0, position.size - 1) + listOf(new_index)
        this.cursor_select_ctl_at_global(type, beat, this.get_first_position_global_ctl(type, beat, new_position))
    }

    override fun mute_channel(channel: Int) {
        super.mute_channel(channel)
        this.cursor_select_channel(channel)
    }

    override fun unmute_channel(channel: Int) {
        super.unmute_channel(channel)
        this.cursor_select_channel(channel)
    }

    override fun mute_line(channel: Int, line_offset: Int) {
        super.mute_line(channel, line_offset)
        this.cursor_select_line(channel, line_offset)
    }

    override fun unmute_line(channel: Int, line_offset: Int) {
        super.unmute_line(channel, line_offset)
        this.cursor_select_line(channel, line_offset)
    }


    /* ------------------- 2nd Order Functions ---------------------------------- */
    override fun insert_beats(beat_index: Int, count: Int) {
        this.lock_cursor {
            super.insert_beats(beat_index, count)
        }
        this.cursor_select_column(beat_index + count - 1)
    }

    override fun remove_tagged_section(beat: Int) {
        super.remove_tagged_section(beat)
        this.cursor_select_column(beat)
    }

    override fun tag_section(beat: Int, title: String?) {
        super.tag_section(beat, title)
        this.cursor_select_column(beat)
    }

    fun offset_selection(amount: Int) {
        val (minimum, maximum) = this.get_min_and_max_in_selection() ?: return // Null means Single or Unset, can return

        val adj_amount = if (minimum + amount < 0) {
            0 - minimum
        } else if (maximum + amount > (8 * this.tuning_map.size) - 1) {
            ((8 * this.tuning_map.size) - 1) - maximum
        } else {
            amount
        }

        val cursor = this.cursor
        val (first, second) = when (cursor.mode) {
            CursorMode.Range -> {
                cursor.get_ordered_range()!!
            }
            CursorMode.Line -> {
                Pair(
                    BeatKey(cursor.channel, cursor.line_offset, 0),
                    BeatKey(cursor.channel, cursor.line_offset, this.length - 1)
                )
            }
            CursorMode.Column -> {
                Pair(
                    BeatKey(0, 0, cursor.beat),
                    BeatKey(this.channels.size, this.channels.last().size - 1, cursor.beat)
                )
            }
            CursorMode.Channel -> {
                Pair(
                    BeatKey(this.cursor.channel, 0, 0),
                    BeatKey(
                        this.cursor.channel,
                        this.get_channel(this.cursor.channel).lines.size - 1,
                        this.length - 1
                    )
                )
            }
            CursorMode.Single,
            CursorMode.Unset -> {
                return
            }
        }

        this.lock_cursor {
            this.offset_range(adj_amount, first, second)
        }
    }

    // Get the minimum and maximum values of the AbsoluteNoteEvents selected by the cursor
    // We don't need to consider RelativeNoteEvents or PercussionEvents
    fun get_min_and_max_in_selection(): Pair<Int, Int>? {
        val cursor = this.cursor
        val (first, second) = when (cursor.mode) {
            CursorMode.Range -> {
                cursor.get_ordered_range()!!
            }
            CursorMode.Line -> {
                Pair(
                    BeatKey(cursor.channel, cursor.line_offset, 0),
                    BeatKey(cursor.channel, cursor.line_offset, this.length - 1)
                )
            }
            CursorMode.Column -> {
                Pair(
                    BeatKey(0, 0, cursor.beat),
                    BeatKey(this.channels.size, this.channels.last().size - 1, cursor.beat)
                )
            }
            CursorMode.Channel -> {
                Pair(
                    BeatKey(this.cursor.channel, 0, 0),
                    BeatKey(
                        this.cursor.channel,
                        this.get_channel(this.cursor.channel).lines.size - 1,
                        this.length - 1
                    )
                )
            }
            CursorMode.Single,
            CursorMode.Unset -> {
                return null
            }
        }

        val radix = this.tuning_map.size
        var minimum = radix * 8
        var maximum = 0

        for (beat_key in this.get_beatkeys_in_range(first, second)) {
            this.get_tree(beat_key).traverse { _: ReducibleTree<out InstrumentEvent>, event: InstrumentEvent? ->
                if (event is AbsoluteNoteEvent) {
                    minimum = min(minimum, event.note)
                    maximum = max(maximum, event.note)
                }
            }
        }
        return Pair(minimum, maximum)
    }

    override fun _controller_global_copy_range(type: EffectType, target: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_global_copy_range(type, target, point_a, point_b, unset_original)
        val position = this.get_first_position_global_ctl(type, target)
        this.cursor_select_ctl_at_global(type, target, position)
    }

    override fun _controller_global_to_channel_copy_range(type: EffectType, target_channel: Int, target_beat: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_global_to_channel_copy_range(type, target_channel, target_beat, point_a, point_b, unset_original)
        this.cursor_select_ctl_at_channel(type, target_channel, target_beat, this.get_first_position_channel_ctl(type, target_channel, target_beat, listOf()))
    }

    override fun _controller_global_to_line_copy_range(type: EffectType, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        super._controller_global_to_line_copy_range(type, beat_a, beat_b, target_key, unset_original)
        this.cursor_select_ctl_at_line(type, target_key, this.get_first_position_line_ctl(type, target_key))
    }

    override fun _controller_channel_to_global_copy_range(type: EffectType, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_channel_to_global_copy_range(type, target_beat, original_channel, point_a, point_b, unset_original)
        this.cursor_select_ctl_at_global(type, target_beat, this.get_first_position_global_ctl(type, target_beat, listOf()))
    }

    override fun _controller_channel_copy_range(type: EffectType, target_channel: Int, target_beat: Int, original_channel: Int, point_a: Int, point_b: Int, unset_original: Boolean) {
        super._controller_channel_copy_range(type, target_channel, target_beat, original_channel, point_a, point_b, unset_original)
        val position = this.get_first_position_channel_ctl(type, target_channel, target_beat)
        this.cursor_select_ctl_at_channel(type, target_channel, target_beat, position)
    }

    override fun _controller_channel_to_line_copy_range(type: EffectType, channel_from: Int, beat_a: Int, beat_b: Int, target_key: BeatKey, unset_original: Boolean) {
        super._controller_channel_to_line_copy_range(type, channel_from, beat_a, beat_b, target_key, unset_original)
        this.cursor_select_ctl_at_line(type, target_key, this.get_first_position_line_ctl(type, target_key))
    }

    override fun _controller_line_to_global_copy_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_beat: Int, unset_original: Boolean) {
        super._controller_line_to_global_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_beat, unset_original)
        this.cursor_select_ctl_at_global(type, target_beat, this.get_first_position_global_ctl(type, target_beat, listOf()))
    }

    override fun _controller_line_to_channel_copy_range(type: EffectType, from_channel: Int, from_line_offset: Int, beat_a: Int, beat_b: Int, target_channel: Int, target_beat: Int, unset_original: Boolean) {
        super._controller_line_to_channel_copy_range(type, from_channel, from_line_offset, beat_a, beat_b, target_channel, target_beat, unset_original)
        this.cursor_select_ctl_at_channel(type, target_channel, target_beat, this.get_first_position_channel_ctl(type, target_channel, target_beat, listOf()))
    }

    override fun _controller_line_copy_range(type: EffectType, beat_key: BeatKey, first_corner: BeatKey, second_corner: BeatKey, unset_original: Boolean) {
        super._controller_line_copy_range(type, beat_key, first_corner, second_corner, unset_original)
        this.cursor_select_ctl_at_line(type, beat_key, this.get_first_position_line_ctl(type, beat_key))
    }

    override fun controller_line_unset_line(type: EffectType, channel: Int, line_offset: Int) {
        super.controller_line_unset_line(type, channel, line_offset)
        this.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    override fun controller_channel_unset_line(type: EffectType, channel: Int) {
        super.controller_channel_unset_line(type, channel)
        this.cursor_select_channel_ctl_line(type, channel)
    }

    override fun controller_global_unset_line(type: EffectType) {
        super.controller_global_unset_line(type)
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_global_unset_range(type: EffectType, first_beat: Int, second_beat: Int) {
        super.controller_global_unset_range(type, first_beat, second_beat)
        val new_position = this.get_first_position_global_ctl(type, first_beat)
        this.cursor_select_ctl_at_global(type, first_beat, new_position)
    }

    override fun controller_channel_unset_range(type: EffectType, channel: Int, first_beat: Int, second_beat: Int) {
        super.controller_channel_unset_range(type, channel, first_beat, second_beat)
        val new_position = this.get_first_position_channel_ctl(type, channel, first_beat)
        this.cursor_select_ctl_at_channel(type, channel, first_beat, new_position)
    }

    override fun controller_line_unset_range(type: EffectType, first_corner: BeatKey, second_corner: BeatKey) {
        super.controller_line_unset_range(type, first_corner, second_corner)
        val new_position = this.get_first_position_line_ctl(type, first_corner)
        this.cursor_select_ctl_at_line(type, first_corner, new_position)
    }

    override fun controller_global_overwrite_range_horizontally(type: EffectType, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_global_overwrite_range_horizontally(type, first_beat, second_beat, repeat)
        }
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_global_to_line_overwrite_range_horizontally(type: EffectType, target_channel: Int, target_line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_global_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, first_beat, second_beat, repeat)

        }
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_line_to_channel_overwrite_range_horizontally(type: EffectType, channel: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        this.lock_cursor {
            super.controller_line_to_channel_overwrite_range_horizontally(type, channel, first_key, second_key, repeat)
        }
        this.cursor_select_channel_ctl_line(type, channel)
    }

    override fun controller_global_to_channel_overwrite_range_horizontally(type: EffectType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_global_to_channel_overwrite_range_horizontally(type, channel, first_beat, second_beat, repeat)
        }
        this.cursor_select_channel_ctl_line(type, channel)
    }

    override fun controller_line_overwrite_range_horizontally(type: EffectType, channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        this.lock_cursor {
            super.controller_line_overwrite_range_horizontally(type, channel, line_offset, first_key, second_key, repeat)
        }
        this.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    override fun controller_line_to_global_overwrite_range_horizontally(type: EffectType, channel: Int, line_offset: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_line_to_global_overwrite_range_horizontally(type, channel, line_offset, first_beat, second_beat, repeat)
        }
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_channel_to_global_overwrite_range_horizontally(type: EffectType, channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_channel_to_global_overwrite_range_horizontally(type, channel, first_beat, second_beat, repeat)
        }
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_channel_overwrite_range_horizontally(type: EffectType, target_channel: Int, from_channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_channel_overwrite_range_horizontally(type, target_channel, from_channel, first_beat, second_beat, repeat)
        }
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_channel_to_line_overwrite_range_horizontally(type: EffectType, target_channel: Int, target_line_offset: Int, from_channel: Int, first_beat: Int, second_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_channel_to_line_overwrite_range_horizontally(type, target_channel, target_line_offset, from_channel, first_beat, second_beat, repeat)
        }
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_global_overwrite_line(type: EffectType, beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_global_overwrite_line(type, beat, repeat)
        }
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_channel_to_global_overwrite_line(type: EffectType, channel: Int, beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_channel_to_global_overwrite_line(type, channel, beat, repeat)
        }
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_line_to_global_overwrite_line(type: EffectType, beat_key: BeatKey, repeat: Int?) {
        this.lock_cursor {
            super.controller_line_to_global_overwrite_line(type, beat_key, repeat)
        }
        this.cursor_select_global_ctl_line(type)
    }

    override fun controller_global_to_line_overwrite_line(type: EffectType, from_beat: Int, target_channel: Int, target_line_offset: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_global_to_line_overwrite_line(type, from_beat, target_channel, target_line_offset, repeat)
        }
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_channel_to_line_overwrite_line(type: EffectType, target_channel: Int, target_line_offset: Int, original_channel: Int, original_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_channel_to_line_overwrite_line(type, target_channel, target_line_offset, original_channel, original_beat, repeat)
        }
        this.cursor_select_line_ctl_line(type, target_channel, target_line_offset)
    }

    override fun controller_channel_overwrite_line(type: EffectType, target_channel: Int, original_channel: Int, original_beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_channel_overwrite_line(type, target_channel, original_channel, original_beat, repeat)
        }
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_line_to_channel_overwrite_line(type: EffectType, target_channel: Int, original_key: BeatKey, repeat: Int?) {
        this.lock_cursor {
            super.controller_line_to_channel_overwrite_line(type, target_channel, original_key, repeat)
        }
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_global_to_channel_overwrite_line(type: EffectType, target_channel: Int, beat: Int, repeat: Int?) {
        this.lock_cursor {
            super.controller_global_to_channel_overwrite_line(type, target_channel, beat, repeat)
        }
        this.cursor_select_channel_ctl_line(type, target_channel)
    }

    override fun controller_line_overwrite_line(type: EffectType, channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int?) {
        this.lock_cursor {
            super.controller_line_overwrite_line(type, channel, line_offset, beat_key, repeat)
        }
        this.cursor_select_line_ctl_line(type, channel, line_offset)
    }

    override fun <T : EffectEvent> controller_global_set_initial_event(type: EffectType, event: T) {
        super.controller_global_set_initial_event(type, event)
        if (this.is_global_ctl_visible(type)) {
            this.cursor_select_global_ctl_line(type)
        } else {
            this.cursor_clear()
        }
    }

    override fun <T : EffectEvent> controller_channel_set_initial_event(type: EffectType, channel: Int, event: T) {
        super.controller_channel_set_initial_event(type, channel, event)
        if (this.is_channel_ctl_visible(type, channel)) {
            this.cursor_select_channel_ctl_line(type, channel)
        } else {
            this.cursor_select_channel(channel)
        }
    }

    override fun <T : EffectEvent> controller_line_set_initial_event(type: EffectType, channel: Int, line_offset: Int, event: T) {
        super.controller_line_set_initial_event(type, channel, line_offset, event)
        if (this.is_line_ctl_visible(type, channel, line_offset) && !this.is_line_selected(channel, line_offset)) {
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
        this.lock_cursor {
            super.move_beat_range(beat_key, first_corner, second_corner)
        }
        this.cursor_select(beat_key, this.get_first_position(beat_key))
    }

    override fun unset_line(channel: Int, line_offset: Int) {
        this.lock_cursor {
            super.unset_line(channel, line_offset)
        }
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

    override fun overwrite_beat_range_horizontally(channel: Int, line_offset: Int, first_key: BeatKey, second_key: BeatKey, repeat: Int?) {
        this.lock_cursor {
            super.overwrite_beat_range_horizontally(channel, line_offset, first_key, second_key, repeat)
        }
        this.cursor_select_line(channel, line_offset)
    }

    override fun overwrite_line(channel: Int, line_offset: Int, beat_key: BeatKey, repeat: Int?) {
        this.lock_cursor {
            super.overwrite_line(channel, line_offset, beat_key, repeat)
        }
        this.cursor_select_line(channel, line_offset)
    }

    override fun insert_beat(beat_index: Int) {
        super.insert_beat(beat_index)
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

    override fun set_global_controller_visibility(type: EffectType, visibility: Boolean) {
        super.set_global_controller_visibility(type, visibility)
        if (visibility) {
            this.cursor_select_global_ctl_line(type)
        } else {
            this.cursor_clear()
        }
    }

    override fun set_channel_controller_visibility(type: EffectType, channel_index: Int, visibility: Boolean) {
        super.set_channel_controller_visibility(type, channel_index, visibility)
        if (visibility) {
            this.cursor_select_channel_ctl_line(type, channel_index)
        } else {
            this.cursor_select_channel(channel_index)
        }
    }

    override fun remove_global_controller(type: EffectType) {
        super.remove_global_controller(type)
        this.cursor_clear()
    }

    override fun set_line_controller_visibility(type: EffectType, channel_index: Int, line_offset: Int, visibility: Boolean) {
        super.set_line_controller_visibility(type, channel_index, line_offset, visibility)
        if (visibility) {
            this.cursor_select_line_ctl_line(type, channel_index, line_offset)
        } else {
            this.cursor_select_line(channel_index, line_offset)
        }
    }

    override fun on_action_blocked(blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked(blocker_key, blocker_position)
        this.cursor_select(blocker_key, blocker_position)
    }

    override fun on_action_blocked_global_ctl(type: EffectType, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_global_ctl(type, blocker_beat, blocker_position)
        this.cursor_select_ctl_at_global(type, blocker_beat, blocker_position)
    }

    override fun on_action_blocked_channel_ctl(type: EffectType, blocker_channel: Int, blocker_beat: Int, blocker_position: List<Int>) {
        super.on_action_blocked_channel_ctl(type, blocker_channel, blocker_beat, blocker_position)
        this.cursor_select_ctl_at_channel(type, blocker_channel, blocker_beat, blocker_position)
    }

    override fun on_action_blocked_line_ctl(type: EffectType, blocker_key: BeatKey, blocker_position: List<Int>) {
        super.on_action_blocked_line_ctl(type, blocker_key, blocker_position)
        this.cursor_select_ctl_at_line(type, blocker_key, blocker_position)
    }

    // BASE FUNCTIONS ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


    // Cursor Functions ////////////////////////////////////////////////////////////////////////////
    open fun cursor_apply(cursor: OpusManagerCursor, force: Boolean = false) {
        if (!force && this._block_cursor_selection()) return
        this.cursor.clear()
        this.cursor = cursor
    }
    open fun cursor_clear() {
        if (this._block_cursor_selection()) return
        this.cursor.clear()
    }
    open fun cursor_select_channel(channel: Int) {
        if (this._block_cursor_selection()) return
        this.cursor.select_channel(channel)
    }
    open fun cursor_select_line(channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) return
        this.cursor.select_line(channel, line_offset)
    }
    open fun cursor_select_line_ctl_line(ctl_type: EffectType, channel: Int, line_offset: Int) {
        if (this._block_cursor_selection()) return
        this.cursor.select_line_ctl_line(channel, line_offset, ctl_type)
    }
    open fun cursor_select_channel_ctl_line(ctl_type: EffectType, channel: Int) {
        if (this._block_cursor_selection()) return
        this.cursor.select_channel_ctl_line(channel, ctl_type)
    }
    open fun cursor_select_global_ctl_line(ctl_type: EffectType) {
        if (this._block_cursor_selection()) return
        this.cursor.select_global_ctl_line(ctl_type)
    }
    open fun cursor_select_column(beat: Int) {
        if (this._block_cursor_selection() || beat >= this.length) return
        this.cursor.select_column(beat)
    }
    open fun cursor_select(beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) return
        this.cursor.select(beat_key, position)
    }
    open fun cursor_select_ctl_at_line(ctl_type: EffectType, beat_key: BeatKey, position: List<Int>) {
        if (this._block_cursor_selection()) return
        if (!this.is_line_ctl_visible(ctl_type, beat_key.channel, beat_key.line_offset)) return
        this.cursor.select_ctl_at_line(beat_key, position, ctl_type)
    }
    open fun cursor_select_ctl_at_channel(ctl_type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) return
        if (!this.is_channel_ctl_visible(ctl_type, channel)) return
        this.cursor.select_ctl_at_channel(channel, beat, position, ctl_type)
    }
    open fun cursor_select_ctl_at_global(ctl_type: EffectType, beat: Int, position: List<Int>) {
        if (this._block_cursor_selection()) return
        if (!this.is_global_ctl_visible(ctl_type)) return
        this.cursor.select_ctl_at_global(beat, position, ctl_type)
    }
    open fun cursor_select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) return
        this.cursor.select_range(beat_key_a, beat_key_b)
    }
    open fun cursor_select_global_ctl_range(type: EffectType, first: Int, second: Int) {
        if (this._block_cursor_selection()) return
        this.cursor.select_global_ctl_range(type, first, second)
    }
    open fun cursor_select_channel_ctl_range(type: EffectType, channel: Int, first: Int, second: Int) {
        if (this._block_cursor_selection()) return
        this.cursor.select_channel_ctl_range(type, channel, first, second)
    }
    open fun cursor_select_line_ctl_range(type: EffectType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        if (this._block_cursor_selection()) return
        this.cursor.select_line_ctl_range(type, beat_key_a, beat_key_b)
    }

    fun set_event_at_cursor(event: EffectEvent) {
        val cursor = this.cursor
        when (cursor.mode) {
            CursorMode.Line -> this.set_initial_event(event)
            CursorMode.Single -> {
                when (cursor.ctl_level) {
                    null -> throw InvalidCursorState()
                    CtlLineLevel.Global -> {
                        val (actual_beat, actual_position) = this.controller_global_get_actual_position(cursor.ctl_type!!, cursor.beat, cursor.get_position())
                        this.controller_global_set_event(
                            cursor.ctl_type!!,
                            actual_beat,
                            actual_position,
                            event
                        )
                    }
                    CtlLineLevel.Channel -> {
                        val (actual_beat, actual_position) = this.controller_channel_get_actual_position(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position())
                        this.controller_channel_set_event(
                            cursor.ctl_type!!,
                            cursor.channel,
                            actual_beat,
                            actual_position,
                            event
                        )
                    }
                    CtlLineLevel.Line -> {
                        val (actual_beat_key, actual_position) = this.controller_line_get_actual_position(cursor.ctl_type!!, cursor.get_beatkey(), cursor.get_position())
                        this.controller_line_set_event(
                            cursor.ctl_type!!,
                            actual_beat_key,
                            actual_position,
                            event
                        )
                    }
                }
            }
            else -> throw InvalidCursorState()
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
                    event
                )
            }
            else -> throw InvalidCursorState()
        }
    }
    fun set_percussion_event_at_cursor() {
        this.percussion_set_event(
            this.cursor.get_beatkey(),
            this.cursor.get_position()
        )
    }
    open fun remove_at_cursor(count: Int = 1) {
        val cursor = this.cursor
        when (cursor.ctl_level) {
            null -> {
                val beat_key = cursor.get_beatkey()
                val position = cursor.get_position()

                val (real_count, _) = this._calculate_new_position_after_remove(this.get_tree_copy(beat_key), position, count)
                this.remove_repeat(beat_key, position, real_count)
            }

            CtlLineLevel.Global -> {
                val working_tree = this.get_global_ctl_tree<EffectEvent>(cursor.ctl_type!!, cursor.beat).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)
                this.repeat_controller_global_remove(cursor.ctl_type!!, cursor.beat, cursor.get_position(), real_count)
            }

            CtlLineLevel.Channel -> {
                val working_tree = this.get_channel_ctl_tree<EffectEvent>(cursor.ctl_type!!, cursor.channel, cursor.beat).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)
                this.repeat_controller_channel_remove(cursor.ctl_type!!, cursor.channel, cursor.beat, cursor.get_position(), real_count)
            }

            CtlLineLevel.Line -> {
                val beat_key = cursor.get_beatkey()
                val working_tree = this.get_line_ctl_tree<EffectEvent>(cursor.ctl_type!!, beat_key).copy()
                val (real_count, cursor_position) = this._calculate_new_position_after_remove(working_tree, cursor.get_position(), count)
                this.repeat_controller_line_remove(cursor.ctl_type!!, beat_key, cursor.get_position(), real_count)
            }
        }
    }

    private fun _post_new_line(channel: Int, line_offset: Int) {
        when (this.cursor.mode) {
            CursorMode.Line,
            CursorMode.Single -> {
                if (this.cursor.channel == channel) {
                    if (this.cursor.line_offset >= line_offset) {
                        this.cursor.line_offset += 1
                    }
                }
            }
            CursorMode.Range -> {
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
            CursorMode.Column,
            CursorMode.Channel,
            CursorMode.Unset -> return
        }
        this.cursor_apply(this.cursor.copy())
    }

    fun unset() {
        when (this.cursor.mode) {
            CursorMode.Range -> {
                when (this.cursor.ctl_level) {
                    null -> {
                        val (first_key, second_key) = this.cursor.get_ordered_range()!!
                        this.unset_range(first_key, second_key)
                        this.cursor_select(first_key, listOf())
                    }
                    CtlLineLevel.Global -> {
                        val (key_a, key_b) = this.cursor.get_ordered_range()!!
                        val start = key_a.beat
                        val end = key_b.beat
                        this.controller_global_unset_range(this.cursor.ctl_type!!, start, end)
                        this.cursor_select_ctl_at_global(this.cursor.ctl_type!!, start, listOf())
                    }
                    CtlLineLevel.Channel -> {
                        val (key_a, key_b) = this.cursor.get_ordered_range()!!
                        val start = key_a.beat
                        val end = key_b.beat
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
            CursorMode.Single -> {
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
                        val real_position = this.controller_global_get_actual_position(this.cursor.ctl_type!!, beat, position)

                        this.controller_global_unset(this.cursor.ctl_type!!, real_position.first, real_position.second)
                    }
                    CtlLineLevel.Channel -> {
                        val channel = this.cursor.channel
                        val beat = this.cursor.beat
                        val position = this.cursor.get_position()
                        val real_position = this.controller_channel_get_actual_position(this.cursor.ctl_type!!, channel, beat, position)

                        this.controller_channel_unset(this.cursor.ctl_type!!, channel, real_position.first, real_position.second)
                    }
                    CtlLineLevel.Line -> {
                        val beat_key = this.cursor.get_beatkey()
                        val position = this.cursor.get_position()
                        val real_position = this.controller_line_get_actual_position(this.cursor.ctl_type!!, beat_key, position)

                        this.controller_line_unset(this.cursor.ctl_type!!, real_position.first, real_position.second)
                    }
                }

            }
            CursorMode.Column -> {
                this.unset_beat(this.cursor.beat)
            }
            CursorMode.Line -> {
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
            CursorMode.Channel -> {
                TODO("The Button hasn't been added to the ui (don't panic)")
            }
            CursorMode.Unset -> {}
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
            this.cursor.channel,
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

    fun <T> _calculate_new_position_after_remove(working_tree: ReducibleTree<T>, position: List<Int>, count: Int): Pair<Int, List<Int>> {
        val cursor_position = position.toMutableList()
        var real_count = 0
        for (i in 0 until count) {
            if (cursor_position.isEmpty()) {
                break
            }

            val parent = working_tree.get(*cursor_position.subList(0, cursor_position.size - 1).toIntArray())
            if (parent.size == 2) {
                parent.set_event(null)
                cursor_position.removeAt(cursor_position.size - 1)
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
            val (first, second) = this.cursor.get_ordered_range()!!
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
            val (first, second) = this.cursor.get_ordered_range()!!
            if (first != second) {
                this.move_beat_range(beat_key, first, second)
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.move_beat_range(beat_key, first, first)
            }
        } else {
            throw InvalidCursorState()
        }
    }

    fun copy_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.get_ordered_range()!!
            if (first != second) {
                this.overwrite_beat_range(beat_key, first, second)
            } else {
                if (this.is_percussion(first.channel) != this.is_percussion(beat_key.channel)) {
                    throw MixedInstrumentException(first, beat_key)
                }
                this.overwrite_beat_range(beat_key, first, second)
            }
        } else {
            throw InvalidCursorState()
        }
    }

    fun copy_line_ctl_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.get_ordered_range()!!
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
                null -> throw InvalidCursorState()
            }
        } else {
            throw InvalidCursorState()
        }
    }

    fun move_line_ctl_to_beat(beat_key: BeatKey) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.get_ordered_range()!!
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
                null -> throw InvalidCursorState()
            }
        } else {
            throw InvalidCursorState()
        }

    }

    fun copy_channel_ctl_to_beat(channel: Int, beat: Int) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.get_ordered_range()!!
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
                        this.controller_channel_overwrite_range(this.cursor.ctl_type!!, channel, beat, first.channel, first.beat, second.beat)
                    } else {
                        this.controller_channel_replace_tree(this.cursor.ctl_type!!, channel, beat, listOf(), this.get_channel_ctl_tree_copy(this.cursor.ctl_type!!, first.channel, first.beat, listOf()))
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
    }

    fun move_channel_ctl_to_beat(channel: Int, beat: Int) {
        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.get_ordered_range()!!
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
                null -> throw InvalidCursorState()
            }
        } else {
            throw InvalidCursorState()
        }
    }

    override fun controller_global_to_line_move_leaf(type: EffectType, beat: Int, position: List<Int>, target_key: BeatKey, target_position: List<Int>) {
        super.controller_global_to_line_move_leaf(type, beat, position, target_key, target_position)
        val cursor_position = this.get_first_position_line_ctl(type, target_key, target_position)
        this.cursor_select_ctl_at_line(type, target_key, cursor_position)
    }

    override fun controller_global_to_channel_move_leaf(type: EffectType, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_global_to_channel_move_leaf(type, beat_from, position_from, channel_to, beat_to, position_to)
        val cursor_position = this.get_first_position_channel_ctl(type, channel_to, beat_to, position_to)
        this.cursor_select_ctl_at_channel(type, channel_to, beat_to, cursor_position)
    }

    override fun controller_global_move_leaf(type: EffectType, beat_from: Int, position_from: List<Int>, beat_to: Int, position_to: List<Int>) {
        super.controller_global_move_leaf(type, beat_from, position_from, beat_to, position_to)
        val cursor_position = this.get_first_position_global_ctl(type, beat_to, position_to)
        this.cursor_select_ctl_at_global(type, beat_to, cursor_position)
    }

    override fun controller_channel_to_line_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, beat_key_to: BeatKey, position_to: List<Int>) {
        super.controller_channel_to_line_move_leaf(type, channel_from, beat_from, position_from, beat_key_to, position_to)
        val cursor_position = this.get_first_position_line_ctl(type, beat_key_to, position_to)
        this.cursor_select_ctl_at_line(type, beat_key_to, cursor_position)
    }

    override fun controller_channel_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_channel_move_leaf(type, channel_from, beat_from, position_from, channel_to, beat_to, position_to)
        val cursor_position = this.get_first_position_channel_ctl(type, channel_to, beat_to, position_to)
        this.cursor_select_ctl_at_channel(type, channel_to, beat_to, cursor_position)
    }

    override fun controller_channel_to_global_move_leaf(type: EffectType, channel_from: Int, beat_from: Int, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_channel_to_global_move_leaf(type, channel_from, beat_from, position_from, target_beat, target_position)
        val cursor_position = this.get_first_position_global_ctl(type, target_beat, target_position)
        this.cursor_select_ctl_at_global(type, target_beat, cursor_position)
    }

    override fun controller_line_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, beatkey_to: BeatKey, position_to: List<Int>) {
        super.controller_line_move_leaf(type, beatkey_from, position_from, beatkey_to, position_to)
        val cursor_position = this.get_first_position_line_ctl(type, beatkey_to, position_to)
        this.cursor_select_ctl_at_line(type, beatkey_to, cursor_position)
    }

    override fun controller_line_to_channel_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, channel_to: Int, beat_to: Int, position_to: List<Int>) {
        super.controller_line_to_channel_move_leaf(type, beatkey_from, position_from, channel_to, beat_to, position_to)
        val cursor_position = this.get_first_position_channel_ctl(type, channel_to, beat_to, position_to)
        this.cursor_select_ctl_at_channel(type, channel_to, beat_to, cursor_position)
    }

    override fun controller_line_to_global_move_leaf(type: EffectType, beatkey_from: BeatKey, position_from: List<Int>, target_beat: Int, target_position: List<Int>) {
        super.controller_line_to_global_move_leaf(type, beatkey_from, position_from, target_beat, target_position)
        val cursor_position = this.get_first_position_global_ctl(type, target_beat, target_position)
        this.cursor_select_ctl_at_global(type, target_beat, cursor_position)
    }

    fun copy_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) throw InvalidOverwriteCall()

        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.get_ordered_range()!!
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
    }

    fun move_global_ctl_to_beat(beat: Int) {
        if (this.cursor.ctl_level != CtlLineLevel.Global) throw InvalidOverwriteCall()

        if (this.cursor.is_selecting_range()) {
            val (first, second) = this.cursor.get_ordered_range()!!
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
                null -> throw InvalidCursorState()
            }
        } else {
            throw InvalidCursorState()
        }
    }

    fun move_to_previous_visible_line(repeat: Int = 1) {
        val cursor = this.cursor
        if (cursor.mode != CursorMode.Line) throw IncorrectCursorMode(this.cursor.mode, CursorMode.Line)

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
        if (cursor.mode != CursorMode.Line) throw IncorrectCursorMode(this.cursor.mode, CursorMode.Line)

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
                val working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceeding_leaf_position(
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
                val controller = this.channels[channel].get_controller<EffectEvent>(cursor.ctl_type!!)

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
                    val next_pair = this.get_global_ctl_proceeding_leaf_position(
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
                    val next_pair = this.get_proceeding_leaf_position(
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
                val working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()
                val controller = this.channels[working_beat_key.channel].lines[working_beat_key.line_offset].get_controller<EffectEvent>(cursor.ctl_type!!)

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
                val controller = this.channels[channel].get_controller<EffectEvent>(cursor.ctl_type!!)

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
                val controller = this.get_controller<EffectEvent>(cursor.ctl_type!!)
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
                val controller = this.channels[this.cursor.channel].lines[this.cursor.line_offset].get_controller<EffectEvent>(this.cursor.ctl_type!!)

                for (i in 0 until repeat) {
                    val next_pair = controller.get_preceding_leaf_position(
                        working_beat,
                        working_position
                    ) ?: break
                    working_beat = next_pair.first
                    working_position = next_pair.second
                }

                this.cursor_select_ctl_at_line(this.cursor.ctl_type!!,
                    BeatKey(working_beat, this.cursor.channel, this.cursor.line_offset), working_position)
            }
            CtlLineLevel.Channel -> {
                var working_beat = this.cursor.beat
                val channel = this.cursor.channel
                var working_position = this.cursor.get_position()
                val controller = this.channels[channel].get_controller<EffectEvent>(this.cursor.ctl_type!!)

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
                val controller = this.get_controller<EffectEvent>(this.cursor.ctl_type!!)

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
                val working_beat_key = cursor.get_beatkey()
                var working_position = cursor.get_position()

                for (i in 0 until repeat) {
                    val next_pair = this.get_line_ctl_proceeding_leaf_position(
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
                val controller = this.channels[channel].get_controller<EffectEvent>(cursor.ctl_type!!)

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
                    val next_pair = this.get_global_ctl_proceeding_leaf_position(
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
                    val next_pair = this.get_proceeding_leaf_position(
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
        if (this.cursor.ctl_level != null) return false

        return when (this.cursor.mode) {
            CursorMode.Single -> {
                val cbeat_key = this.cursor.get_beatkey()
                val cposition = this.cursor.get_position()
                cbeat_key == beat_key && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
            }
            CursorMode.Range -> this.cursor.range!!.second == beat_key
            CursorMode.Line,
            CursorMode.Column,
            CursorMode.Unset,
            CursorMode.Channel -> false
        }
    }

    fun is_channel_selected(channel: Int): Boolean {
        val cursor = this.cursor
        return (cursor.mode == CursorMode.Channel && cursor.ctl_level == null && cursor.channel == channel)
    }

    fun is_line_selected(channel: Int, line_offset: Int): Boolean {
        val cursor = this.cursor
        return (cursor.mode == CursorMode.Line && cursor.ctl_level == null && cursor.line_offset == line_offset && cursor.channel == channel)
    }

    fun is_line_selected_secondary(channel: Int, line_offset: Int): Boolean {
        val cursor = this.cursor
        return when (cursor.mode) {
            CursorMode.Column,
            CursorMode.Unset -> false
            CursorMode.Line -> {
                when (cursor.ctl_level) {
                    null,
                    CtlLineLevel.Global -> false
                    CtlLineLevel.Channel -> cursor.channel == channel
                    CtlLineLevel.Line -> {
                        cursor.channel == channel && line_offset == cursor.line_offset
                    }
                }
            }
            CursorMode.Single -> {
                cursor.channel == channel && cursor.line_offset == line_offset && cursor.ctl_level == null
            }
            CursorMode.Channel -> {
                cursor.channel == channel
            }
            CursorMode.Range -> {
                val (first, second) = cursor.get_ordered_range()!!
                when (cursor.ctl_level) {
                    null -> {
                        val abs_y_start = this.get_instrument_line_index(first.channel, first.line_offset)
                        val abs_y_end = this.get_instrument_line_index(second.channel, second.line_offset)
                        val this_y = this.get_instrument_line_index(channel, line_offset)
                        (abs_y_start .. abs_y_end).contains(this_y)
                    }
                    else -> false
                }
            }
        }
    }

    fun is_secondary_selection(beat_key: BeatKey, position: List<Int>): Boolean {
        if (this.cursor.ctl_level != null) {
            return false
        }

        return when (this.cursor.mode) {
            CursorMode.Single -> {
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
            CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                beat_key != this.cursor.range!!.second && beat_key in this.get_beatkeys_in_range(first, second)
            }

            CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }

            CursorMode.Line -> {
                this.cursor.line_offset == beat_key.line_offset && this.cursor.channel == beat_key.channel
            }

            CursorMode.Channel -> {
                beat_key.channel == this.cursor.channel
            }

            else -> {
                false
            }
        }

    }
    fun is_global_control_selected(control_type: EffectType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Single -> {
                if (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) {
                    val cposition = this.cursor.get_position()
                    beat == this.cursor.beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition
                } else {
                    false
                }
            }
            CursorMode.Range -> {
                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) && (beat == this.cursor.range!!.second.beat)
            }
            CursorMode.Unset,
            CursorMode.Column,
            CursorMode.Channel,
            CursorMode.Line -> {
                false
            }
        }
    }

    fun is_global_control_secondary_selected(control_type: EffectType, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Global || control_type != this.cursor.ctl_type) {
                    false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    var output = false
                    val controller = this.get_controller<EffectEvent>(control_type)
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
            CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type) && (beat != this.cursor.range!!.second.beat) && (first.beat .. second.beat).contains(beat)
            }
            CursorMode.Column -> {
                this.cursor.beat == beat
            }
            CursorMode.Line -> {
                (this.cursor.ctl_level == CtlLineLevel.Global && control_type == this.cursor.ctl_type)
            }
            else -> {
                false
            }
        }
    }
    fun is_channel_control_selected(control_type: EffectType, channel: Int, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
                        && beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            CursorMode.Range -> {
                (beat == this.cursor.range!!.second.beat) && (this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type) && this.cursor.range!!.first.channel == channel
            }
            CursorMode.Column,
            CursorMode.Line,
            CursorMode.Channel,
            CursorMode.Unset -> {
                false
            }
        }
    }

    fun is_channel_control_secondary_selected(control_type: EffectType, channel: Int, beat: Int, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Column -> {
                beat == this.cursor.beat
            }
            CursorMode.Line -> {
                this.cursor.channel == channel && this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type
            }
            CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Channel || this.cursor.ctl_type != control_type) {
                    false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }
                    if (channel != this.cursor.channel) {
                        return false
                    }

                    var output = false
                    val controller = this.get_all_channels()[channel].get_controller<EffectEvent>(control_type)
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
            CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                this.cursor.ctl_level == CtlLineLevel.Channel && this.cursor.ctl_type == control_type && (first.channel .. second.channel).contains(channel) && this.cursor.range!!.second.beat != beat && (first.beat .. second.beat).contains(beat)
            }

            CursorMode.Channel -> {
                this.cursor.ctl_level != CtlLineLevel.Global && this.cursor.channel == channel
            }
            CursorMode.Unset -> {
                false
            }
        }
    }

    fun is_line_control_selected(control_type: EffectType, beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Single -> {
                val cposition = this.cursor.get_position()
                this.cursor.channel == beat_key.channel
                        && this.cursor.line_offset == beat_key.line_offset
                        && control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
                        && beat_key.beat == this.cursor.beat
                        && position.size >= cposition.size
                        && position.subList(0, cposition.size) == cposition
            }
            CursorMode.Range -> {
                (beat_key == this.cursor.range!!.second) && control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Line
            }
            CursorMode.Channel,
            CursorMode.Unset,
            CursorMode.Column,
            CursorMode.Line -> {
                false
            }
        }
    }

    fun is_line_control_secondary_selected(control_type: EffectType, beat_key: BeatKey, position: List<Int>): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Column -> {
                this.cursor.beat == beat_key.beat
            }
            CursorMode.Line -> {
                this.cursor.channel == beat_key.channel
                        && this.cursor.line_offset == beat_key.line_offset
                        && control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Line
            }
            CursorMode.Single -> {
                if (this.cursor.ctl_level != CtlLineLevel.Line || control_type != this.cursor.ctl_type) {
                     false
                } else {
                    val cbeat = this.cursor.beat
                    val cposition = this.cursor.get_position()
                    val beat = beat_key.beat
                    if (cbeat == beat && position.size >= cposition.size && position.subList(0, cposition.size) == cposition) {
                        return false
                    }

                    if (this.cursor.channel != beat_key.channel || this.cursor.line_offset != beat_key.line_offset) {
                        return false
                    }

                    var output = false
                    val controller = this.get_all_channels()[beat_key.channel].lines[beat_key.line_offset].get_controller<EffectEvent>(control_type)
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
            CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                if (this.cursor.ctl_type == null) {
                    beat_key in this.get_beatkeys_in_range(first, second)
                } else {
                    control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Line && beat_key in this.get_beatkeys_in_range(first, second) && beat_key != this.cursor.range!!.second
                }
            }
            CursorMode.Unset -> {
                false
            }

            CursorMode.Channel -> {
                this.cursor.channel == beat_key.channel
            }
        }
    }

    fun is_line_control_line_selected(control_type: EffectType, channel: Int, line_offset: Int): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Line ->  {
                this.cursor.channel == channel && (this.cursor.ctl_level == CtlLineLevel.Line && control_type == this.cursor.ctl_type && this.cursor.line_offset == line_offset)
            }
            CursorMode.Range,
            CursorMode.Column,
            CursorMode.Unset,
            CursorMode.Single,
            CursorMode.Channel -> false

        }
    }

    fun is_line_control_line_selected_secondary(control_type: EffectType, channel: Int, line_offset: Int): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Column,
            CursorMode.Unset -> false
            CursorMode.Line -> {
                when (this.cursor.ctl_level) {
                    CtlLineLevel.Line,
                    CtlLineLevel.Channel,
                    CtlLineLevel.Global -> false
                    null -> {
                        channel == this.cursor.channel && this.cursor.line_offset == line_offset
                    }
                }
            }
            CursorMode.Channel -> {
                this.cursor.channel == channel
            }
            CursorMode.Single -> {
                this.cursor.ctl_level == CtlLineLevel.Line && control_type == this.cursor.ctl_type && channel == this.cursor.channel && this.cursor.line_offset == line_offset
            }
            CursorMode.Range -> {
                val (first_key, second_key) = this.cursor.get_ordered_range()!!
                val target = this.get_instrument_line_index(channel, line_offset)
                val first = this.get_instrument_line_index(first_key.channel, first_key.line_offset)
                val second = this.get_instrument_line_index(second_key.channel, second_key.line_offset)
                if (this.cursor.ctl_type == null) {
                    (first .. second).contains(target)
                } else if (control_type == this.cursor.ctl_type) {
                    this.cursor.ctl_level == CtlLineLevel.Line && (first .. second).contains(target)
                } else {
                    false
                }
            }
        }
    }

    fun is_channel_control_line_selected(control_type: EffectType, channel: Int): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Line -> {
                control_type == this.cursor.ctl_type
                        && this.cursor.ctl_level == CtlLineLevel.Channel
                        && this.cursor.channel == channel
            }
            CursorMode.Range,
            CursorMode.Unset,
            CursorMode.Column,
            CursorMode.Channel,
            CursorMode.Single -> false
        }
    }

    fun is_channel_control_line_selected_secondary(control_type: EffectType, channel: Int): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Line,
            CursorMode.Column,
            CursorMode.Unset -> false
            CursorMode.Channel -> {
                this.cursor.channel == channel
            }
            CursorMode.Single -> {
                this.cursor.channel == channel && control_type == this.cursor.ctl_type && this.cursor.ctl_level == CtlLineLevel.Channel
            }
            CursorMode.Range -> {
                val (first, _) = this.cursor.get_ordered_range()!!
                control_type == this.cursor.ctl_type && first.channel == channel && this.cursor.ctl_level == CtlLineLevel.Channel
            }
        }
    }

    fun is_global_control_line_selected(control_type: EffectType): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Line -> {
                this.cursor.ctl_level == CtlLineLevel.Global && this.cursor.ctl_type == control_type
            }
            CursorMode.Single,
            CursorMode.Range,
            CursorMode.Channel,
            CursorMode.Unset,
            CursorMode.Column -> false
        }
    }

    fun is_global_control_line_selected_secondary(control_type: EffectType): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Line,
            CursorMode.Column,
            CursorMode.Channel,
            CursorMode.Unset -> false
            CursorMode.Single -> {
                this.cursor.ctl_level == CtlLineLevel.Global && this.cursor.ctl_type == control_type
            }
            CursorMode.Range -> {
                this.cursor.ctl_level == CtlLineLevel.Global && this.cursor.ctl_type == control_type
            }
        }
    }

    fun is_beat_selected(beat: Int): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Column -> this.cursor.beat == beat
            else -> false
        }
    }
    fun is_beat_selected_secondary(beat: Int): Boolean {
        return when (this.cursor.mode) {
            CursorMode.Single -> this.cursor.beat == beat
            CursorMode.Range -> {
                val (first, second) = this.cursor.get_ordered_range()!!
                (min(first.beat, second.beat) .. max(first.beat, second.beat)).contains(beat)
            }
            else -> false
        }
    }
    fun select_first_in_beat(beat: Int) {
        when (this.cursor.ctl_level) {
            null -> {
                val new_beat_key = BeatKey(this.cursor.channel, this.cursor.line_offset, beat)
                val new_position = this.get_first_position(new_beat_key, listOf())
                this.cursor_select(
                    new_beat_key,
                    new_position
                )
            }
            CtlLineLevel.Line -> {
                val new_beat_key = BeatKey(this.cursor.channel, this.cursor.line_offset, beat)
                val new_position = this.get_first_position_line_ctl(this.cursor.ctl_type!!, new_beat_key, listOf())
                this.cursor_select_ctl_at_line(
                    this.cursor.ctl_type!!,
                    new_beat_key,
                    new_position
                )
            }
            CtlLineLevel.Channel -> {
                val new_position = this.get_first_position_channel_ctl(this.cursor.ctl_type!!, this.cursor.channel, beat, listOf())
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

    fun <T: EffectEvent> set_initial_event(event: T) {
        when (this.cursor.ctl_level) {
            null,
            CtlLineLevel.Line -> this.controller_line_set_initial_event(this.cursor.ctl_type ?: EffectType.Volume, this.cursor.channel, this.cursor.line_offset, event)
            CtlLineLevel.Channel -> this.controller_channel_set_initial_event(this.cursor.ctl_type!!, this.cursor.channel, event)
            CtlLineLevel.Global -> this.controller_global_set_initial_event(this.cursor.ctl_type!!, event)
        }
    }

    fun get_active_active_control_set(): EffectControlSet? {
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
        return when (this.cursor.mode) {
            CursorMode.Channel,
            CursorMode.Line,
            CursorMode.Single -> {
                val start_channel = when (this.cursor.ctl_level) {
                    CtlLineLevel.Global -> 0
                    null,
                    CtlLineLevel.Line,
                    CtlLineLevel.Channel -> this.cursor.channel
                }

                kotlin.math.max(0, kotlin.math.min(start_channel + n, this.channels.size - 1))
            }

            CursorMode.Column -> {
                kotlin.math.max(0, kotlin.math.min(n - 1, this.channels.size - 1))
            }

            CursorMode.Range,
            CursorMode.Unset -> null
        }
    }

    internal fun _block_cursor_selection(): Boolean {
        return (this._blocked_action_catcher > 0 || this._cursor_lock > 0)
    }

    override fun move_line(channel_index_from: Int, line_offset_from: Int, channel_index_to: Int, line_offset_to: Int) {
        val (adj_channel, adj_line_offset) = if (channel_index_from == channel_index_to && line_offset_from < line_offset_to) {
            Pair(channel_index_to, line_offset_to - 1)
        } else if (channel_index_from < channel_index_to && this.channels[channel_index_from].size == 1) {
            Pair(channel_index_to - 1, line_offset_to)
        } else {
            Pair(channel_index_to, line_offset_to)
        }

        this.lock_cursor {
            super.move_line(channel_index_from, line_offset_from, channel_index_to, line_offset_to)
        }

        this.cursor_select_line(adj_channel, adj_line_offset)
    }

    fun get_tree(): ReducibleTree<out OpusEvent>? {
        if (this.cursor.mode != CursorMode.Single) return null
        return when (this.cursor.ctl_level) {
            null -> this.get_tree(this.cursor.get_beatkey(), this.cursor.get_position())
            CtlLineLevel.Line -> this.get_line_ctl_tree(this.cursor.ctl_type!!, this.cursor.get_beatkey(), this.cursor.get_position())
            CtlLineLevel.Channel -> this.get_channel_ctl_tree(this.cursor.ctl_type!!, this.cursor.channel, this.cursor.beat, this.cursor.get_position())
            CtlLineLevel.Global -> this.get_global_ctl_tree_copy(this.cursor.ctl_type!!, this.cursor.beat, this.cursor.get_position())
        }
    }

    fun get_event_at_cursor(): OpusEvent? {
        if (this.cursor.mode != CursorMode.Single) return null
        return when (this.cursor.ctl_level) {
            null -> {
                val (actual_beat_key, actual_position) = this.get_actual_position(this.cursor.get_beatkey(), this.cursor.get_position())
                this.get_tree(actual_beat_key, actual_position).event
            }
            CtlLineLevel.Line -> {
                val (actual_beat_key, actual_position) = this.controller_line_get_actual_position(
                    this.cursor.ctl_type!!,
                    this.cursor.get_beatkey(),
                    this.cursor.get_position()
                )
                this.get_line_ctl_tree<EffectEvent>(this.cursor.ctl_type!!, actual_beat_key, actual_position).event
            }
            CtlLineLevel.Channel -> {
                val (actual_beat, actual_position) = this.controller_channel_get_actual_position(
                    this.cursor.ctl_type!!,
                    this.cursor.channel,
                    this.cursor.beat,
                    this.cursor.get_position()
                )
                this.get_channel_ctl_tree<EffectEvent>(this.cursor.ctl_type!!, this.cursor.channel, actual_beat, actual_position).event
            }
            CtlLineLevel.Global -> {
                val (actual_beat, actual_position) = this.controller_global_get_actual_position(
                    this.cursor.ctl_type!!,
                    this.cursor.beat,
                    this.cursor.get_position()
                )
                this.get_global_ctl_tree_copy<EffectEvent>(this.cursor.ctl_type!!, actual_beat, actual_position).event
            }
        }
    }

    open fun _set_note_octave(beat_key: BeatKey, position: List<Int>, octave: Int, mode: RelativeInputMode, fallback_offset: Int = 0) {
        val current_tree_position = this.get_actual_position(beat_key, position)
        val current_tree = this.get_tree(current_tree_position.first, current_tree_position.second)
        val current_event = current_tree.get_event()
        val duration = current_event?.duration ?: 1
        val radix = this.tuning_map.size

        val new_event = when (mode) {
            RelativeInputMode.Absolute -> {
                AbsoluteNoteEvent(
                    when (current_event) {
                        is AbsoluteNoteEvent -> {
                            val offset = current_event.note % radix
                            (octave * radix) + offset
                        }
                        else -> {
                            val cursor = this.cursor
                            val absolute_value = this.get_preceding_leaf_position(cursor.get_beatkey(), cursor.get_position())?.let {
                                this.get_absolute_value(it.first, it.second)
                            }

                            val offset = if (absolute_value != null) {
                                absolute_value % radix
                            } else {
                                fallback_offset
                            }
                            (octave * radix) + offset
                        }
                    },
                    duration
                )
            }
            RelativeInputMode.Positive -> {
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> (octave * radix) + (abs(current_event.offset) % radix)
                        else -> octave * radix
                    },
                    duration
                )
            }
            RelativeInputMode.Negative -> {
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> {
                            0 - ((octave * radix) + (abs(current_event.offset) % radix))
                        }
                        else -> 0 - (octave * radix)
                    },
                    duration
                )
            }
        }

        this.set_event(beat_key, position, new_event)
    }

    open fun _set_note_offset(beat_key: BeatKey, position: List<Int>, offset: Int, mode: RelativeInputMode, fallback_octave: Int = 0) {
        val current_tree = this.get_tree(beat_key, position)
        val current_event = current_tree.get_event()
        val duration = current_event?.duration ?: 1
        val radix = this.tuning_map.size

        val new_event = when (mode) {
            RelativeInputMode.Absolute -> {
                AbsoluteNoteEvent(
                    when (current_event) {
                        is AbsoluteNoteEvent -> {
                            val octave = (current_event.note / radix)
                            (octave * radix) + offset
                        }
                        else -> {
                            val cursor = this.cursor
                            val absolute_value = this.get_preceding_leaf_position(cursor.get_beatkey(), cursor.get_position())?.let {
                                this.get_absolute_value(it.first, it.second)
                            }

                            val octave = if (absolute_value != null) {
                                absolute_value / radix
                            } else {
                                fallback_octave
                            }

                            offset + (octave * radix)
                        }
                    },
                    duration
                )
            }
            RelativeInputMode.Positive -> {
                println("P: $offset - - - -- - - - - ")
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> ((abs(current_event.offset) / radix) * radix) + offset
                        else -> offset
                    },
                    duration
                )
            }
            RelativeInputMode.Negative -> {
                println("N: $offset - - - -- - - - - ")
                RelativeNoteEvent(
                    when (current_event) {
                        is RelativeNoteEvent -> {
                            -1 * (((abs(current_event.offset) / radix) * radix) + offset)
                        }
                        else -> -1 * offset
                    },
                    duration
                )
            }
        }

        this.set_event(beat_key, position, new_event)
    }

    override fun set_channel_event_color(channel: Int, color: Color?) {
        super.set_channel_event_color(channel, color)
        this.cursor_select_channel(channel)
    }

    override fun set_channel_event_bg_color(channel: Int, color: Color?) {
        super.set_channel_event_bg_color(channel, color)
        this.cursor_select_channel(channel)
    }

    override fun set_channel_effect_color(channel: Int, color: Color?) {
        super.set_channel_effect_color(channel, color)
        this.cursor_select_channel(channel)
    }

    override fun set_channel_effect_bg_color(channel: Int, color: Color?) {
        super.set_channel_effect_bg_color(channel, color)
        this.cursor_select_channel(channel)
    }

    override fun set_line_event_color(channel: Int, line_offset: Int, color: Color?) {
        super.set_line_event_color(channel, line_offset, color)
        this.cursor_select_line(channel, line_offset)
    }

    override fun set_line_event_bg_color(channel: Int, line_offset: Int, color: Color?) {
        super.set_line_event_bg_color(channel, line_offset, color)
        this.cursor_select_line(channel, line_offset)
    }

    override fun set_line_effect_color(channel: Int, line_offset: Int, color: Color?) {
        super.set_line_effect_color(channel, line_offset, color)
        this.cursor_select_line(channel, line_offset)
    }

    override fun set_line_effect_bg_color(channel: Int, line_offset: Int, color: Color?) {
        super.set_line_effect_bg_color(channel, line_offset, color)
        this.cursor_select_line(channel, line_offset)
    }
}
