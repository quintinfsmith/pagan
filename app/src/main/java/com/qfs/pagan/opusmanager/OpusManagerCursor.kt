package com.qfs.pagan.opusmanager

data class OpusManagerCursor(
    var mode: CursorMode = CursorMode.Unset,
    var channel: Int = 0,
    var line_offset: Int = 0,
    var beat: Int = 0,
    var position: List<Int> = listOf(),
    var range: Pair<BeatKey, BeatKey>? = null
) {
    enum class CursorMode {
        Row,
        Column,
        Single,
        Range,
        Unset
    }
    class InvalidModeException(actual: CursorMode, expected: CursorMode): Exception("Incorrect Cursor Mode. expected $expected but got $actual")

    var is_linking = false
    override fun equals(other: Any?): Boolean {
        if (other !is OpusManagerCursor) {
            return false
        }

        if (other.mode != this.mode) {
            return false
        }
        return when (this.mode) {
            CursorMode.Row -> {
                other.channel == this.channel && other.line_offset == this.line_offset
            }
            CursorMode.Column -> {
                other.beat == this.beat
            }
            CursorMode.Unset -> {
                true
            }
            CursorMode.Single -> {
                this.get_beatkey() == other.get_beatkey()
            }
            CursorMode.Range -> {
                this.range == other.range
            }
        }
    }
    fun is_linking_range(): Boolean {
        return this.mode == CursorMode.Range && this.is_linking
    }

    fun clear() {
        this.mode = CursorMode.Unset
        this.channel = 0
        this.line_offset = 0
        this.beat = 0
        this.position = listOf()
        this.range = null
        this.is_linking = false
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
        this.position = position
        this.is_linking = false
    }

    fun select_row(channel: Int, line_offset: Int) {
        this.mode = CursorMode.Row
        this.channel = channel
        this.line_offset = line_offset
        this.is_linking = false
    }

    fun select_column(beat: Int) {
        this.mode = CursorMode.Column
        this.beat = beat
        this.is_linking = false
    }

    fun select_range(beat_key_a: BeatKey, beat_key_b: BeatKey) {
        this.mode = CursorMode.Range

        val (from_key, to_key) = if (beat_key_a.channel < beat_key_b.channel) {
            Pair(
                BeatKey(beat_key_a.channel, beat_key_a.line_offset, -1),
                BeatKey(beat_key_b.channel, beat_key_b.line_offset, -1)
            )
        } else if (beat_key_a.channel == beat_key_b.channel) {
            if (beat_key_a.line_offset < beat_key_b.line_offset) {
                Pair(
                    BeatKey(beat_key_a.channel, beat_key_a.line_offset, -1),
                    BeatKey(beat_key_b.channel, beat_key_b.line_offset, -1)
                )
            } else {
                Pair(
                    BeatKey(beat_key_b.channel, beat_key_b.line_offset, -1),
                    BeatKey(beat_key_a.channel, beat_key_a.line_offset, -1)
                )
            }
        } else {
            Pair(
                BeatKey(beat_key_b.channel, beat_key_b.line_offset, -1),
                BeatKey(beat_key_a.channel, beat_key_a.line_offset, -1)
            )
        }

        from_key.beat = Integer.min(beat_key_a.beat, beat_key_b.beat)
        to_key.beat = Integer.max(beat_key_a.beat, beat_key_b.beat)
        this.range = Pair(from_key, to_key)
    }

    fun select_to_link(beat_key: BeatKey) {
        this.select(beat_key, listOf())
        this.is_linking = true
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
    //        // TODO: This'll problably bite me in the ass...
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
