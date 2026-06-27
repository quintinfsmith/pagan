/*
 * Pagan, A Music Sequencer
 * Copyright (C) 2022  Quintin Foster Smith
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * Inquiries can be made to Quintin via email at smith.quintin@protonmail.com
 */
package com.qfs.pagan
import android.view.KeyEvent.KEYCODE_0
import android.view.KeyEvent.KEYCODE_1
import android.view.KeyEvent.KEYCODE_2
import android.view.KeyEvent.KEYCODE_3
import android.view.KeyEvent.KEYCODE_4
import android.view.KeyEvent.KEYCODE_5
import android.view.KeyEvent.KEYCODE_6
import android.view.KeyEvent.KEYCODE_7
import android.view.KeyEvent.KEYCODE_8
import android.view.KeyEvent.KEYCODE_9
import android.widget.Toast
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import com.qfs.pagan.ComponentActivity.ComponentActivityEditor
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.IncompatibleChannelException
import com.qfs.pagan.structure.opusmanager.base.InvalidChannel
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import kotlin.math.max
import kotlin.math.min
import com.qfs.pagan.OpusLayerInterface as OpusManager

class KeyboardInputInterface(var context: ComponentActivityEditor) {
    enum class Context {
        Global,
        Unset,
        Channel,
        Beat,
        LineStandard,
        LinePercussion,
        Line,
        EffectLine,
        EffectChannel,
        EffectGlobal,
        Leaf,
        LeafStandard,
        LeafPercussion,
        LeafLineEffect,
        LeafChannelEffect,
        LeafGlobalEffect,
        Range
    }

    enum class FunctionAlias {
        Undo,
        Redo,
        Delete,
        EscapeContext,
        SelectBeat,
        SelectBeatFromLine,
        SelectLineFromBeat,
        SelectBeatNext,
        SelectBeatPrev,
        SelectLine,
        SelectLineNext,
        SelectLinePrev,
        SelectFirstLineNextChannel,
        SelectFirstLinePrevChannel,
        SelectChannel,
        SelectChannelNext,
        SelectChannelPrev,
        AdjustOctaveUp,
        AdjustOffsetUp,
        AdjustOctaveDown,
        AdjustOffsetDown,

        ChannelRemove,
        ChannelInsert,
        ChannelInsertAfter,
        ChannelKitInsert,
        ChannelKitInsertAfter,
        ChannelDuplicate,
        ChannelMuteToggle,
        ChannelAppendLine,
        ChannelSetSoundfontIndex,
        ChannelSetMidiBank,
        ChannelSetMidiProgram,

        LineMoveDown,
        LineMoveUp,
        LineMoveTo,

        LineSetChannel,
        LineSetPercussionInstrument,
        LineDuplicate,
        LineInsert,
        LineInsertAfter,
        LineRemove,
        LineMuteToggle,
        LineSetVolume,

        LeafUnset,
        LeafAdd,
        LeafAddBefore,
        LeafRemove,
        LeafSplit,
        LeafSetDuration,

        SelectLeafNext,
        SelectLeafPrevious,
        SelectLeafUp,
        SelectLeafDown,

        SetOctave,
        SetOffset,

        TogglePercussion,

        // ------ UI Function ----------//
        ZoomIn,
        ZoomOut,
        ZoomInFull,
        ZoomOutFull
    }

    var input_buffer_value: Int? = null
    val running_buffer: MutableList<KeyboardMap.AliasKey> = mutableListOf()

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

