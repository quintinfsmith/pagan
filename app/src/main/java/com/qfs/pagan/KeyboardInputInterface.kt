package com.qfs.pagan
import android.view.KeyEvent
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusManagerCursor
import kotlin.math.max
import kotlin.math.min
import com.qfs.pagan.OpusLayerInterface as OpusManager

class KeyboardInputInterface(var opus_manager: OpusManager) {

    abstract class KeyStrokeNode(val keyboard_interface: KeyboardInputInterface) {
        abstract fun call(opus_manager: OpusManager): Boolean
    }

    abstract class CursorSpecificKeyStrokeNode(keyboard_interface: KeyboardInputInterface): KeyStrokeNode(keyboard_interface) {
        /*
            NOTE: When overriding functions, don't call super().
            the super only raises a flag to return false in the call()
         */
        var _no_function_called = false
        internal fun clear_value_buffer(default: Int = 0, minimum: Int? = null, maximum: Int? = null): Int {
            return this.keyboard_interface.clear_value_buffer(default, minimum, maximum)
        }


        override fun call(opus_manager: OpusManager): Boolean {
            when (opus_manager.cursor.mode) {
                OpusManagerCursor.CursorMode.Line -> this.line(opus_manager)
                OpusManagerCursor.CursorMode.Column -> this.column(opus_manager)
                OpusManagerCursor.CursorMode.Single -> this.single(opus_manager)
                OpusManagerCursor.CursorMode.Range -> this.range(opus_manager)
                OpusManagerCursor.CursorMode.Unset -> this.unset(opus_manager)
            }

            val output = this._no_function_called

            this._no_function_called = false

            return output
        }

        open fun column(opus_manager: OpusManager) {
            this._no_function_called = true
        }

        open fun line(opus_manager: OpusManager) {
            this._no_function_called = true
        }

        open fun single(opus_manager: OpusManager) {
            this._no_function_called = true
        }

        open fun unset(opus_manager: OpusManager) {
            this._no_function_called = true
        }

        open fun range(opus_manager: OpusManager) {
            this._no_function_called = true
        }
    }

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

    val key_code_map = hashMapOf(
        Pair(KeyEvent.KEYCODE_A, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                opus_manager.insert_line(repeat)
            }
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                opus_manager.insert_beat_after_cursor(repeat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                var repeat = this.clear_value_buffer(1, maximum=64)
                if (repeat > 0) {
                    val tree = opus_manager.get_tree()
                    if (tree.parent == null) {
                        opus_manager.split_tree(repeat + 1)
                    } else {
                        opus_manager.insert_after(repeat)
                    }
                }
            }
        },

        Pair(KeyEvent.KEYCODE_B, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun unset(opus_manager: OpusLayerInterface) {
                this.column(opus_manager)
            }

            override fun column(opus_manager: OpusLayerInterface) {
                val beat = this.clear_value_buffer(0, 0, opus_manager.beat_count - 1)
                opus_manager.cursor_select_column(beat)
            }

            override fun line(opus_manager: OpusLayerInterface) {
                this.single(opus_manager)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val beat = this.clear_value_buffer(0, 0, opus_manager.beat_count - 1)
                val new_beat_key = BeatKey(
                    opus_manager.cursor.channel,
                    opus_manager.cursor.line_offset,
                    beat
                )
                val new_position = opus_manager.get_first_position(new_beat_key)
                opus_manager.cursor_select(new_beat_key, new_position)
            }

            override fun range(opus_manager: OpusLayerInterface) {
                val beat = this.clear_value_buffer(0, 0, opus_manager.beat_count - 1)
                val new_beat_key = opus_manager.cursor.range!!.second
                new_beat_key.beat = beat

                opus_manager.cursor_select_range(
                    opus_manager.cursor.range!!.first,
                    new_beat_key
                )
            }
        },

