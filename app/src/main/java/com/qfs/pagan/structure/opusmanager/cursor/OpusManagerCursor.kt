package com.qfs.pagan.structure.opusmanager.cursor

import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase

data class OpusManagerCursor(
    var mode: CursorMode = CursorMode.Unset,
    var channel: Int = 0,
    var line_offset: Int = 0,
    var beat: Int = 0,
    var position: List<Int> = listOf(),
    var range: Pair<BeatKey, BeatKey>? = null,
    var ctl_level: CtlLineLevel? = null,
    var ctl_type: EffectType? = null
) {

    override fun equals(other: Any?): Boolean {
        if (other !is OpusManagerCursor) {
            return false
        }

        if (other.mode != this.mode) {
            return false
        }

        return when (this.mode) {
            CursorMode.Line -> {
                if (this.ctl_level != other.ctl_level || this.ctl_type != other.ctl_type) {
                    false
                } else {
                    when (this.ctl_level) {
                        null,
                        CtlLineLevel.Line -> {
                            other.channel == this.channel && other.line_offset == this.line_offset
                        }
                        CtlLineLevel.Channel -> other.channel == this.channel
                        CtlLineLevel.Global -> true
                    }
                }
            }

            CursorMode.Channel -> {
                other.channel == this.channel && this.ctl_level == other.ctl_level && this.ctl_type == other.ctl_type
            }

            CursorMode.Column -> {
                other.beat == this.beat
            }

            CursorMode.Unset -> {
                true
            }

            CursorMode.Single -> {
                if (this.ctl_level != other.ctl_level || this.ctl_type != other.ctl_type) {
                    false
                } else {
                    when (this.ctl_level) {
                        null,
                        CtlLineLevel.Line -> {
                            this.get_beatkey() == other.get_beatkey() && this.get_position() == other.get_position()
                        }
                        CtlLineLevel.Channel -> {
                            this.channel == other.channel && other.beat == this.beat
                        }
                        CtlLineLevel.Global -> {
                            other.beat == this.beat
                        }
                    }
                }
            }

            CursorMode.Range -> {
                this.range == other.range && other.ctl_level == this.ctl_level && other.ctl_type == this.ctl_type
            }
        }
    }

    fun is_selecting_range(): Boolean {
        return this.mode == CursorMode.Range
    }

    fun clear() {
        this.mode = CursorMode.Unset
        this.channel = 0
        this.line_offset = 0
        this.beat = 0
        this.ctl_level = null
        this.ctl_type = null
        this.position = listOf()
        this.range = null
    }

    fun get_beatkey(): BeatKey {
        if (this.mode != CursorMode.Single) {
            throw InvalidModeException(this.mode, CursorMode.Single)
        }

        return BeatKey(
            this.channel,
            this.line_offset,
            this.beat
        )
    }

    fun get_position(): List<Int> {
        if (this.mode != CursorMode.Single) {
            throw InvalidModeException(this.mode, CursorMode.Single)
        }

        return this.position.toList()
    }

    fun select(beat_key: BeatKey, position: List<Int>) {
        this.mode = CursorMode.Single
        this.channel = beat_key.channel
        this.line_offset = beat_key.line_offset
        this.beat = beat_key.beat
        this.position = position.toList()
        this.ctl_type = null
        this.ctl_level = null
    }

    fun select_line(channel: Int, line_offset: Int) {
        this.mode = CursorMode.Line
        this.channel = channel
        this.line_offset = line_offset
        this.ctl_type = null
        this.ctl_level = null
    }

    fun select_channel(channel: Int) {
        this.mode = CursorMode.Channel
        this.channel = channel
        this.line_offset = 0
        this.ctl_type = null
        this.ctl_level = null
    }

    fun select_column(beat: Int) {
        this.mode = CursorMode.Column
        this.beat = beat
        this.ctl_type = null
        this.ctl_level = null
    }

    fun select_ctl_at_line(beat_key: BeatKey, position: List<Int>, type: EffectType) {
        this.mode = CursorMode.Single
        this.channel = beat_key.channel
        this.line_offset = beat_key.line_offset
        this.beat = beat_key.beat
        this.position = position.toList()
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Line
    }

    fun select_ctl_at_channel(channel: Int, beat: Int, position: List<Int>, type: EffectType) {
        this.mode = CursorMode.Single
        this.channel = channel
        this.line_offset = 0
        this.beat = beat
        this.position = position.toList()
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Channel
    }

    fun select_ctl_at_global(beat: Int, position: List<Int>, type: EffectType) {
        this.mode = CursorMode.Single
        this.channel = 0
        this.line_offset = 0
        this.beat = beat
        this.position = position.toList()
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Global
    }

    fun select_line_ctl_line(channel: Int, line_offset: Int, type: EffectType) {
        this.mode = CursorMode.Line
        this.channel = channel
        this.line_offset = line_offset
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Line
    }

    fun select_channel_ctl_line(channel: Int, type: EffectType) {
        this.mode = CursorMode.Line
        this.channel = channel
        this.line_offset = 0
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Channel
    }

    fun select_global_ctl_line(type: EffectType) {
        this.mode = CursorMode.Line
        this.channel = 0
        this.line_offset = 0
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Global
    }

    fun select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.mode = CursorMode.Range

        this.range = Pair(beat_key_a, beat_key_b)

        this.ctl_type = null
        this.ctl_level = null
    }

    fun get_ordered_range(): Pair<BeatKey, BeatKey>? {
        if (this.mode != CursorMode.Range) {
            return null
        }

        return OpusLayerBase.get_ordered_beat_key_pair(this.range!!.first, this.range!!.second)
    }

    fun select_first_corner(beat_key: BeatKey) {
        this.select_range(beat_key, beat_key)
        this.ctl_type = null
        this.ctl_level = null
    }

    fun select_line_ctl_first_corner(type: EffectType, beat_key: BeatKey) {
        this.select_range(beat_key, beat_key)
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Line
    }

    fun select_line_ctl_range(type: EffectType, beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.select_range(beat_key_a, beat_key_b)

        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Line
    }


    fun select_global_ctl_range(type: EffectType, first_beat: Int, second_beat: Int) {
        this.range = Pair(
            BeatKey(0, 0, first_beat),
            BeatKey(0, 0, second_beat)
        )
        this.mode = CursorMode.Range
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Global
    }

    fun select_global_ctl_end_point(type: EffectType, beat: Int) {
        this.select_global_ctl_range(type, beat, beat)
    }

    fun select_channel_ctl_range(type: EffectType, channel: Int, first_beat: Int, second_beat: Int) {
        this.range = Pair(
            BeatKey(channel, 0, first_beat),
            BeatKey(channel, 0, second_beat)
        )
        this.mode = CursorMode.Range
        this.ctl_type = type
        this.ctl_level = CtlLineLevel.Channel
    }

    fun select_channel_ctl_end_point(type: EffectType, channel: Int, beat: Int) {
        this.select_channel_ctl_range(type,channel, beat, beat)
    }

    override fun hashCode(): Int {
        var result = this.mode.hashCode()
        result = ((result shl 5) + (result shr 27)).xor(this.channel)
        result = ((result shl 5) + (result shr 27)).xor(this.line_offset)
        result = ((result shl 5) + (result shr 27)).xor(this.beat)
        result = ((result shl 5) + (result shr 27)).xor(this.position.hashCode())
        result = ((result shl 5) + (result shr 27)).xor((this.range?.hashCode() ?: 0))
        result = ((result shl 5) + (result shr 27)).xor((this.ctl_level?.hashCode() ?: 0))
        result = ((result shl 5) + (result shr 27)).xor((this.ctl_type?.hashCode() ?: 0))
        return result
    }

    //fun move_left() {
    //    var working_tree = this.opus_manager.get_beat_tree(this.get_beatkey())
    //    for (i in this.position) {
    //        working_tree = working_tree.get(i)
    //    }

    //    while (this.position.isNotEmpty()) {
    //        if (this.position.last() == 0) {
    //            if (working_tree.parent != null) {
    //                working_tree = working_tree.parent!!
    //            } else {
    //                break
    //            }
    //            this.position.removeAt(this.position.size - 1)
    //        } else {
    //            this.position[this.position.size - 1] -= 1
    //            break
    //        }
    //    }

    //    if (this.beat > 0 && this.position.isEmpty()) {
    //        this.beat -= 1
    //        this.settle(true)
    //    } else {
    //        this.settle()
    //    }
    //}

    //fun move_right() {
    //    var working_tree = this.opus_manager.get_beat_tree(this.get_beatkey())
    //    for (i in this.position) {
    //        working_tree = working_tree.get(i)
    //    }

    //    while (this.position.isNotEmpty()) {
    //        if (working_tree.parent!!.size - 1 == this.position.last()) {
    //            this.position.removeAt(this.position.size - 1)
    //            working_tree = working_tree.parent!!
    //        } else if (working_tree.parent!!.size - 1 > this.position.last()) {
    //            this.position[this.position.size - 1] += 1
    //            break
    //        }
    //    }

    //    if (this.x < this.opus_manager.opus_beat_count - 1 && this.position.isEmpty()) {
    //        this.x += 1
    //        this.settle()
    //    } else {
    //        this.settle(true)
    //    }
    //}

    //fun settle(right_align: Boolean = false) {
    //    if (this.opus_manager.opus_beat_count == 0) {
    //        // NOTE: This'll problably bite me in the ass...
    //        return
    //    }

    //    this.y = max(0, min(this.y, this.opus_manager.line_count() - 1))
    //    this.x = max(0, min(this.x, this.opus_manager.opus_beat_count - 1))
    //    // First, get the beat
    //    val working_beat = this.opus_manager.get_beat_tree(this.get_beatkey())
    //    var working_tree = working_beat

    //    val working_position = mutableListOf<Int>()

    //    // Then get the current_working_tree
    //    var index = 0
    //    for (j in this.position) {
    //        if (working_tree.is_leaf()) {
    //            break
    //        }
    //        working_tree = if (working_tree.size <= j) {
    //            working_tree.get(working_tree.size - 1)
    //        } else {
    //            working_tree.get(j)
    //        }
    //        working_position.add(j)
    //    }

    //    // Then find the leaf if not already found
    //    while (! working_tree.is_leaf()) {
    //        working_tree = if (right_align) {
    //            working_position.add(working_tree.size - 1)
    //            working_tree.get(working_tree.size - 1)
    //        } else {
    //            working_position.add(0)
    //            working_tree.get(0)
    //        }
    //    }
    //    this.position = working_position
    //}
}