    val cursor_map: HashMap<FunctionAlias, (ComponentActivityEditor, OpusManager) -> Boolean> = hashMapOf(
        FunctionAlias.EscapeContext to { _, opus_manager ->
            if (this.input_buffer_value == null) {
                opus_manager.cursor_clear()
            } else {
                this.input_buffer_value = null
            }

            true
        },
        FunctionAlias.Undo to { _, opus_manager ->
            val count = this.get_buffer_value(1, 0)
            if (count > 0) {
                opus_manager.apply_undo(count)
            }
            true
        },
        FunctionAlias.Redo to { _, opus_manager ->
            val count = this.get_buffer_value(1, 0)
            if (count > 0) {
                opus_manager.apply_redo(count)
            }
            true
        },
        FunctionAlias.SelectBeat to { _, opus_manager ->
            val column = this.get_buffer_value(0, 0, opus_manager.length - 1)
            opus_manager.cursor_select_column(column)
            true
        },
        FunctionAlias.SelectChannel to { _, opus_manager ->
            val visible_channels = opus_manager.get_visible_channels()
            opus_manager.cursor_select_channel(
                this.get_buffer_value(
                    when (opus_manager.cursor.mode) {
                        CursorMode.Line,
                        CursorMode.Single -> opus_manager.cursor.channel
                        else -> visible_channels.size - 1
                    },
                    0,
                    visible_channels.size - 1
                )
            )
            true
        },
        FunctionAlias.SelectChannelNext to { _, opus_manager ->
            opus_manager.cursor_select_next_channel(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.SelectChannelPrev to { _, opus_manager ->
            opus_manager.cursor_select_previous_channel(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.ChannelRemove to { _, opus_manager ->
            opus_manager.remove_selected_channel(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.ChannelInsert to { _, opus_manager ->
            opus_manager.insert_channel_at_cursor(
                this.get_buffer_value(1, 0),
                false
            )
            true
        },
        FunctionAlias.ChannelInsertAfter to { _, opus_manager ->
            opus_manager.insert_channel_after_cursor(
                this.get_buffer_value(1, 0),
                false
            )
            true
        },
        FunctionAlias.ChannelKitInsert to { _, opus_manager ->
            opus_manager.insert_channel_at_cursor(
                this.get_buffer_value(1, 0),
                true
            )
            true
        },
        FunctionAlias.ChannelKitInsertAfter to { _, opus_manager ->
            opus_manager.insert_channel_after_cursor(
                this.get_buffer_value(1, 0),
                true
            )
            true
        },
        FunctionAlias.ChannelDuplicate to { _, opus_manager ->
            opus_manager.duplicate_channel_at_cursor(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.ChannelMuteToggle to { _, opus_manager ->
            opus_manager.toggle_selected_channel_mute()
            true
        },
        FunctionAlias.ChannelAppendLine to { _, opus_manager ->
            opus_manager.selected_channel_new_line(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.ChannelSetSoundfontIndex to { context, opus_manager ->
            opus_manager.channel_set_soundfont_index(
                this.get_buffer_value(0, 0, max(0, context.state_model.active_soundfonts.value.size - 1))
            )
            true
        },
        FunctionAlias.ChannelSetMidiBank to { _, opus_manager ->
            opus_manager.channel_set_midi_bank(
                this.get_buffer_value(0, 0,127)
            )
            true
        },
        FunctionAlias.ChannelSetMidiProgram to { _, opus_manager ->
            opus_manager.channel_set_midi_program(
                this.get_buffer_value(0,0, 127)
            )
            true
        },
        FunctionAlias.SelectLine to { _, opus_manager ->
            val channel = when (opus_manager.cursor.mode) {
                CursorMode.Single,
                CursorMode.Channel,
                CursorMode.Line -> opus_manager.cursor.channel
                else -> 0
            }
            val default_value = opus_manager.get_channel(channel).lines.size - 1
            val line_offset = this.get_buffer_value(0, 0, default_value)
            opus_manager.cursor_select_line(channel, line_offset)

            true
        },
        FunctionAlias.SelectLineNext to { _, opus_manager ->
            opus_manager.cursor_select_next_line_in_channel(
                this.get_buffer_value(1, 0)
            )
            true
        },

        FunctionAlias.SelectLinePrev to { _, opus_manager ->
            opus_manager.cursor_select_prev_line_in_channel(
                this.get_buffer_value(1, 0)
            )
            true
        },

        FunctionAlias.SelectFirstLineNextChannel to { _, opus_manager ->
            opus_manager.cursor_select_first_line_in_next_channel(
                this.get_buffer_value(1, 0)
            )
            true
        },

        FunctionAlias.SelectFirstLinePrevChannel to { _, opus_manager ->
            opus_manager.cursor_select_first_line_in_prev_channel(
                this.get_buffer_value(1, 0)
            )
            true
        },

        FunctionAlias.LeafSetDuration to { _, opus_manager ->
            opus_manager.set_duration_at_cursor(
                this.get_buffer_value(1, 1)
            )
            true
        },

        FunctionAlias.SetOctave to { context, opus_manager ->
            val default_octave = opus_manager.latest_set_octave ?: 0
            opus_manager.set_note_octave_at_cursor(
                this.get_buffer_value(
                    default_octave,
                    0,
                    Values.OctaveCount - 1
                ),
                context.state_model.relative_input_mode.value
            )
            true
        },

        FunctionAlias.SetOffset to { context, opus_manager ->
            val default_offset = opus_manager.latest_set_offset ?: 0
            opus_manager.set_note_offset_at_cursor(
                this.get_buffer_value(
                    default_offset,
                    0,
                    opus_manager.get_radix() - 1
                ),
                context.state_model.relative_input_mode.value
            )
            true
        },
        FunctionAlias.TogglePercussion to { _, opus_manager ->
            opus_manager.toggle_percussion_event_at_cursor()
            true
        },
        FunctionAlias.LineMoveDown to { _, opus_manager ->
            val count = this.get_buffer_value(1, 1)
            opus_manager.move_selected_line_down(count)
            true
        },
        FunctionAlias.LineMoveUp to { _, opus_manager ->
            val count = this.get_buffer_value(1, 1)
            opus_manager.move_selected_line_up(count)
            true
        },
        FunctionAlias.LineMoveTo to { _, opus_manager ->
            val line_offset = this.get_buffer_value(0, 0)
            opus_manager.move_selected_line_to(line_offset)
            true
        },
        FunctionAlias.LineSetChannel to { _, opus_manager ->
            try {
                opus_manager.move_selected_line_to_channel(
                    this.get_buffer_value(
                        0,
                        0
                    )
                )
            } catch (e: InvalidChannel) {
                Toast.makeText(this.context, this.context.getString(R.string.invalid_channel, e.channel), Toast.LENGTH_SHORT).show()
            } catch (e: IncompatibleChannelException) {
                Toast.makeText(this.context, R.string.feedback_mixed_copy, Toast.LENGTH_SHORT).show()
            }
            true
        },
        FunctionAlias.LineSetPercussionInstrument to { _, opus_manager ->
            val instrument = this.get_buffer_value(0, 0, 127)
            opus_manager.set_percussion_instrument(instrument)
            true
        },
        FunctionAlias.LineDuplicate to { _, opus_manager ->
            val count = this.get_buffer_value(
                Values.DialogInput.InsertLine,
                1,
                Values.DialogInput.Max.InsertLine
            )
            opus_manager.duplicate_line_at_cursor(count)
            true
        },
        FunctionAlias.AdjustOffsetUp to { _, opus_manager ->
            val count = this.get_buffer_value(
                1,
                1,
                opus_manager.get_radix() * Values.OctaveCount
            )
            opus_manager.offset_selection(count)
            true
        },
        FunctionAlias.AdjustOffsetDown to { _, opus_manager ->
            val count = this.get_buffer_value(
                1,
                1,
                opus_manager.get_radix() * Values.OctaveCount
            )
            opus_manager.offset_selection(-1 * count)
            true
        },
        FunctionAlias.AdjustOctaveUp to { _, opus_manager ->
            val count = this.get_buffer_value(
                1, 1, Values.OctaveCount
            )
            opus_manager.offset_selection(count * opus_manager.get_radix())
            true
        },
        FunctionAlias.AdjustOctaveDown to { _, opus_manager ->
            val count = this.get_buffer_value(
                1, 1, Values.OctaveCount
            )
            opus_manager.offset_selection(-1 * count * opus_manager.get_radix())
            true
        },
        FunctionAlias.LineInsert to { _, opus_manager ->
            val count = this.get_buffer_value(
                Values.DialogInput.InsertLine,
                1,
                Values.DialogInput.Max.InsertLine
            )
            opus_manager.insert_line_at_cursor(count)
            true
        },

        FunctionAlias.LineInsertAfter to { _, opus_manager ->
            val count = this.get_buffer_value(
                Values.DialogInput.InsertLine,
                1,
                Values.DialogInput.Max.InsertLine
            )
            opus_manager.insert_line_after_cursor(count)
            true
        },

        FunctionAlias.LineRemove to { _, opus_manager ->
            val count = this.get_buffer_value(
                Values.DialogInput.RemoveLine,
                1,
            )
            opus_manager.remove_line_at_cursor(count)
            true
        },

        FunctionAlias.LineSetVolume to { _, opus_manager ->
            opus_manager.set_selected_line_volume(
                this.get_buffer_value(
                    100,
                    0
                ).toFloat() / 100F
            )
            true
        },
        FunctionAlias.LineMuteToggle to { _, opus_manager ->
            opus_manager.toggle_selected_line_mute()
            true
        },

        FunctionAlias.LeafSplit to { _, opus_manager ->
            opus_manager.split_tree_at_cursor(
                this.get_buffer_value(
                    Values.DialogInput.Split,
                    1,
                    Values.DialogInput.Max.Split
                )
            )
            true
        },
        FunctionAlias.LeafUnset to  { _, opus_manager ->
            opus_manager.unset()
            true
        },
        FunctionAlias.LeafRemove to { _, opus_manager ->
            opus_manager.remove_at_cursor(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.LeafAdd to { _, opus_manager ->
            opus_manager.insert_after_cursor(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.LeafAddBefore to { _, opus_manager ->
            opus_manager.insert_at_cursor(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.ZoomIn to { context, _ ->
            context.state_model.increment_zoom()
            context.state_model.recenter()
            true
        },
        FunctionAlias.ZoomOut to { context, _ ->
            context.state_model.decrement_zoom()
            context.state_model.recenter()
            true
        },
        FunctionAlias.ZoomInFull to { context, _ ->
            val state_model = context.state_model
            state_model.queued_zoom_index.intValue = state_model.max_zoom_index.intValue
            state_model.recenter()
            true
        },
        FunctionAlias.ZoomOutFull to { context, _ ->
            val state_model = context.state_model
            state_model.queued_zoom_index.intValue = 0
            state_model.recenter()
            true
        },
        FunctionAlias.SelectLeafNext to { _, opus_manager ->
            opus_manager.select_next_leaf(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.SelectLeafPrevious to { _, opus_manager ->
            opus_manager.select_previous_leaf(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.SelectLeafUp to { _, opus_manager ->
            opus_manager.select_leaf_up(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.SelectLeafDown to { _, opus_manager ->
            opus_manager.select_leaf_down(
                this.get_buffer_value(1, 0)
            )
            true
        },
        FunctionAlias.SelectBeatFromLine to { _, opus_manager ->
            opus_manager.select_first_in_beat(
                this.get_buffer_value(0, 0, opus_manager.length - 1)
            )
            true
        },
        FunctionAlias.SelectLineFromBeat to { _, opus_manager ->
            val beat = opus_manager.cursor.beat
            opus_manager.cursor_select_first_line_in_channel(
                this.get_buffer_value(0, 0, opus_manager.channels.size - 1)
            )
            opus_manager.select_first_in_beat(beat)
            true
        }
        //SelectLine to { context, opus_manager ->
        //    this.go_to_first_line_in_channel(context, opus_manager)
        //},
        //Context.Beat to hashMapOf(
        //    Triple(KEYCODE_L, false, false) to { context, opus_manager ->
        //        val visible_channels = opus_manager.get_visible_channels()
        //        val channel = this.get_buffer_value(visible_channels.size - 1, 0, visible_channels.size - 1)
        //        val beat_key = BeatKey(channel, 0, opus_manager.cursor.beat)
        //        context.cursor_select(beat_key, opus_manager.get_first_position(beat_key))

        //        true
        //    }
        //),
        //Context.LineStandard to hashMapOf(
        //    //Triple(KEYCODE_L, false, false) to { context, opus_manager ->
        //    //    val beat = this.get_buffer_value(opus_manager.length - 1, 0, opus_manager.length - 1)
        //    //    val beat_key = BeatKey(opus_manager.cursor.channel, opus_manager.cursor.line_offset, beat)
        //    //    context.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
        //    //    true
        //    //}
        //),
        //Context.Range to hashMapOf()
    )

    fun input(event: KeyEvent): Boolean {
        val key_code = event.nativeKeyEvent.keyCode
        val current_buffer = this.input_buffer_value
        val opus_manager = this.context.controller_model.opus_manager

        val cursor_mode = opus_manager.cursor.mode

        val input_contexts: Array<Context> = when (cursor_mode) {
            CursorMode.Line -> {
                when (opus_manager.cursor.ctl_level) {
                    CtlLineLevel.Line -> arrayOf(Context.EffectLine)
                    CtlLineLevel.Channel -> arrayOf(Context.EffectChannel)
                    CtlLineLevel.Global -> arrayOf(Context.EffectGlobal)
                    null -> {
                        arrayOf(
                            if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
                                Context.LinePercussion
                            } else {
                                Context.LineStandard
                            },
                            Context.Line
                        )
                    }
                }
            }
            CursorMode.Single -> {
                arrayOf(
                    when (opus_manager.cursor.ctl_level) {
                        CtlLineLevel.Line -> Context.LeafLineEffect
                        CtlLineLevel.Channel -> Context.LeafChannelEffect
                        CtlLineLevel.Global -> Context.LeafGlobalEffect
                        null -> if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
                            Context.LeafPercussion
                        } else {
                            Context.LeafStandard
                        }
                    },
                    Context.Leaf
                )
            }
            CursorMode.Beat -> arrayOf(Context.Beat)
            CursorMode.Channel -> arrayOf(Context.Channel)
            CursorMode.Unset -> arrayOf(Context.Unset)
            CursorMode.Range -> TODO()
        }

        var output: Boolean? = null
        var clear_running_buffer = true
        val key_node = KeyboardMap.AliasKey(key_code, event.isShiftPressed, event.isCtrlPressed)
        this.running_buffer.add(key_node)
        for (input_context in input_contexts + arrayOf(Context.Global)) {
            KeyboardMap[input_context, this.running_buffer].let { (keep_alive, alias) ->
                alias?.let {
                    this.cursor_map[alias]?.let {
                        output = it(this.context, opus_manager)
                        break
                    }
                }

                if (keep_alive) {
                    clear_running_buffer = false
                    break
                }
            }
        }


        if (clear_running_buffer) {
            this.running_buffer.clear()
        }

        output = output ?: if (this.running_buffer.isEmpty()) {
            // If no function was assigned, check for buffer input, otherwise return false
            when (key_code) {
                KEYCODE_0, KEYCODE_1, KEYCODE_2, KEYCODE_3, KEYCODE_4,
                KEYCODE_5, KEYCODE_6, KEYCODE_7, KEYCODE_8, KEYCODE_9 -> {
                    this.input_buffer_value = ((this.input_buffer_value ?: 0) * 10) + key_code - KEYCODE_0
                    true
                }
                else -> false
            }
        } else {
            false
        }

        // clear the buffer if a function was found and the buffer wasn't modified
        if (output && this.input_buffer_value == current_buffer) {
            this.clear_buffer_value()
        }

        //return output
        return true
    }

//    val key_code_map = hashMapOf(
//        Pair(KEYCODE_LEFT_BRACKET, false) to object: CursorSpecificKeyStrokeNode(this) {
//            override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//
//                if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
//                    opus_manager.set_percussion_event_at_cursor()
//                } else {
//                   // opus_manager.set_note_octave_at_cursor(
//                   //     this.get_buffer_value(0, 0, 7) // )
//                }
//            }
//        },
//
//        Pair(KEYCODE_RIGHT_BRACKET, false) to object: CursorSpecificKeyStrokeNode(this) {
//            override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                // opus_manager.set_note_offset_at_cursor(
//                //     this.get_buffer_value(0, 0, opus_manager.tuning_map.size - 1)
//                // )
//            }
//        },
//
//        Pair(KEYCODE_A, false) to object: CursorSpecificKeyStrokeNode(this) {
//            override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                val repeat = this.get_buffer_value(1, maximum=9999)
//                opus_manager.insert_line_at_cursor(repeat)
//            }
//            override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                val repeat = this.get_buffer_value(1, maximum=9999)
//                opus_manager.insert_beat_after_cursor(repeat)
//            }
//
//            override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                var repeat = this.get_buffer_value(1, maximum=64)
//                if (repeat > 0) {
//                    val tree = opus_manager.get_tree() ?: return
//                    if (tree.parent == null) {
//                        try {
//                            opus_manager.split_tree_at_cursor(repeat + 1)
//                        } catch (e: BlockedTreeException) {
//                            // pass
//                        }
//                    } else {
//                        opus_manager.insert_after_cursor(repeat)
//                    }
//                }
//            }
//        },
//
//        Pair(KEYCODE_B, false) to object: CursorSpecificKeyStrokeNode(this) {
//            override fun unset(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                this.column(context, opus_manager, ctrl_pressed)
//            }
//
//            override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                val beat = this.get_buffer_value(0, 0, opus_manager.length - 1)
//                opus_manager.cursor_select_column(beat)
//            }
//
//            override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                this.single(context,opus_manager, ctrl_pressed)
//            }
//
//            override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                val beat = this.get_buffer_value(0, 0, opus_manager.length - 1)
//                val new_beat_key = BeatKey(
//                    opus_manager.cursor.channel,
//                    opus_manager.cursor.line_offset,
//                    beat
//                )
//                val new_position = opus_manager.get_first_position(new_beat_key)
//                opus_manager.cursor_select(new_beat_key, new_position)
//            }
//
//            override fun range(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                val beat = this.get_buffer_value(0, 0, opus_manager.length - 1)
//                val new_beat_key = opus_manager.cursor.range!!.second
//                new_beat_key.beat = beat
//
//                opus_manager.cursor_select_range(
//                    opus_manager.cursor.range!!.first,
//                    new_beat_key
//                )
//            }
//        },
//
//        Pair(KEYCODE_B, true) to object: KeyStrokeNode(this) {
//            override fun call(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean): Boolean {
//                val default = if (opus_manager.cursor.mode == CursorMode.Single) {
//                    opus_manager.cursor.beat
//                } else {
//                    opus_manager.length - 1
//                }
//                val beat = this.get_buffer_value(default, 0, opus_manager.length - 1)
//
//                opus_manager.cursor_select_column(beat)
//                return true
//            }
//        },
//
//        Pair(KEYCODE_C, false) to object: CursorSpecificKeyStrokeNode(this) {
//            override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                when (opus_manager.cursor.ctl_level) {
//                    CtlLineLevel.Line -> TODO()
//                    CtlLineLevel.Channel -> TODO()
//                    CtlLineLevel.Global -> TODO()
//                    null -> {
//                        if (opus_manager.cursor.range != null) {
//                            opus_manager.overwrite_beat_range(
//                                opus_manager.cursor.get_beatkey(),
//                                opus_manager.cursor.range!!.first,
//                                opus_manager.cursor.range!!.second,
//                            )
//                            opus_manager.cursor.range = null
//                        }
//                    }
//                }
//            }
//        },
//        Pair(KEYCODE_D, true) to object: CursorSpecificKeyStrokeNode(this) {
//            override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//                val beatkey = opus_manager.cursor.get_beatkey()
//                opus_manager.cursor_select_range(beatkey, beatkey)
//            }
//        },
//
//        //Pair(KEYCODE_E, true) to object: CursorSpecificKeyStrokeNode(this) {
//        //    override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
//        //        val ctl_type_map = EffectType.entries.associateBy { it.i }
//        //        val ctl_type = ctl_type_map [this.get_buffer_value(EffectType.Volume.i)] ?: return
//        //        for ((check_type, _) in OpusLayerInterface.line_controller_domain) {
//        //            if (check_type == ctl_type) {
//        //                val channel = opus_manager.cursor.channel
//        //                val line_offset = opus_manager.cursor.line_offset
//        //                opus_manager.toggle_line_controller_visibility(ctl_type, channel, line_offset)
//        //                break
//        //            }
//        //        }
//
//        //    }
//
//        //    override fun channel(context: ComponentActivityEditor, ctrl_pressed: Boolean) {
//        //        val ctl_type_map = EffectType.entries.associateBy { it.i }
//        //        val ctl_type = ctl_type_map [this.get_buffer_value(EffectType.Volume.i)] ?: return
//        //        for ((check_type, _) in OpusLayerInterface.channel_controller_domain) {
//        //            if (check_type == ctl_type) {
//        //                val channel = opus_manager.cursor.channel
//        //                opus_manager.toggle_channel_controller_visibility(ctl_type, channel)
//        //                break
//        //            }
//        //        }
//        //    }
//        //},
//
////         Pair(KEYCODE_H, false) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val movement_value = this.get_buffer_value(1, minimum=0)
////                 if (movement_value != 0) {
////                     val beat_key = BeatKey(
////                         opus_manager.cursor.channel,
////                         opus_manager.cursor.line_offset,
////                         max(0, opus_manager.cursor.beat - movement_value)
////                     )
////
////                     opus_manager.cursor_select(
////                         beat_key,
////                         opus_manager.get_first_position(beat_key)
////                     )
////                 }
////             }
////
////             override fun range(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 opus_manager.cursor_select_range(
////                     opus_manager.cursor.range!!.first,
////                     BeatKey(
////                         opus_manager.cursor.range!!.second.channel,
////                         opus_manager.cursor.range!!.second.line_offset,
////                         max(0, opus_manager.cursor.range!!.second.beat - this.get_buffer_value(1, minimum=0))
////                     )
////                 )
////             }
////
////             override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val movement_value = this.get_buffer_value(1, minimum=0)
////                 val new_beat = max(0, opus_manager.cursor.beat - movement_value)
////                 opus_manager.cursor_select_column(new_beat)
////             }
////
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 opus_manager.select_previous_leaf(this.get_buffer_value(1, minimum=0))
////             }
////         },
////
////         Pair(KEYCODE_H, true) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val movement_value = this.get_buffer_value(1)
////                 opus_manager.select_first_leaf_in_previous_beat(movement_value)
////             }
////         },
////
////         Pair(KEYCODE_I, false) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1, maximum=9999, minimum=0)
////                 opus_manager.new_line_repeat(opus_manager.cursor.channel, opus_manager.cursor.line_offset, repeat)
////             }
////
////             override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1, maximum=9999)
////                 opus_manager.insert_beat_at_cursor(repeat)
////             }
////
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1, maximum=64, minimum=0)
////                 if (repeat > 0) {
////                     val tree = opus_manager.get_tree() ?: return
////                     if (tree.parent == null) {
////                         try {
////                             opus_manager.split_tree_at_cursor(repeat + 1, true)
////                         } catch (e: BlockedTreeException) {
////                             // pass
////                         }
////                     } else {
////                         opus_manager.insert_at_cursor(repeat)
////                     }
////                 }
////             }
////         },
////
////         //Pair(KEYCODE_J, false) to object: CursorSpecificKeyStrokeNode(this) {
////         //    override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //        val movement_value = this.get_buffer_value(1, minimum=0)
////         //        opus_manager.move_to_next_visible_line(movement_value)
////         //    }
////
////         //    override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //        this.keyboard_interface._cursor_select_next_leaf_down()
////         //    }
////         //},
////
////         Pair(KEYCODE_J, true) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1, minimum=0)
////                 val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
////                 val new_key = BeatKey(new_channel, 0, opus_manager.cursor.beat)
////
////                 opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
////             }
////             override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1, minimum=0)
////                 val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
////                 opus_manager.cursor_select_line(new_channel, 0)
////             }
////
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1, minimum=0)
////                 val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
////
////                 val new_key = opus_manager.cursor.get_beatkey()
////                 new_key.line_offset = 0
////                 new_key.channel = new_channel
////
////                 opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
////             }
////         },
////
////         // Pair(KEYCODE_K, false) to object: CursorSpecificKeyStrokeNode(this) {
////         //     override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         val movement_value = this.get_buffer_value(1, minimum=0)
////         //         opus_manager.move_to_previous_visible_line(movement_value)
////
////         //     }
////
////         //     override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         this.keyboard_interface._cursor_select_next_leaf_up()
////         //     }
////
////         //     override fun range(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         val new_beat_key = opus_manager.cursor.range!!.second
////         //         val movement_value = this.get_buffer_value(1, maximum=new_beat_key.beat, minimum=0)
////         //         val visible_line = (movement_value % (opus_manager.get_row_count() - 1)) - 1
////         //         val (channel, line_offset) = opus_manager.get_channel_and_line_offset(
////         //             opus_manager.get_ctl_line_from_row(visible_line)
////         //         )
////
////         //         opus_manager.cursor_select_range(
////         //             opus_manager.cursor.range!!.first,
////         //             new_beat_key
////         //         )
////         //     }
////         // },
////
////         Pair(KEYCODE_K, true) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1) * -1
////                 val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
////                 opus_manager.cursor_select_line(new_channel, 0)
////             }
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1) * -1
////                 val new_channel = opus_manager.get_nth_next_channel_at_cursor(repeat) ?: return
////
////                 val new_key = opus_manager.cursor.get_beatkey()
////                 new_key.line_offset = 0
////                 new_key.channel = new_channel
////
////                 opus_manager.cursor_select(new_key, opus_manager.get_first_position(new_key))
////             }
////         },
////
////         Pair(KEYCODE_L, false) to object: CursorSpecificKeyStrokeNode(this) {
////
////             override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val movement_value = this.get_buffer_value(1, minimum=0)
////                 val new_beat = min(opus_manager.length - 1, opus_manager.cursor.beat + movement_value)
////                 opus_manager.cursor_select_column(new_beat)
////             }
////
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 opus_manager.select_next_leaf(this.get_buffer_value(1, minimum=0))
////             }
////
////             override fun range(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val movement_value = this.get_buffer_value(1, minimum=0)
////
////                 opus_manager.cursor_select_range(
////                     opus_manager.cursor.range!!.first,
////                     BeatKey(
////                         opus_manager.cursor.range!!.second.channel,
////                         opus_manager.cursor.range!!.second.line_offset,
////                         min(opus_manager.length - 1, opus_manager.cursor.range!!.second.beat + movement_value)
////                     )
////                 )
////             }
////         },
////
////         Pair(KEYCODE_L, true) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val movement_value = this.get_buffer_value(1, minimum=0)
////                 if (movement_value != 0) {
////                     val beat_key = BeatKey(
////                         opus_manager.cursor.channel,
////                         opus_manager.cursor.line_offset,
////                         min(movement_value - 1, opus_manager.length - 1)
////                     )
////
////                     opus_manager.cursor_select(
////                         beat_key,
////                         opus_manager.get_first_position(beat_key)
////                     )
////                 }
////             }
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val movement_value = this.get_buffer_value(1, minimum=0)
////                 val cursor = opus_manager.cursor
////                 var beat = min(opus_manager.length - 1, cursor.beat + movement_value)
////                 opus_manager.select_first_in_beat(beat)
////             }
////         },
////
////         Pair(KEYCODE_M, false) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 when (opus_manager.cursor.ctl_level) {
////                     CtlLineLevel.Line -> TODO()
////                     CtlLineLevel.Channel -> TODO()
////                     CtlLineLevel.Global -> TODO()
////                     null -> {
////                         if (opus_manager.cursor.range != null) {
////                             opus_manager.move_beat_range(
////                                 opus_manager.cursor.get_beatkey(),
////                                 opus_manager.cursor.range!!.first,
////                                 opus_manager.cursor.range!!.second,
////                             )
////                             opus_manager.cursor.range = null
////                         }
////                     }
////                 }
////             }
////
////             override fun range(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 when (opus_manager.cursor.ctl_level) {
////                     CtlLineLevel.Line -> TODO()
////                     CtlLineLevel.Channel -> TODO()
////                     CtlLineLevel.Global -> TODO()
////                     null -> {
////                         val range = opus_manager.cursor.range
////                         opus_manager.cursor.range = range
////                         val beat_key = opus_manager.cursor.range!!.second
////                         val position = opus_manager.get_first_position(beat_key)
////                         opus_manager.cursor_select(beat_key, position)
////                     }
////                 }
////             }
////         },
////
////         Pair(KEYCODE_R, true) to object: CursorSpecificKeyStrokeNode(this) {
////            //  private fun _set_relative_mode(context: ComponentActivityEditor, opus_manager: OpusManager, force_mode: RelativeInputMode = RelativeInputMode.Absolute) {
////            //     if (opus_manager.relative_mode == RelativeInputMode.Absolute) {
////            //         //val activity = opus_manager.get_activity()
////            //         //if (activity != null) {
////            //         //    activity.configuration.relative_mode = true
////            //         //    activity.save_configuration()
////            //         //}
////            //     }
////
////            //     if (opus_manager.relative_mode != force_mode) {
////            //         opus_manager.force_relative_mode(force_mode)
////            //     }
////            //  }
////
////            //  override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////            //      this._set_relative_mode(opus_manager)
////            //      opus_manager.convert_events_in_beat_to_relative(opus_manager.cursor.beat)
////            //  }
////
////            //  override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////            //      if (opus_manager.is_percussion(opus_manager.cursor.channel)) return
////            //
////            //      val tree = opus_manager.get_tree() ?: return
////
////            //      if (tree.has_event()) {
////            //          val event = tree.get_event()
////            //          if (event is AbsoluteNoteEvent) {
////            //              opus_manager.convert_event_to_relative(
////            //                  opus_manager.cursor.get_beatkey(),
////            //                  opus_manager.cursor.get_position()
////            //              )
////
////            //              val force_mode = if ((tree.get_event() as RelativeNoteEvent).offset >= 0) {
////            //                  RelativeInputMode.Negative
////            //              } else {
////            //                  RelativeInputMode.Positive
////            //              }
////
////            //              this._set_relative_mode(opus_manager, force_mode)
////            //          } else if (event is RelativeNoteEvent) {
////            //              opus_manager.set_event(
////            //                  opus_manager.cursor.get_beatkey(),
////            //                  opus_manager.cursor.get_position(),
////            //                  RelativeNoteEvent(0 - event.offset, event.duration)
////            //              )
////            //          }
////            //      } else {
////            //          //this._set_relative_mode(
////            //          //    opus_manager,
////            //          //    if (opus_manager.relative_mode == RelativeInputMode.Positive) {
////            //          //        RelativeInputMode.Negative
////            //          //    } else {
////            //          //        RelativeInputMode.Positive
////            //          //    }
////            //          //)
////            //      }
////            //  }
////
////            //  override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////            //      this._set_relative_mode(opus_manager)
////            //      opus_manager.convert_events_in_line_to_relative(opus_manager.cursor.channel, opus_manager.cursor.line_offset)
////            //  }
////         },
////
////         Pair(KEYCODE_R, false) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 opus_manager.convert_events_in_beat_to_absolute(opus_manager.cursor.beat)
////             }
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 if (opus_manager.is_percussion(opus_manager.cursor.channel)) {
////                     return
////                 }
////
////                 try {
////                     opus_manager.convert_event_to_absolute(opus_manager.cursor.get_beatkey(), opus_manager.cursor.get_position())
////                 } catch (e: NoteOutOfRange) {
////                     val tree = opus_manager.get_tree() ?: return
////                     val event = tree.get_event()!! as RelativeNoteEvent
////                     opus_manager.set_event_at_cursor(
////                         AbsoluteNoteEvent(
////                             min((opus_manager.tuning_map.size * 8) - 1, max(0, e.n)),
////                             event.duration
////                         )
////                     )
////                 }
////             }
////
////             override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 opus_manager.convert_events_in_line_to_absolute(opus_manager.cursor.channel, opus_manager.cursor.line_offset)
////             }
////         },
////
////         Pair(KEYCODE_S, false) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val splits = this.get_buffer_value(2, minimum=2, maximum=64)
////                 val cursor = opus_manager.cursor
////                 when (cursor.ctl_level) {
////                     null -> {
////                         try {
////                             opus_manager.split_tree_at_cursor(splits)
////                         } catch (e: BlockedTreeException) {
////                             // ignore
////                         }
////                     }
////                     CtlLineLevel.Line -> TODO()
////                     CtlLineLevel.Channel -> TODO()
////                     CtlLineLevel.Global -> {
////                         opus_manager.controller_global_split_tree(
////                             cursor.ctl_type!!,
////                             cursor.beat,
////                             cursor.get_position(),
////                             splits
////                         )
////                     }
////                 }
////             }
////         },
////
////         Pair(KEYCODE_U, false) to object: KeyStrokeNode(this) {
////             fun apply_undo(context: ComponentActivityEditor) {
////                 val repeat = this.get_buffer_value(1)
////                 context.apply_undo(repeat)
////             }
////
////             override fun call(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean): Boolean {
////                 this.apply_undo(context)
////                 return true
////             }
////         },
////
////         // Pair(KEYCODE_V, true) to object: CursorSpecificKeyStrokeNode(this) {
////         //     /* All the methods call cursor_select_line, but choose the channel differently */
////         //     override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         val channel = opus_manager.cursor.channel
////         //         val default = opus_manager.get_visible_channels()[channel].lines.size - 1
////         //         opus_manager.cursor_select_line(
////         //             channel,
////         //             this.get_buffer_value(default, 0, default)
////         //         )
////         //     }
////         //     override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         this.line(opus_manager, ctrl_pressed)
////         //     }
////         //     override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         val channels = opus_manager.get_visible_channels()
////         //         val channel = channels.size // Last channel that is also visible
////         //         val default = channels[channel].lines.size - 1
////         //         opus_manager.cursor_select_line(
////         //             channel,
////         //             this.get_buffer_value(default, 0, default)
////         //         )
////         //     }
////         //     override fun unset(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         this.column(opus_manager, ctrl_pressed)
////         //     }
////
////         //     override fun range(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////         //         val channel = opus_manager.cursor.range!!.first.channel
////         //         val default = opus_manager.get_visible_channels()[channel].lines.size - 1
////         //         opus_manager.cursor_select_line(
////         //             channel,
////         //             this.get_buffer_value(default, 0, default)
////         //         )
////         //     }
////         // },
////
////         Pair(KEYCODE_W, true) to object: KeyStrokeNode(this) {
////             override fun call(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean): Boolean {
////                 //opus_manager.get_activity()?.project_save()
////                 return true
////             }
////         },
////
////         Pair(KEYCODE_X, false) to object: KeyStrokeNode(this) {
////             override fun call(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean): Boolean {
////                 opus_manager.unset()
////                 return true
////             }
////         },
////
////         Pair(KEYCODE_X, true) to object: CursorSpecificKeyStrokeNode(this) {
////             override fun column(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1, minimum=0, maximum=opus_manager.length - 1)
////                 if (repeat > 0) {
////                     opus_manager.remove_beat_at_cursor(repeat)
////                 }
////             }
////
////             override fun single(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val repeat = this.get_buffer_value(1)
////                 if (repeat > 0) {
////                     opus_manager.remove_at_cursor(repeat)
////                 }
////             }
////
////             override fun line(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 val cursor = opus_manager.cursor
////                 val line_count = opus_manager.get_visible_channels()[cursor.channel].lines.size
////                 val repeat = this.get_buffer_value(1, maximum=line_count - 1, minimum=0)
////                 if (repeat > 0) {
////                     opus_manager.remove_line_at_cursor(repeat)
////                 }
////             }
////
////             override fun range(context: ComponentActivityEditor, opus_manager: OpusManager, ctrl_pressed: Boolean) {
////                 opus_manager.unset()
////             }
////         }
////     )
////
//
//    // private fun _cursor_select_next_prev_leaf(direction_up: Boolean = false) {
//    //     val repeat = this.get_buffer_value(1)
//    //     val cursor = this.opus_manager.cursor
//    //     val current_line = when (cursor.ctl_level) {
//    //         null -> {
//    //             this.opus_manager.get_visible_row_from_ctl_line(
//    //                 this.opus_manager.get_actual_line_index(
//    //                     this.opus_manager.get_instrument_line_index(
//    //                         cursor.channel,
//    //                         cursor.line_offset
//    //                     )
//    //                 )
//    //             )!!
//    //         }
//    //         CtlLineLevel.Global -> this.opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
//    //         CtlLineLevel.Line -> this.opus_manager.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.channel, cursor.line_offset)
//    //         CtlLineLevel.Channel -> this.opus_manager.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.channel)
//    //     }
//
//    //     val line_count = this.opus_manager.get_row_count()
//    //     val direction_mod = if (direction_up) {
//    //         -1
//    //     } else {
//    //         1
//    //     }
//
//    //     val next_line = max(0, min(line_count - 1, current_line + (repeat * direction_mod)))
//    //     val next_ctl_line = this.opus_manager.get_ctl_line_from_row(next_line)
//    //     val (target, ctl_level, ctl_type) = this.opus_manager.get_ctl_line_info(next_ctl_line)
//    //     when (ctl_level) {
//    //         CtlLineLevel.Global -> {
//    //             val beat = this.opus_manager.cursor.beat
//    //             val target_position = this.opus_manager.get_first_position_global_ctl(ctl_type!!, beat)
//    //             this.opus_manager.cursor_select_ctl_at_global(ctl_type, beat, target_position)
//    //         }
//    //         null -> {
//    //             val (channel, offset) = this.opus_manager.get_channel_and_line_offset(target)
//    //             val target_beat_key = BeatKey(channel, offset, this.opus_manager.cursor.beat)
//    //             val target_position = this.opus_manager.get_first_position(target_beat_key, listOf())
//    //             this.opus_manager.cursor_select(target_beat_key, target_position)
//    //         }
//    //         CtlLineLevel.Line -> TODO()
//    //         CtlLineLevel.Channel -> TODO()
//    //     }
//    // }
//
}