        Pair(KeyEvent.KEYCODE_B, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun unset(opus_manager: OpusManager) {
                this.column(opus_manager)
            }

            override fun column(opus_manager: OpusLayerInterface) {
                val beat = this.clear_value_buffer(opus_manager.beat_count - 1, 0, opus_manager.beat_count - 1)
                opus_manager.cursor_select_column(beat)
            }
            override fun single(opus_manager: OpusLayerInterface) {
                val beat = this.clear_value_buffer(opus_manager.cursor.beat, 0, opus_manager.beat_count - 1)
                opus_manager.cursor_select_column(beat)
            }
        },

        Pair(KeyEvent.KEYCODE_C, false) to object: KeyStrokeNode(this) {
            override fun call(opus_manager: OpusManager): Boolean {
                opus_manager.unset()
                return true
            }
        },

        Pair(KeyEvent.KEYCODE_H, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)
                if (movement_value != 0) {
                    val beat_key = BeatKey(
                        opus_manager.cursor.channel,
                        opus_manager.cursor.line_offset,
                        max(0, opus_manager.cursor.beat - movement_value)
                    )

                    opus_manager.cursor_select(
                        beat_key,
                        opus_manager.get_first_position(beat_key)
                    )
                }
            }

            override fun range(opus_manager: OpusLayerInterface) {
                opus_manager.cursor_select_range(
                    opus_manager.cursor.range!!.first,
                    BeatKey(
                        opus_manager.cursor.range!!.second.channel,
                        opus_manager.cursor.range!!.second.line_offset,
                        max(0, opus_manager.cursor.range!!.second.beat - this.clear_value_buffer(1, minimum=0))
                    )
                )
            }

            override fun column(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)
                val new_beat = max(0, opus_manager.cursor.beat - movement_value)
                opus_manager.cursor_select_column(new_beat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                opus_manager.select_previous_leaf(this.clear_value_buffer(1, minimum=0))
            }
        },

        Pair(KeyEvent.KEYCODE_H, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1)
                opus_manager.select_first_leaf_in_previous_beat(movement_value)
            }
        },

        Pair(KeyEvent.KEYCODE_I, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1, maximum=9999, minimum=0)
                opus_manager.new_line(opus_manager.cursor.channel, opus_manager.cursor.line_offset, repeat)
            }
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1, maximum=9999)
                opus_manager.insert_beat_at_cursor(repeat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1, maximum=64, minimum=0)
                if (repeat > 0) {
                    opus_manager.insert(repeat)
                }
            }
        },

        Pair(KeyEvent.KEYCODE_J, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)
                opus_manager.move_to_next_visible_line(movement_value)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                this.keyboard_interface._cursor_select_next_leaf_down()
            }

            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1, minimum=0)
                if (repeat != 0) {
                    this.keyboard_interface._cursor_select_line_from_column(repeat)
                }
            }
        },

        Pair(KeyEvent.KEYCODE_J, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1)
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
                val new_key = BeatKey(new_channel, 0, opus_manager.cursor.beat)

                opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
            }
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1)
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
                opus_manager.cursor_select_line(new_channel, 0)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1)
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return

                val new_key = opus_manager.cursor.get_beatkey()
                new_key.line_offset = 0
                new_key.channel = new_channel

                opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
            }
        },

        Pair(KeyEvent.KEYCODE_K, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun column(opus_manager: OpusLayerInterface) {
                val master_line_count = opus_manager.get_row_count()
                val repeat = this.clear_value_buffer(1, minimum=0)
                if (repeat != 0) {
                    this.keyboard_interface._cursor_select_line_from_column(repeat)
                }
            }

            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)
                opus_manager.move_to_previous_visible_line(movement_value)

            }

            override fun single(opus_manager: OpusLayerInterface) {
                this.keyboard_interface._cursor_select_next_leaf_up()
            }
            override fun range(opus_manager: OpusLayerInterface) {
                val new_beat_key = opus_manager.cursor.range!!.second
                val movement_value = this.clear_value_buffer(1, maximum=new_beat_key.beat, minimum=0)
                val visible_line = (movement_value % (opus_manager.get_row_count() - 1)) - 1
                val (channel, line_offset) = opus_manager.get_channel_and_line_offset(
                    opus_manager.get_ctl_line_from_row(visible_line)
                )

                opus_manager.cursor_select_range(
                    opus_manager.cursor.range!!.first,
                    new_beat_key
                )
            }
        },

        Pair(KeyEvent.KEYCODE_K, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1) * -1
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
                val new_key = BeatKey(new_channel, 0, opus_manager.cursor.beat)

                opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
            }
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1) * -1
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
                opus_manager.cursor_select_line(new_channel, 0)
            }
            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1) * -1
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return

                val new_key = opus_manager.cursor.get_beatkey()
                new_key.line_offset = 0
                new_key.channel = new_channel

                opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
            }

        },

        Pair(KeyEvent.KEYCODE_L, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)
                if (movement_value != 0) {
                    val beat_key = BeatKey(
                        opus_manager.cursor.channel,
                        opus_manager.cursor.line_offset,
                        min(opus_manager.cursor.beat + movement_value, opus_manager.beat_count - 1)
                    )

                    opus_manager.cursor_select(
                        beat_key,
                        opus_manager.get_first_position(beat_key)
                    )
                }
            }

            override fun column(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)
                val new_beat = min(opus_manager.beat_count - 1, opus_manager.cursor.beat + movement_value)
                opus_manager.cursor_select_column(new_beat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                opus_manager.select_next_leaf(this.clear_value_buffer(1, minimum=0))
            }

            override fun range(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)

                opus_manager.cursor_select_range(
                    opus_manager.cursor.range!!.first,
                    BeatKey(
                        opus_manager.cursor.range!!.second.channel,
                        opus_manager.cursor.range!!.second.line_offset,
                        min(opus_manager.beat_count - 1, opus_manager.cursor.range!!.second.beat + movement_value)
                    )
                )
            }
        },

        Pair(KeyEvent.KEYCODE_L, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusLayerInterface) {
                val movement_value = this.clear_value_buffer(1, minimum=0)
                val cursor = opus_manager.cursor
                var beat = min(opus_manager.beat_count - 1, cursor.beat + movement_value)
                opus_manager.select_first_in_beat(beat)
            }
        },

        Pair(KeyEvent.KEYCODE_S, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusLayerInterface) {
                val splits = this.clear_value_buffer(2, minimum=2, maximum=64)
                val cursor = opus_manager.cursor
                when (cursor.ctl_level) {
                    null -> opus_manager.split_tree(splits)
                    CtlLineLevel.Line -> TODO()
                    CtlLineLevel.Channel -> TODO()
                    CtlLineLevel.Global -> {
                        opus_manager.split_global_ctl_tree(
                            cursor.ctl_type!!,
                            cursor.beat,
                            cursor.position,
                            splits
                        )
                    }
                }
            }
        },

        Pair(KeyEvent.KEYCODE_U, false) to object: CursorSpecificKeyStrokeNode(this) {
            fun apply_undo(opus_manager: OpusManager) {
                val repeat = this.clear_value_buffer(1)
                opus_manager.apply_undo(repeat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                this.apply_undo(opus_manager)
            }
            override fun column(opus_manager: OpusLayerInterface) {
                this.apply_undo(opus_manager)
            }
            override fun line(opus_manager: OpusLayerInterface) {
                this.apply_undo(opus_manager)
            }
            override fun unset(opus_manager: OpusLayerInterface) {
                this.apply_undo(opus_manager)
            }
            override fun range(opus_manager: OpusLayerInterface) {
                this.apply_undo(opus_manager)
            }
        },

        Pair(KeyEvent.KEYCODE_X, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1)
                if (repeat > 0) {
                    opus_manager.remove_beat_at_cursor(repeat)
                }
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.clear_value_buffer(1)
                if (repeat > 0) {
                    opus_manager.remove_at_cursor(repeat)
                }
            }

            override fun line(opus_manager: OpusLayerInterface) {
                val cursor = opus_manager.cursor
                val line_count = opus_manager.get_all_channels()[cursor.channel].lines.size
                val repeat = this.clear_value_buffer(1, maximum=line_count - 1, minimum=0)
                if (repeat > 0) {
                    opus_manager.remove_line(repeat)
                }
            }

            override fun range(opus_manager: OpusLayerInterface) {
                opus_manager.unset()
            }
        }
    )

    fun input(key_code: Int, event: KeyEvent): Boolean {
        return if (this.key_code_map.containsKey(Pair(key_code, event.isShiftPressed))) {
            this.key_code_map[Pair(key_code, event.isShiftPressed)]!!.call(this.opus_manager)
        } else {
            when (event.keyCode) {
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
                else -> false
            }
        }
    }

    private fun _cursor_select_next_prev_leaf(direction_up: Boolean = false) {
        val repeat = this.clear_value_buffer(1)
        val cursor = this.opus_manager.cursor
        val current_line = when (cursor.ctl_level) {
            null -> {
                this.opus_manager.get_visible_row_from_ctl_line(
                    this.opus_manager.get_actual_line_index(
                        this.opus_manager.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )!!
            }
            CtlLineLevel.Global -> this.opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
            CtlLineLevel.Line -> this.opus_manager.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
            CtlLineLevel.Channel -> this.opus_manager.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel)
        }

        val line_count = this.opus_manager.get_row_count()
        val direction_mod = if (direction_up) {
            -1
        } else {
            1
        }

        when (val next_line = max(0, min(line_count - 1, current_line + (repeat * direction_mod)))) {
            current_line -> return
            0 -> this.opus_manager.cursor_select_column(this.opus_manager.cursor.beat)
            else -> {
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
    }

    private fun _cursor_select_next_leaf_down() {
        this._cursor_select_next_prev_leaf(false)
    }

    private fun _cursor_select_next_leaf_up() {
        this._cursor_select_next_prev_leaf(true)
    }

    private fun _cursor_select_line_from_column(repeat: Int) {
        val cursor = this.opus_manager.cursor
        var current_visible_line = when (cursor.ctl_level) {
            CtlLineLevel.Line -> {
                this.opus_manager.get_visible_row_from_ctl_line_line(
                    cursor.ctl_type!!,
                    cursor.channel,
                    cursor.line_offset
                )
            }
            CtlLineLevel.Channel -> {
                this.opus_manager.get_visible_row_from_ctl_line_channel(
                    cursor.ctl_type!!,
                    cursor.channel
                )
            }

            CtlLineLevel.Global -> {
                this.opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
            }

            null -> {
                this.opus_manager.get_visible_row_from_ctl_line(
                    this.opus_manager.get_ctl_line_from_row(
                        this.opus_manager.get_instrument_line_index(
                            cursor.channel,
                            cursor.line_offset
                        )
                    )
                )!!
            }
        }

        val ctl_line = this.opus_manager.get_ctl_line_from_row(current_visible_line)
        val (target, ctl_level, ctl_type) = this.opus_manager.get_ctl_line_info(ctl_line)

        when (ctl_level) {
            CtlLineLevel.Line -> {
                val target_position = this.opus_manager.get_first_position_line_ctl(
                    ctl_type!!,
                    cursor.get_beatkey(),
                    listOf()
                )
                this.opus_manager.cursor_select_ctl_at_line(ctl_type!!, cursor.get_beatkey(), target_position)
            }
            CtlLineLevel.Channel -> {
                var target_position = this.opus_manager.get_first_position_channel_ctl(
                    ctl_type!!,
                    cursor.channel,
                    cursor.beat,
                    listOf()
                )
                this.opus_manager.cursor_select_ctl_at_channel(ctl_type!!, cursor.channel, cursor.beat, target_position)
            }
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
