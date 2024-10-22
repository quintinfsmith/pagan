package com.qfs.pagan
import android.view.KeyEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusManagerCursor
import kotlin.math.max
import kotlin.math.min
import com.qfs.pagan.OpusLayerInterface as OpusManager

class KeyboardInputInterface(var opus_manager: OpusManager) {
    var input_buffer_value: Int? = null

    fun clear_value_buffer(default: Int = 0, minimum: Int? = null, maximum: Int? = null): Int {
        var output = this.input_buffer_value ?: default
        this.input_buffer_value = null
        if (minimum != null) {
            output = max(minimum, output)
        }
        if (maximum != null) {
            output = min(maximum, output)
        }

        return output
    }

    fun input_key_a(): Boolean {
        return when (this.opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Line -> {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                this.opus_manager.insert_line(repeat)
                true
            }
            OpusManagerCursor.CursorMode.Column -> {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                this.opus_manager.insert_beat_after_cursor(repeat)
                true
            }
            OpusManagerCursor.CursorMode.Single -> {
                var repeat = this.clear_value_buffer(1, maximum=64)
                if (repeat > 0) {
                    val tree = this.opus_manager.get_tree()
                    if (tree.parent == null) {
                        this.opus_manager.split_tree(repeat + 1)
                    } else {
                        this.opus_manager.insert_after(repeat)
                    }
                }
                true
            }
            OpusManagerCursor.CursorMode.Range,
            OpusManagerCursor.CursorMode.Unset -> false
        }
    }

    fun input_key_b(): Boolean {
        val beat = this.clear_value_buffer(0, 0, this.opus_manager.beat_count - 1)
        return when (this.opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Line,
            OpusManagerCursor.CursorMode.Single -> {
                val new_beat_key = BeatKey(
                    this.opus_manager.cursor.channel,
                    this.opus_manager.cursor.line_offset,
                    beat
                )

                val new_position = this.opus_manager.get_first_position(new_beat_key)
                this.opus_manager.cursor_select(new_beat_key, new_position)
                true
            }
            OpusManagerCursor.CursorMode.Column,
            OpusManagerCursor.CursorMode.Unset -> {
                this.opus_manager.cursor_select_column(beat)
                true
            }
            OpusManagerCursor.CursorMode.Range -> {
                val new_beat_key = this.opus_manager.cursor.range!!.second
                new_beat_key.beat = beat
                this.opus_manager.cursor_select_range(
                    this.opus_manager.cursor.range!!.first,
                    new_beat_key
                )
                true
            }
        }
    }

    fun input_key_c(): Boolean {
        this.opus_manager.unset()
        return true
    }

    fun input_key_i(): Boolean {
        return when (this.opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Line -> TODO()
            OpusManagerCursor.CursorMode.Column -> {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                this.opus_manager.insert_beat_at_cursor(repeat)
                true
            }
            OpusManagerCursor.CursorMode.Single -> {
                var repeat = this.clear_value_buffer(1, maximum=64, minimum=0)
                if (repeat > 0) {
                    val tree = this.opus_manager.get_tree()
                    if (tree.parent == null) {
                        this.opus_manager.split_tree(repeat + 1, true)
                    } else {
                        this.opus_manager.insert(repeat)
                    }
                }
                true
            }
            OpusManagerCursor.CursorMode.Range -> TODO()
            OpusManagerCursor.CursorMode.Unset -> TODO()
        }
    }


    fun input_key_u(): Boolean {
        val repeat = this.clear_value_buffer(1)
        this.opus_manager.apply_undo(repeat)
        return true
    }

    fun input(key_code: Int, event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                this.input_buffer_value = ((this.input_buffer_value ?: 0) * 10) + event.keyCode - KeyEvent.KEYCODE_0
                true
            }

            KeyEvent.KEYCODE_ESCAPE -> {
                if (this.input_buffer_value == null) {
                    false
                } else {
                    this.input_buffer_value = null
                    true
                }
            }

