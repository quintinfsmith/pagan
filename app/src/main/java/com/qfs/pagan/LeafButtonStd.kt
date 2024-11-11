package com.qfs.pagan

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.AbsoluteNoteEvent
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerCursor
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.opusmanager.OpusVolumeEvent
import com.qfs.pagan.opusmanager.PercussionEvent
import com.qfs.pagan.opusmanager.RelativeNoteEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.roundToInt

class LeafButtonStd(
    context: Context,
    initial_radix: Int,
    private var _event: InstrumentEvent?,
    var position: List<Int>
) : LeafButton(ContextThemeWrapper(context, R.style.leaf)) {

    init {
        this.minimumHeight = resources.getDimension(R.dimen.line_height).roundToInt()
        this.set_text(initial_radix)
    }

    override fun get_tint_list(): IntArray {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map

        return intArrayOf(
            color_map[ColorMap.Palette.LeafInvalid],
            color_map[ColorMap.Palette.LeafInvalidSelected],
            color_map[ColorMap.Palette.SecondarySelectionInvalid],

            color_map[ColorMap.Palette.Leaf],
            color_map[ColorMap.Palette.Spill],

            // Primary
            color_map[ColorMap.Palette.Selection],
            color_map[ColorMap.Palette.LeafSelected],
            color_map[ColorMap.Palette.LeafSelected], // spill

            // Secondary
            color_map[ColorMap.Palette.Selection],
            color_map[ColorMap.Palette.SecondarySelection],
            color_map[ColorMap.Palette.SecondarySelection],

            color_map[ColorMap.Palette.ChannelOdd],
            color_map[ColorMap.Palette.ChannelEven]
        )

    }

    override fun long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = this.get_beat_key()
        val current_move_mode = this.get_activity().configuration.move_mode

        if (cursor.is_selecting_range() && cursor.ctl_level == null && current_move_mode != PaganConfiguration.MoveMode.MERGE) {
            opus_manager.cursor_select_range(opus_manager.cursor.range!!.first, beat_key)
        } else {
            opus_manager.cursor_select_range(beat_key, beat_key)
        }

        return true
    }

    override fun callback_click() {
        val beat_key = this.get_beat_key()
        val position = this.position
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val editor_table = this._get_editor_table() // Will need if overflow exception is passed
        if (cursor.is_selecting_range() && cursor.ctl_level == null) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> {
                        opus_manager.copy_to_beat(beat_key)
                    }
                    PaganConfiguration.MoveMode.MOVE -> {
                        opus_manager.move_to_beat(beat_key)
                    }
                    PaganConfiguration.MoveMode.MERGE -> {
                        opus_manager.merge_into_beat(beat_key)
                    }
                }
                // Kludge, if the temporary blocker is set, assume the cursor has already changed
                if (opus_manager.temporary_blocker == null) {
                    opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                }
            } catch (e: Exception) {
                when (e) {
                    is OpusLayerBase.MixedInstrumentException -> {
                        editor_table.notify_cell_changes(listOf(this.get_coord()))
                        opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                        (this.get_activity()).feedback_msg(context.getString(R.string.feedback_mixed_link))
                    }
                    is OpusLayerBase.RangeOverflow -> {
                        editor_table.notify_cell_changes(listOf(this.get_coord()))
                        opus_manager.cursor_select(beat_key, this.position)
                        this.get_activity().feedback_msg(context.getString(R.string.feedback_bad_range))
                    }
                    is OpusLayerCursor.InvalidCursorState -> {
                        // Shouldn't ever actually be possible
                        throw e
                    }
                    is OpusLayerBase.InvalidMergeException -> {
                        editor_table.notify_cell_changes(listOf(this.get_coord()))
                        opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                    }
                    else -> {
                        throw e
                    }
                }
            }
        } else {
            opus_manager.cursor_select(beat_key, position)
            val tree = opus_manager.get_tree()

            thread {
                if (tree.is_event()) {
                    val note = if (opus_manager.is_percussion(beat_key.channel)) {
                        opus_manager.get_percussion_instrument(beat_key.line_offset)
                    } else {
                        opus_manager.get_absolute_value(beat_key, position) ?: return@thread
                    }
                    if (note >= 0) {
                        (editor_table.context as MainActivity).play_event(
                            beat_key.channel,
                            note,
                            (opus_manager.get_current_line_controller_event(ControlEventType.Volume, beat_key, position) as OpusVolumeEvent).value
                        )
                    }
                }
            }
        }
    }

    private fun set_text(_radix: Int? = null) {
        val event = this._event
        val base_context = (this.context as ContextThemeWrapper).baseContext
        val radix = _radix ?: this.get_opus_manager().tuning_map.size
        this.removeAllViews()

        when (event) {
            is PercussionEvent -> {
                val label_percussion = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value))
                this.addView(label_percussion)
                label_percussion.gravity = Gravity.CENTER
                label_percussion.text = resources.getString(R.string.percussion_label)
            }
            is RelativeNoteEvent -> {
                val sub_wrapper = LinearLayout(base_context)
                val right_wrapper = LinearLayout(base_context)
                right_wrapper.orientation = VERTICAL
                val label_octave = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_octave))
                val label_offset = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_offset))
                val label_prefix = LeafText(ContextThemeWrapper(base_context, R.style.leaf_prefix))

                this.addView(sub_wrapper)
                sub_wrapper.layoutParams.width = WRAP_CONTENT
                sub_wrapper.layoutParams.height = MATCH_PARENT
                (sub_wrapper.layoutParams as LayoutParams).gravity = Gravity.CENTER

                sub_wrapper.addView(right_wrapper)
                right_wrapper.layoutParams.height = MATCH_PARENT
                (right_wrapper.layoutParams as LayoutParams).gravity = Gravity.CENTER_VERTICAL
                right_wrapper.setPadding(0, -3, 0, -7)
                (right_wrapper.layoutParams as LayoutParams).weight = 1F

                right_wrapper.addView(label_prefix)
                label_prefix.layoutParams.height = 0
                (label_prefix.layoutParams as LayoutParams).weight = 1F
                label_prefix.gravity = Gravity.START

                right_wrapper.addView(label_octave)
                label_octave.gravity = Gravity.START
                label_octave.layoutParams.height = 0
                (label_octave.layoutParams as LayoutParams).weight = 1F

                sub_wrapper.addView(label_offset)
                label_offset.layoutParams.height = MATCH_PARENT

                label_prefix.text = if (event.offset < 0) {
                    context.getString(R.string.pfx_subtract)
                } else {
                    context.getString(R.string.pfx_add)
                }
                label_octave.text = "${abs(event.offset) / radix}"
                label_offset.text = "${abs(event.offset) % radix}"
            }

            is AbsoluteNoteEvent -> {
                val sub_wrapper = LinearLayout(base_context)
                val label_octave = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_octave))
                val label_offset = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_offset))

                this.addView(sub_wrapper)
                sub_wrapper.layoutParams.width = WRAP_CONTENT
                sub_wrapper.layoutParams.height = MATCH_PARENT
                (sub_wrapper.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER

                sub_wrapper.addView(label_octave)
                label_octave.gravity = Gravity.BOTTOM
                label_octave.layoutParams.height = MATCH_PARENT

                sub_wrapper.addView(label_offset)
                label_offset.layoutParams.height = MATCH_PARENT

                label_octave.text = "${event.note / radix}"
                label_offset.text = "${event.note % radix}"
            }
            else -> {
                // Do nothing
            }
        }
    }

    override fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        //if (this.parent == null || this._get_editor_table().needs_setup) {
        if (this.parent == null) {
            return drawableState
        }

        val opus_manager = this.get_opus_manager()
        val beat_key = try {
            this.get_beat_key()
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }
        if (beat_key.beat == -1) {
            return drawableState
        }

        val position = this.position
        val tree = try {
            opus_manager.get_tree(beat_key, position)
        } catch (e: OpusTree.InvalidGetCall) {
            return drawableState
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }

        val original_position = try {
            opus_manager.get_actual_position(beat_key, position)
        } catch (e: OpusTree.InvalidGetCall) {
            return drawableState
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }
        val tree_original = try {
            opus_manager.get_tree(original_position.first, original_position.second)
        } catch (e: OpusTree.InvalidGetCall) {
            return drawableState
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }

        val new_state = mutableListOf<Int>()
        if (tree.is_event()) {
            new_state.add(R.attr.state_active)
            val match_cursor = OpusManagerCursor(
                OpusManagerCursor.CursorMode.Single,
                original_position.first.channel,
                original_position.first.line_offset,
                original_position.first.beat
            )

            if (opus_manager.temporary_blocker == match_cursor) {
                new_state.add(R.attr.state_invalid)
            } else {
                when (tree.get_event()) {
                    is RelativeNoteEvent -> {
                        val abs_value = opus_manager.get_absolute_value(beat_key, position)
                        if (abs_value == null || abs_value < 0 || abs_value >= opus_manager.tuning_map.size * 8) {
                            new_state.add(R.attr.state_invalid)
                        }
                    }
                    else -> {}
                }
            }
        } else if (tree_original != tree) {
            when (tree_original.get_event()) {
                is RelativeNoteEvent -> {
                    val abs_value = opus_manager.get_absolute_value(original_position.first, original_position.second)
                    if (abs_value == null || abs_value < 0 || abs_value >= opus_manager.tuning_map.size * 8) {
                        new_state.add(R.attr.state_invalid)
                    }
                }
                else -> {}
            }
            new_state.add(R.attr.state_spill)
        }

        if (opus_manager.is_selected(beat_key, position)) {
            new_state.add(R.attr.state_focused)
        } else if (opus_manager.is_secondary_selection(beat_key, position)) {
            new_state.add(R.attr.state_focused_secondary)
        }

        if (beat_key.channel % 2 == 0) {
            new_state.add(R.attr.state_alternate)
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    fun get_beat_key(): BeatKey {
        val opus_manager = this.get_opus_manager()
        val (pointer, ctl_level, _) = opus_manager.get_ctl_line_info(
            opus_manager.get_ctl_line_from_row((this.parent as CellLayout).row)
        )

        val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
        return BeatKey(channel, line_offset, this.get_beat())
    }

}
