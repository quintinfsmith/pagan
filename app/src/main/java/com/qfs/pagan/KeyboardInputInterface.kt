package com.qfs.pagan
import android.view.KeyEvent
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerOverlapControl
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import kotlin.math.max
import kotlin.math.min
import com.qfs.pagan.OpusLayerInterface as OpusManager

class KeyboardInputInterface(var opus_manager: OpusManager) {
    abstract class KeyStrokeNode(val keyboard_interface: KeyboardInputInterface) {
        abstract fun call(opus_manager: OpusManager): Boolean
        internal fun get_buffer_value(default: Int = 0, minimum: Int? = null, maximum: Int? = null): Int {
            return this.keyboard_interface.get_buffer_value(default, minimum, maximum)
        }


    }

    abstract class CursorSpecificKeyStrokeNode(keyboard_interface: KeyboardInputInterface): KeyStrokeNode(keyboard_interface) {
        /*
            NOTE: When overriding functions, don't call super().
            the super only raises a flag to return false in the call()
         */
        var _no_function_called = false
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

    fun get_buffer_value(default: Int = 0, minimum: Int? = null, maximum: Int? = null): Int {
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

    fun clear_buffer_value() {
        this.input_buffer_value = null
    }

    val key_code_map = hashMapOf(
        Pair(KeyEvent.KEYCODE_ESCAPE, false) to object: KeyStrokeNode(this) {
            override fun call(opus_manager: OpusManager): Boolean {
                return if (this@KeyboardInputInterface.input_buffer_value == null) {
                    false
                } else {
                    this@KeyboardInputInterface.input_buffer_value = null
                    true
                }
            }
        },

        Pair(KeyEvent.KEYCODE_GRAVE, false) to object: KeyStrokeNode(this) {
            override fun call(opus_manager: OpusManager): Boolean {
                try {
                    opus_manager.toggle_percussion_visibility()
                } catch (e: OpusManager.HidingNonEmptyPercussionException) {
                    // pass
                }
                return true
            }
        },

        Pair(KeyEvent.KEYCODE_LEFT_BRACKET, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusManager) {
                if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
                    opus_manager.set_percussion_event_at_cursor()
                } else {
                    opus_manager.set_note_octave_at_cursor(
                        this.get_buffer_value(0, 0, 7)
                    )
                }
            }
        },

        Pair(KeyEvent.KEYCODE_RIGHT_BRACKET, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusManager) {
                opus_manager.set_note_offset_at_cursor(
                    this.get_buffer_value(0, 0, opus_manager.tuning_map.size - 1)
                )
            }
        },