            KeyEvent.KEYCODE_A -> this.input_key_a()
            KeyEvent.KEYCODE_B -> this.input_key_b()
            KeyEvent.KEYCODE_C -> this.input_key_c()
            KeyEvent.KEYCODE_I -> this.input_key_i()
            KeyEvent.KEYCODE_U -> this.input_key_u()

            else -> {
                when (this.opus_manager.cursor.mode) {
                    OpusManagerCursor.CursorMode.Line -> this._input_row(key_code, event)
                    OpusManagerCursor.CursorMode.Column -> this._input_column(key_code, event)
                    OpusManagerCursor.CursorMode.Single -> this._input_single(key_code, event)
                    OpusManagerCursor.CursorMode.Range -> this._input_range(key_code, event)
                    OpusManagerCursor.CursorMode.Unset -> this._input_unset(key_code, event)
                }
            }
        }
    }

    private fun _input_row(key_code: Int, event: KeyEvent): Boolean {
        return false
    }

    private fun _input_column(key_code: Int, event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_H -> {
                val movement_value = this.clear_value_buffer(1)
                val new_beat = max(0, this.opus_manager.cursor.beat - movement_value)
                this.opus_manager.cursor_select_column(new_beat)
                true
            }


            KeyEvent.KEYCODE_J -> {
                val repeat = this.clear_value_buffer(1)
                if (repeat % (this.opus_manager.get_row_count() + 1) != 0) {
                    val visible_line = (repeat % (this.opus_manager.get_row_count() + 1)) - 1
                    this._cursor_select_line_from_column(visible_line)
                }
                true
            }

            KeyEvent.KEYCODE_K -> {
                val master_line_count = this.opus_manager.get_row_count()
                val repeat = this.clear_value_buffer(1)
                if (repeat % (master_line_count + 1) != 0) {
                    val visible_line = master_line_count - (repeat % (master_line_count + 1))
                    this._cursor_select_line_from_column(visible_line)
                }
                true
            }

            KeyEvent.KEYCODE_L -> {
                val movement_value = this.clear_value_buffer(1)
                val new_beat = min(this.opus_manager.beat_count - 1, this.opus_manager.cursor.beat + movement_value)
                this.opus_manager.cursor_select_column(new_beat)
                true
            }
            KeyEvent.KEYCODE_X -> {
                val repeat = this.clear_value_buffer(1)
                if (repeat > 0) {
                    this.opus_manager.remove_beat_at_cursor(repeat)
                }
                true
            }
            else -> {
                false
            }
        }
    }

    private fun _input_single(key_code: Int, event: KeyEvent): Boolean {
        return if (event.isShiftPressed) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_H -> {
                    // TODO: Code duplication here and KEYCODE_L
                    val movement_value = this.clear_value_buffer(1)
                    val cursor = this.opus_manager.cursor
                    var beat = (cursor.beat - movement_value) % this.opus_manager.beat_count
                    if (beat < 0) {
                        beat += this.opus_manager.beat_count
                    }

                    when (cursor.ctl_level) {
                        null -> {
                            val new_beat_key = BeatKey(cursor.channel, cursor.line_offset, beat)
                            val new_position = this.opus_manager.get_first_position(new_beat_key, listOf())
                            this.opus_manager.cursor_select(
                                new_beat_key,
                                new_position
                            )
                        }
                        CtlLineLevel.Line -> {
                            val new_beat_key = BeatKey(cursor.channel, cursor.line_offset, beat)
                            val new_position = this.opus_manager.get_first_position_line_ctl(cursor.ctl_type!!, new_beat_key, listOf())
                            this.opus_manager.cursor_select_ctl_at_line(
                                cursor.ctl_type!!,
                                new_beat_key,
                                new_position
                            )
                        }
                        CtlLineLevel.Channel -> {
                            val new_position = this.opus_manager.get_first_position_channel_ctl(cursor.ctl_type!!, cursor.channel, beat, listOf())
                            this.opus_manager.cursor_select_ctl_at_channel(
                                cursor.ctl_type!!,
                                cursor.channel,
                                beat,
                                new_position
                            )
                        }
                        CtlLineLevel.Global -> {
                            val new_position = this.opus_manager.get_first_position_global_ctl(cursor.ctl_type!!, beat, listOf())
                            this.opus_manager.cursor_select_ctl_at_global(
                                cursor.ctl_type!!,
                                beat,
                                new_position
                            )
                        }
                    }
                    true
                }
                KeyEvent.KEYCODE_L -> {
                    // TODO: Code duplication here and KEYCODE_H
                    val movement_value = this.clear_value_buffer(1)
                    val cursor = this.opus_manager.cursor
                    var beat = (cursor.beat + movement_value) % this.opus_manager.beat_count
                    when (cursor.ctl_level) {
                        null -> {
                            val new_beat_key = BeatKey(cursor.channel, cursor.line_offset, beat)
                            val new_position = this.opus_manager.get_first_position(new_beat_key, listOf())
                            this.opus_manager.cursor_select(
                                new_beat_key,
                                new_position
                            )
                        }
                        CtlLineLevel.Line -> {
                            val new_beat_key = BeatKey(cursor.channel, cursor.line_offset, beat)
                            val new_position = this.opus_manager.get_first_position_line_ctl(cursor.ctl_type!!, new_beat_key, listOf())
                            this.opus_manager.cursor_select_ctl_at_line(
                                cursor.ctl_type!!,
                                new_beat_key,
                                new_position
                            )
                        }
                        CtlLineLevel.Channel -> {
                            val new_position = this.opus_manager.get_first_position_channel_ctl(cursor.ctl_type!!, cursor.channel, beat, listOf())
                            this.opus_manager.cursor_select_ctl_at_channel(
                                cursor.ctl_type!!,
                                cursor.channel,
                                beat,
                                new_position
                            )
                        }
                        CtlLineLevel.Global -> {
                            val new_position = this.opus_manager.get_first_position_global_ctl(cursor.ctl_type!!, beat, listOf())
                            this.opus_manager.cursor_select_ctl_at_global(
                                cursor.ctl_type!!,
                                beat,
                                new_position
                            )
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        } else {
            when (event.keyCode) {
                KeyEvent.KEYCODE_B -> {
                    val cursor = this.opus_manager.cursor
                    val new_beat_key = cursor.get_beatkey()
                    new_beat_key.beat = this.clear_value_buffer(0, 0, this.opus_manager.beat_count - 1)

                    val position = this.opus_manager.get_first_position(new_beat_key)
                    this.opus_manager.cursor_select(new_beat_key, position)
                    true
                }


                KeyEvent.KEYCODE_H -> {
                    val movement_value = this.clear_value_buffer(1)
                    val cursor = this.opus_manager.cursor
                    when (cursor.ctl_level) {
                        CtlLineLevel.Line -> {
                            var working_beat = cursor.beat
                            var working_position = cursor.position
                            val controller = this.opus_manager.channels[cursor.channel].lines[cursor.line_offset].controllers.get_controller(cursor.ctl_type!!)

                            for (i in 0 until movement_value) {
                                val next_pair = controller.get_preceding_leaf_position(
                                    working_beat,
                                    working_position
                                ) ?: break
                                working_beat = next_pair.first
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select_ctl_at_line(cursor.ctl_type!!, BeatKey(working_beat, cursor.channel, cursor.line_offset), working_position)
                        }
                        CtlLineLevel.Channel -> {
                            var working_beat = cursor.beat
                            val channel = cursor.channel
                            var working_position = cursor.position
                            val controller = this.opus_manager.channels[channel].controllers.get_controller(cursor.ctl_type!!)

                            for (i in 0 until movement_value) {
                                val next_pair = controller.get_preceding_leaf_position(
                                    working_beat,
                                    working_position
                                ) ?: break
                                working_beat = next_pair.first
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)
                        }
                        CtlLineLevel.Global -> {
                            var working_beat = cursor.beat
                            var working_position = cursor.position
                            val controller = this.opus_manager.controllers.get_controller(cursor.ctl_type!!)

                            for (i in 0 until movement_value) {
                                val next_pair = controller.get_preceding_leaf_position(
                                    working_beat,
                                    working_position
                                ) ?: break
                                working_beat = next_pair.first
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

                        }
                        null -> {
                            var working_beat_key = this.opus_manager.cursor.get_beatkey()
                            var working_position = this.opus_manager.cursor.get_position()

                            for (i in 0 until movement_value) {
                                val next_pair = this.opus_manager.get_preceding_leaf_position(
                                    working_beat_key,
                                    working_position
                                ) ?: break
                                working_beat_key = next_pair.first
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select(working_beat_key, working_position)

                        }
                    }
                    true
                }

                KeyEvent.KEYCODE_L -> {
                    val movement_value = this.clear_value_buffer(1)
                    val cursor = this.opus_manager.cursor
                    when (cursor.ctl_level) {
                        CtlLineLevel.Line -> {
                            var working_beat_key = cursor.get_beatkey()
                            var working_position = cursor.get_position()

                            for (i in 0 until movement_value) {
                                val next_pair = this.opus_manager.get_line_ctl_proceding_leaf_position(
                                    cursor.ctl_type!!,
                                    working_beat_key,
                                    working_position
                                ) ?: break
                                working_beat_key = BeatKey(
                                    working_beat_key.channel,
                                    working_beat_key.line_offset,
                                    next_pair.first
                                )
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select_ctl_at_line(cursor.ctl_type!!, working_beat_key, working_position)
                        }
                        CtlLineLevel.Channel -> {
                            val channel = cursor.channel
                            var working_beat = cursor.beat
                            var working_position = cursor.position
                            for (i in 0 until movement_value) {
                                val next_pair = this.opus_manager.get_channel_ctl_proceding_leaf_position(
                                    cursor.ctl_type!!,
                                    channel,
                                    working_beat,
                                    working_position
                                ) ?: break
                                working_beat = next_pair.first
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select_ctl_at_channel(cursor.ctl_type!!, channel, working_beat, working_position)

                        }
                        CtlLineLevel.Global -> {
                            var working_beat = cursor.beat
                            var working_position = cursor.position
                            for (i in 0 until movement_value) {
                                val next_pair = this.opus_manager.get_global_ctl_proceding_leaf_position(
                                    cursor.ctl_type!!,
                                    working_beat,
                                    working_position
                                ) ?: break
                                working_beat = next_pair.first
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select_ctl_at_global(cursor.ctl_type!!, working_beat, working_position)

                        }
                        null -> {
                            var working_beat_key = cursor.get_beatkey()
                            var working_position = cursor.get_position()

                            for (i in 0 until movement_value) {
                                val next_pair = this.opus_manager.get_proceding_leaf_position(
                                    working_beat_key,
                                    working_position
                                ) ?: break
                                working_beat_key = next_pair.first
                                working_position = next_pair.second
                            }
                            this.opus_manager.cursor_select(working_beat_key, working_position)
                        }
                    }

                    true
                }

                KeyEvent.KEYCODE_J -> {
                    this._cursor_select_next_leaf_down()
                    true
                }
                KeyEvent.KEYCODE_K -> {
                    this._cursor_select_next_leaf_up()
                    true
                }

                KeyEvent.KEYCODE_U -> {
                    val repeat = this.clear_value_buffer(1)
                    this.opus_manager.apply_undo(repeat)
                    true
                }

                KeyEvent.KEYCODE_X -> {
                    val repeat = this.clear_value_buffer(1)
                    if (repeat > 0) {
                        this.opus_manager.remove_at_cursor(repeat)
                    }
                    true
                }


                KeyEvent.KEYCODE_SLASH -> {
                    val splits = this.clear_value_buffer(2, minimum=2, maximum=64)
                    val cursor = this.opus_manager.cursor
                    when (cursor.ctl_level) {
                        null -> {
                            this.opus_manager.split_tree(splits)
                        }

                        CtlLineLevel.Line -> TODO()
                        CtlLineLevel.Channel -> TODO()
                        CtlLineLevel.Global -> {
                            this.opus_manager.split_global_ctl_tree(
                                cursor.ctl_type!!,
                                cursor.beat,
                                cursor.position,
                                splits
                            )
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }

        }
    }

    private fun _input_range(key_code: Int, event: KeyEvent): Boolean {
        return false
    }
    private fun _input_unset(key_code: Int, event: KeyEvent): Boolean {
        return false
    }

    private fun _cursor_select_next_prev_leaf(direction_up: Boolean = false) {
        val repeat = this.clear_value_buffer(1)
        val current_line = when (this.opus_manager.cursor.ctl_level) {
            null -> {
                this.opus_manager.get_visible_row_from_ctl_line(
                    this.opus_manager.get_actual_line_index(
                        this.opus_manager.get_instrument_line_index(
                            this.opus_manager.cursor.channel,
                            this.opus_manager.cursor.line_offset
                        )
                    )
                )!!
            }
            CtlLineLevel.Global -> {
                this.opus_manager.get_visible_row_from_ctl_line_global(this.opus_manager.cursor.ctl_type!!)
            }
            CtlLineLevel.Line -> TODO()
            CtlLineLevel.Channel -> TODO()
        } + 1
        val line_count = this.opus_manager.get_row_count()
        val direction_mod = if (direction_up) {
            -1
        } else {
            1
        }

        val next_line = ((current_line + (repeat * direction_mod)) % (line_count + 1))
        if (next_line == current_line) {
            return
        } else if (next_line == 0) {
            this.opus_manager.cursor_select_column(this.opus_manager.cursor.beat)
        } else {
            val next_ctl_line = this.opus_manager.get_ctl_line_from_row(next_line - 1)
            val (target, ctl_level, ctl_type) = this.opus_manager.get_ctl_line_info(next_ctl_line)
            when (ctl_level) {
                CtlLineLevel.Global -> {
                    val beat = this.opus_manager.cursor.beat
                    val target_position = this.opus_manager.get_first_position_global_ctl(ctl_type!!, beat)
                    this.opus_manager.cursor_select_ctl_at_global(ctl_type!!, beat, target_position)
                }
                null -> {
                    val (channel, offset) = this.opus_manager.get_channel_and_line_offset(target)
                    val target_beat_key = BeatKey(channel, offset, this.opus_manager.cursor.beat)
                    val target_position = this.opus_manager.get_first_position(target_beat_key, listOf())
                    this.opus_manager.cursor_select(target_beat_key, target_position)
                }
                CtlLineLevel.Line -> TODO()
                CtlLineLevel.Channel -> TODO()
            }
        }
    }

    private fun _cursor_select_next_leaf_down() {
        this._cursor_select_next_prev_leaf(false)
    }

    private fun _cursor_select_next_leaf_up() {
        this._cursor_select_next_prev_leaf(true)
    }


    private fun _cursor_select_line_from_column(visible_line: Int) {
        val cursor = this.opus_manager.cursor
        val ctl_line = this.opus_manager.get_ctl_line_from_row(visible_line)

        val (target, ctl_level, ctl_type) = this.opus_manager.get_ctl_line_info(ctl_line)
        when (ctl_level) {
            CtlLineLevel.Line -> TODO()
            CtlLineLevel.Channel -> TODO()
            CtlLineLevel.Global -> {
                val target_position = this.opus_manager.get_first_position_global_ctl(ctl_type!!, cursor.beat, listOf())
                this.opus_manager.cursor_select_ctl_at_global(ctl_type!!, cursor.beat, target_position)
            }
            null -> {
                val (channel, line_offset) = this.opus_manager.get_channel_and_line_offset(target)
                val target_key = BeatKey(channel, line_offset, cursor.beat)
                val target_position = this.opus_manager.get_first_position(target_key, listOf())
                this.opus_manager.cursor_select(target_key, target_position)
            }
        }
    }

}
