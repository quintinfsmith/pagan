package com.qfs.pagan
import android.util.Log
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

    fun input(key_code: Int, event: KeyEvent): Boolean {
        Log.d("AAA", "$event")
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

            else -> {
                when (this.opus_manager.cursor.mode) {
                    OpusManagerCursor.CursorMode.Row -> this._input_row(key_code, event)
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
            KeyEvent.KEYCODE_A -> {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                this.opus_manager.insert_beat_after_cursor(repeat)
                true
            }

            KeyEvent.KEYCODE_H -> {
                val movement_value = this.clear_value_buffer(1)
                val new_beat = max(0, this.opus_manager.cursor.beat - movement_value)
                this.opus_manager.cursor_select_column(new_beat)
                true
            }

            KeyEvent.KEYCODE_I -> {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                this.opus_manager.insert_beat_at_cursor(repeat)
                true
            }

            KeyEvent.KEYCODE_J -> {
                val repeat = this.clear_value_buffer(1)
                if (repeat % (this.opus_manager.get_visible_master_line_count() + 1) != 0) {
                    val visible_line = (repeat % (this.opus_manager.get_visible_master_line_count() + 1)) - 1
                    this._cursor_select_line_from_column(visible_line)
                }
                true
            }

            KeyEvent.KEYCODE_K -> {
                val master_line_count = this.opus_manager.get_visible_master_line_count()
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

            KeyEvent.KEYCODE_U -> {
                val repeat = this.clear_value_buffer(1)
                this.opus_manager.apply_undo(repeat)
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
        return when (event.keyCode) {
            KeyEvent.KEYCODE_A -> {
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

            KeyEvent.KEYCODE_C -> {
                if (this.clear_value_buffer(1) != 0) {
                    this.opus_manager.unset()
                }
                true
            }

            KeyEvent.KEYCODE_H -> {
                val movement_value = this.clear_value_buffer(1)
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
                true
            }

            KeyEvent.KEYCODE_I -> {
                var repeat = this.clear_value_buffer(1, maximum=64)
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

            KeyEvent.KEYCODE_L -> {
                val movement_value = this.clear_value_buffer(1)
                var working_beat_key = this.opus_manager.cursor.get_beatkey()
                var working_position = this.opus_manager.cursor.get_position()

                for (i in 0 until movement_value) {
                    val next_pair = this.opus_manager.get_proceding_leaf_position(
                        working_beat_key,
                        working_position
                    ) ?: break
                    working_beat_key = next_pair.first
                    working_position = next_pair.second
                }
                this.opus_manager.cursor_select(working_beat_key, working_position)

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
                    this.opus_manager.remove(repeat)
                }
                true
            }

            KeyEvent.KEYCODE_SLASH -> {
                val splits = this.clear_value_buffer(2, minimum=2, maximum=64)
                this.opus_manager.split_tree(splits)
                true
            }
            else -> {
                false
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
                    this.opus_manager.get_ctl_line_index(
                        this.opus_manager.get_abs_offset(
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
        val line_count = this.opus_manager.get_visible_master_line_count()
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
            val next_ctl_line = this.opus_manager.get_ctl_line_from_visible_row(next_line - 1)
            val (target, ctl_level, ctl_type) = this.opus_manager.get_ctl_line_info(next_ctl_line)
            when (ctl_level) {
                CtlLineLevel.Global -> {
                    val beat = this.opus_manager.cursor.beat
                    val target_position = this.opus_manager.get_first_position_global_ctl(ctl_type!!, beat)
                    this.opus_manager.cursor_select_ctl_at_global(ctl_type!!, beat, target_position)
                }
                null -> {
                    val (channel, offset) = this.opus_manager.get_std_offset(target)
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
        val ctl_line = this.opus_manager.get_ctl_line_from_visible_row(visible_line)

        val (target, ctl_level, ctl_type) = this.opus_manager.get_ctl_line_info(ctl_line)
        when (ctl_level) {
            CtlLineLevel.Line -> TODO()
            CtlLineLevel.Channel -> TODO()
            CtlLineLevel.Global -> {
                val target_position = this.opus_manager.get_first_position_global_ctl(ctl_type!!, cursor.beat, listOf())
                this.opus_manager.cursor_select_ctl_at_global(ctl_type!!, cursor.beat, target_position)
            }
            null -> {
                val (channel, line_offset) = this.opus_manager.get_std_offset(target)
                val target_key = BeatKey(channel, line_offset, cursor.beat)
                val target_position = this.opus_manager.get_first_position(target_key, listOf())
                this.opus_manager.cursor_select(target_key, target_position)
            }
        }
    }

}