        Pair(KeyEvent.KEYCODE_A, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, maximum=9999)
                opus_manager.insert_line(repeat)
            }
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, maximum=9999)
                opus_manager.insert_beat_after_cursor(repeat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                var repeat = this.get_buffer_value(1, maximum=64)
                if (repeat > 0) {
                    val tree = opus_manager.get_tree()
                    if (tree.parent == null) {
                        try {
                            opus_manager.split_tree(repeat + 1)
                        } catch (e: OpusLayerOverlapControl.BlockedTreeException) {
                            // pass
                        }
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
                val beat = this.get_buffer_value(0, 0, opus_manager.beat_count - 1)
                opus_manager.cursor_select_column(beat)
            }

            override fun line(opus_manager: OpusLayerInterface) {
                this.single(opus_manager)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val beat = this.get_buffer_value(0, 0, opus_manager.beat_count - 1)
                val new_beat_key = BeatKey(
                    opus_manager.cursor.channel,
                    opus_manager.cursor.line_offset,
                    beat
                )
                val new_position = opus_manager.get_first_position(new_beat_key)
                opus_manager.cursor_select(new_beat_key, new_position)
            }

            override fun range(opus_manager: OpusLayerInterface) {
                val beat = this.get_buffer_value(0, 0, opus_manager.beat_count - 1)
                val new_beat_key = opus_manager.cursor.range!!.second
                new_beat_key.beat = beat

                opus_manager.cursor_select_range(
                    opus_manager.cursor.range!!.first,
                    new_beat_key
                )
            }
        },

        Pair(KeyEvent.KEYCODE_B, true) to object: KeyStrokeNode(this) {
            override fun call(opus_manager: OpusLayerInterface): Boolean {
                val default = if (opus_manager.cursor.mode == OpusManagerCursor.CursorMode.Single) {
                    opus_manager.cursor.beat
                } else {
                    opus_manager.beat_count - 1
                }
                val beat = this.get_buffer_value(default, 0, opus_manager.beat_count - 1)

                opus_manager.cursor_select_column(beat)
                return true
            }
        },

        Pair(KeyEvent.KEYCODE_C, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusManager) {
                when (opus_manager.cursor.ctl_level) {
                    CtlLineLevel.Line -> TODO()
                    CtlLineLevel.Channel -> TODO()
                    CtlLineLevel.Global -> TODO()
                    null -> {
                        if (opus_manager.marked_range != null) {
                            opus_manager.overwrite_beat_range(
                                opus_manager.cursor.get_beatkey(),
                                opus_manager.marked_range!!.first,
                                opus_manager.marked_range!!.second,
                            )
                            opus_manager.marked_range = null
                        }
                    }
                }
            }
        },
        Pair(KeyEvent.KEYCODE_C, true) to object: KeyStrokeNode(this) {
            override fun call(opus_manager: OpusManager): Boolean {
                val visible_channels = opus_manager.get_visible_channels()
                val channel = this.get_buffer_value(visible_channels.size - 1, 0, visible_channels.size - 1)
                opus_manager.cursor_select_line(channel, 0)
                return true
            }
        },

        Pair(KeyEvent.KEYCODE_D, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusManager) {
                val beatkey = opus_manager.cursor.get_beatkey()
                opus_manager.cursor_select_range(beatkey, beatkey)
            }
        },

        Pair(KeyEvent.KEYCODE_H, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)
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
                        max(0, opus_manager.cursor.range!!.second.beat - this.get_buffer_value(1, minimum=0))
                    )
                )
            }

            override fun column(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)
                val new_beat = max(0, opus_manager.cursor.beat - movement_value)
                opus_manager.cursor_select_column(new_beat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                opus_manager.select_previous_leaf(this.get_buffer_value(1, minimum=0))
            }
        },

        Pair(KeyEvent.KEYCODE_H, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1)
                opus_manager.select_first_leaf_in_previous_beat(movement_value)
            }
        },

        Pair(KeyEvent.KEYCODE_I, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, maximum=9999, minimum=0)
                opus_manager.new_line(opus_manager.cursor.channel, opus_manager.cursor.line_offset, repeat)
            }

            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, maximum=9999)
                opus_manager.insert_beat_at_cursor(repeat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, maximum=64, minimum=0)
                if (repeat > 0) {
                    val tree = opus_manager.get_tree()
                    if (tree.parent == null) {
                        try {
                            opus_manager.split_tree(repeat + 1, true)
                        } catch (e: OpusLayerOverlapControl.BlockedTreeException) {
                            // pass
                        }
                    } else {
                        opus_manager.insert(repeat)
                    }
                }
            }
        },

        Pair(KeyEvent.KEYCODE_J, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)
                opus_manager.move_to_next_visible_line(movement_value)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                this.keyboard_interface._cursor_select_next_leaf_down()
            }
        },

        Pair(KeyEvent.KEYCODE_J, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, minimum=0)
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
                val new_key = BeatKey(new_channel, 0, opus_manager.cursor.beat)

                opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
            }
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, minimum=0)
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
                opus_manager.cursor_select_line(new_channel, 0)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, minimum=0)
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return

                val new_key = opus_manager.cursor.get_beatkey()
                new_key.line_offset = 0
                new_key.channel = new_channel

                opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
            }
        },

        Pair(KeyEvent.KEYCODE_K, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)
                opus_manager.move_to_previous_visible_line(movement_value)

            }

            override fun single(opus_manager: OpusLayerInterface) {
                this.keyboard_interface._cursor_select_next_leaf_up()
            }

            override fun range(opus_manager: OpusLayerInterface) {
                val new_beat_key = opus_manager.cursor.range!!.second
                val movement_value = this.get_buffer_value(1, maximum=new_beat_key.beat, minimum=0)
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
            override fun line(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1) * -1
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
                opus_manager.cursor_select_line(new_channel, 0)
            }
            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1) * -1
                val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return

                val new_key = opus_manager.cursor.get_beatkey()
                new_key.line_offset = 0
                new_key.channel = new_channel

                opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
            }
        },

        Pair(KeyEvent.KEYCODE_L, false) to object: CursorSpecificKeyStrokeNode(this) {

            override fun column(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)
                val new_beat = min(opus_manager.beat_count - 1, opus_manager.cursor.beat + movement_value)
                opus_manager.cursor_select_column(new_beat)
            }

            override fun single(opus_manager: OpusLayerInterface) {
                opus_manager.select_next_leaf(this.get_buffer_value(1, minimum=0))
            }

            override fun range(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)

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
            override fun line(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)
                if (movement_value != 0) {
                    val beat_key = BeatKey(
                        opus_manager.cursor.channel,
                        opus_manager.cursor.line_offset,
                        min(movement_value - 1, opus_manager.beat_count - 1)
                    )

                    opus_manager.cursor_select(
                        beat_key,
                        opus_manager.get_first_position(beat_key)
                    )
                }
            }
            override fun single(opus_manager: OpusLayerInterface) {
                val movement_value = this.get_buffer_value(1, minimum=0)
                val cursor = opus_manager.cursor
                var beat = min(opus_manager.beat_count - 1, cursor.beat + movement_value)
                opus_manager.select_first_in_beat(beat)
            }
        },

        Pair(KeyEvent.KEYCODE_M, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusManager) {
                when (opus_manager.cursor.ctl_level) {
                    CtlLineLevel.Line -> TODO()
                    CtlLineLevel.Channel -> TODO()
                    CtlLineLevel.Global -> TODO()
                    null -> {
                        if (opus_manager.marked_range != null) {
                            opus_manager.move_beat_range(
                                opus_manager.cursor.get_beatkey(),
                                opus_manager.marked_range!!.first,
                                opus_manager.marked_range!!.second,
                            )
                            opus_manager.marked_range = null
                        }
                    }
                }
            }

            override fun range(opus_manager: OpusManager) {
                when (opus_manager.cursor.ctl_level) {
                    CtlLineLevel.Line -> TODO()
                    CtlLineLevel.Channel -> TODO()
                    CtlLineLevel.Global -> TODO()
                    null -> {
                        val range = opus_manager.cursor.range
                        opus_manager.marked_range = range
                        val beat_key = opus_manager.cursor.range!!.second
                        val position = opus_manager.get_first_position(beat_key)
                        opus_manager.cursor_select(beat_key, position)
                    }
                }
            }
        },
        Pair(KeyEvent.KEYCODE_N, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusManager) {
                when (opus_manager.cursor.ctl_level) {
                    CtlLineLevel.Line -> TODO()
                    CtlLineLevel.Channel -> TODO()
                    CtlLineLevel.Global -> TODO()
                    null -> {
                        if (opus_manager.marked_range != null) {
                            opus_manager.link_beat_range(
                                opus_manager.cursor.get_beatkey(),
                                opus_manager.marked_range!!.first,
                                opus_manager.marked_range!!.second,
                            )
                            opus_manager.marked_range = null
                        }
                    }
                }
            }
        },

        Pair(KeyEvent.KEYCODE_R, true) to object: CursorSpecificKeyStrokeNode(this) {
            private fun _set_relative_mode(opus_manager: OpusManager, force_mode: Int = 1) {
                if (opus_manager.relative_mode == 0) {
                    val activity = opus_manager.get_activity()
                    if (activity != null) {
                        activity.configuration.relative_mode = true
                        activity.save_configuration()
                    }
                }
                if (opus_manager.relative_mode != force_mode) {
                    opus_manager.set_relative_mode(force_mode)
                }
            }

            override fun column(opus_manager: OpusManager) {
                this._set_relative_mode(opus_manager)
                opus_manager.convert_events_in_beat_to_relative(opus_manager.cursor.beat)
            }

            override fun single(opus_manager: OpusManager) {
                if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
                    return
                }
                
                val tree = opus_manager.get_tree()

                if (tree.is_event()) {
                    val event = tree.get_event()
                    if (event is AbsoluteNoteEvent) {
                        opus_manager.convert_event_to_relative(
                            opus_manager.cursor.get_beatkey(),
                            opus_manager.cursor.get_position()
                        )

                        val force_mode = if ((tree.get_event() as RelativeNoteEvent).offset >= 0) {
                            2
                        } else {
                            1
                        }

                        this._set_relative_mode(opus_manager, force_mode)
                    } else if (event is RelativeNoteEvent) {
                        opus_manager.set_event(
                            opus_manager.cursor.get_beatkey(),
                            opus_manager.cursor.get_position(),
                            RelativeNoteEvent(0 - event.offset, event.duration)
                        )
                    }
                } else {
                    this._set_relative_mode(
                        opus_manager,
                        if (opus_manager.relative_mode == 1) {
                            2
                        } else {
                            1
                        }
                    )
                }
            }

            override fun line(opus_manager: OpusManager) {
                this._set_relative_mode(opus_manager)
                opus_manager.convert_events_in_line_to_relative(opus_manager.cursor.channel, opus_manager.cursor.line_offset)
            }
        },

        Pair(KeyEvent.KEYCODE_R, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun column(opus_manager: OpusManager) {
                opus_manager.convert_events_in_beat_to_absolute(opus_manager.cursor.beat)
                opus_manager.set_relative_mode(0)
            }
            override fun single(opus_manager: OpusManager) {
                if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
                    return
                }

                try {
                    opus_manager.convert_event_to_absolute(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
                } catch (e: OpusLayerBase.NoteOutOfRange) {
                    val tree = opus_manager.get_tree()
                    val event = tree.get_event()!! as RelativeNoteEvent
                    opus_manager.set_event_at_cursor(
                        AbsoluteNoteEvent(
                            min((opus_manager.tuning_map.size * 8) - 1, max(0, e.n)),
                            event.duration
                        )
                    )
                }
                opus_manager.set_relative_mode(0)
            }

            override fun line(opus_manager: OpusManager) {
                opus_manager.convert_events_in_line_to_absolute(opus_manager.cursor.channel, opus_manager.cursor.line_offset)
                opus_manager.set_relative_mode(0)
            }
        },

        Pair(KeyEvent.KEYCODE_S, false) to object: CursorSpecificKeyStrokeNode(this) {
            override fun single(opus_manager: OpusLayerInterface) {
                val splits = this.get_buffer_value(2, minimum=2, maximum=64)
                val cursor = opus_manager.cursor
                when (cursor.ctl_level) {
                    null -> {
                        try {
                            opus_manager.split_tree(splits)
                        } catch (e: OpusLayerOverlapControl.BlockedTreeException) {
                            // ignore
                        }
                    }
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
                val repeat = this.get_buffer_value(1)
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

        Pair(KeyEvent.KEYCODE_V, true) to object: CursorSpecificKeyStrokeNode(this) {
            /* All the methods call cursor_select_line, but choose the channel differently */
            override fun line(opus_manager: OpusManager) {
                val channel = opus_manager.cursor.channel
                val default = opus_manager.get_visible_channels()[channel].lines.size - 1
                opus_manager.cursor_select_line(
                    channel,
                    this.get_buffer_value(default, 0, default)
                )
            }
            override fun single(opus_manager: OpusManager) {
                this.line(opus_manager)
            }
            override fun column(opus_manager: OpusManager) {
                val channels = opus_manager.get_visible_channels()
                val channel = channels.size // Last channel that is also visible
                val default = channels[channel].lines.size - 1
                opus_manager.cursor_select_line(
                    channel,
                    this.get_buffer_value(default, 0, default)
                )
            }
            override fun unset(opus_manager: OpusManager) {
                this.column(opus_manager)
            }

            override fun range(opus_manager: OpusManager) {
                val channel = opus_manager.cursor.range!!.first.channel
                val default = opus_manager.get_visible_channels()[channel].lines.size - 1
                opus_manager.cursor_select_line(
                    channel,
                    this.get_buffer_value(default, 0, default)
                )
            }
        },

        Pair(KeyEvent.KEYCODE_X, false) to object: KeyStrokeNode(this) {
            override fun call(opus_manager: OpusManager): Boolean {
                opus_manager.unset()
                return true
            }
        },

        Pair(KeyEvent.KEYCODE_X, true) to object: CursorSpecificKeyStrokeNode(this) {
            override fun column(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1, minimum=0, maximum=opus_manager.beat_count - 1)
                if (repeat > 0) {
                    opus_manager.remove_beat_at_cursor(repeat)
                }
            }

            override fun single(opus_manager: OpusLayerInterface) {
                val repeat = this.get_buffer_value(1)
                if (repeat > 0) {
                    opus_manager.remove_at_cursor(repeat)
                }
            }

            override fun line(opus_manager: OpusLayerInterface) {
                val cursor = opus_manager.cursor
                val line_count = opus_manager.get_visible_channels()[cursor.channel].lines.size
                val repeat = this.get_buffer_value(1, maximum=line_count - 1, minimum=0)
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
        val current_buffer = this.input_buffer_value
        val output = if (this.key_code_map.containsKey(Pair(key_code, event.isShiftPressed))) {
            this.key_code_map[Pair(key_code, event.isShiftPressed)]!!.call(this.opus_manager)
        } else {
            when (event.keyCode) {
                KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4,
                KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                    this.input_buffer_value = ((this.input_buffer_value ?: 0) * 10) + event.keyCode - KeyEvent.KEYCODE_0
                    true
                }
                else -> {
                    false
                }
            }
        }

        // clear the buffer if a function was found and the buffer wasn't modified
        if (output && this.input_buffer_value == current_buffer) {
            this.clear_buffer_value()
        }

        return output
    }

    private fun _cursor_select_next_prev_leaf(direction_up: Boolean = false) {
        val repeat = this.get_buffer_value(1)
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

        val next_line = max(0, min(line_count - 1, current_line + (repeat * direction_mod)))
        val next_ctl_line = this.opus_manager.get_ctl_line_from_row(next_line)
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

    private fun _cursor_select_next_leaf_down() {
        this._cursor_select_next_prev_leaf(false)
    }

    private fun _cursor_select_next_leaf_up() {
        this._cursor_select_next_prev_leaf(true)
    }
}
