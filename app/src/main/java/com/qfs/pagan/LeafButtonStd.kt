package com.qfs.pagan

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusEventSTD
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.structure.OpusTree
import kotlin.concurrent.thread
import kotlin.math.abs

class LeafButtonStd(
    context: Context,
    initial_radix: Int,
    private var _event: OpusEventSTD?,
    var position: List<Int>,
    is_percussion: Boolean
) : LeafButton(context) {

    init {
        this.minimumHeight = resources.getDimension(R.dimen.line_height).toInt()
        this.set_text(is_percussion, initial_radix)
    }

    override fun long_click(): Boolean {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor
        val beat_key = this._get_beat_key()
        if (cursor.is_linking_range()) {
            opus_manager.cursor_select_range_to_link(
                opus_manager.cursor.range!!.first,
                beat_key
            )
        } else if (cursor.is_linking) {
            opus_manager.cursor_select_range_to_link(
                opus_manager.cursor.get_beatkey(),
                beat_key
            )
        } else {
            opus_manager.cursor_select_to_link(beat_key)
        }
        return true
    }

    override fun callback_click() {
        val beat_key = this._get_beat_key()
        val position = this.position
        val opus_manager = this.get_opus_manager()

        val editor_table = this._get_editor_table() // Will need if overflow exception is passed
        if (opus_manager.cursor.is_linking) {
            try {
                when (this.get_activity().configuration.link_mode) {
                    PaganConfiguration.LinkMode.LINK -> {
                        opus_manager.link_beat(beat_key)
                    }
                    PaganConfiguration.LinkMode.COPY -> {
                        opus_manager.copy_to_beat(beat_key)
                    }
                    PaganConfiguration.LinkMode.MOVE -> {
                        opus_manager.move_to_beat(beat_key)
                    }
                }
                opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
            } catch (e: Exception) {
                when (e) {
                    is OpusLayerLinks.SelfLinkError -> { }
                    is OpusLayerLinks.MixedLinkException -> {
                        editor_table.notify_cell_changes(listOf(this.get_coord()))
                        (this.get_activity()).feedback_msg(context.getString(R.string.feedback_mixed_link))
                    }
                    is OpusLayerLinks.LinkRangeOverlap,
                    is OpusLayerBase.RangeOverflow,
                    is OpusLayerLinks.LinkRangeOverflow -> {
                        editor_table.notify_cell_changes(listOf(this.get_coord()))
                        opus_manager.cursor.is_linking = false
                        opus_manager.cursor_select(beat_key, this.position)
                        this.get_activity().feedback_msg(context.getString(R.string.feedback_bad_range))
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
                    val abs_value = opus_manager.get_absolute_value(beat_key, position)
                    if ((abs_value != null) && abs_value >= 0) {
                        (editor_table.context as MainActivity).play_event(
                            beat_key.channel,
                            if (opus_manager.is_percussion(beat_key.channel)) {
                                opus_manager.get_percussion_instrument(beat_key.line_offset)
                            } else {
                                opus_manager.get_absolute_value(beat_key, position) ?: return@thread
                            },
                            opus_manager.get_line_volume(beat_key.channel, beat_key.line_offset)
                        )
                    }
                }
            }
        }
    }

    override fun is_selected(): Boolean {
        TODO("Not yet implemented")
    }

    private fun set_text(is_percussion: Boolean, _radix: Int? = null) {
        val event = this._event
        val base_context = (this.context as ContextThemeWrapper).baseContext
        val radix = _radix ?: this.get_opus_manager().tuning_map.size
        this.removeAllViews()

        if (event == null) {
            return
        }

        if (is_percussion) {
            val label_percussion = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value))
            this.addView(label_percussion)
            label_percussion.gravity = Gravity.CENTER
            label_percussion.text = resources.getString(R.string.percussion_label)
        } else if (event.relative) {
            val sub_wrapper = LinearLayout(base_context)
            val right_wrapper = LinearLayout(base_context)
            right_wrapper.orientation = VERTICAL
            val label_octave = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_octave))
            val label_offset = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_offset))
            val label_prefix = LeafText(ContextThemeWrapper(base_context, R.style.leaf_prefix))

            this.addView(sub_wrapper)
            sub_wrapper.layoutParams.width = WRAP_CONTENT
            sub_wrapper.layoutParams.height = MATCH_PARENT
            (sub_wrapper.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER

            sub_wrapper.addView(right_wrapper)
            right_wrapper.layoutParams.height = MATCH_PARENT

            right_wrapper.addView(label_prefix)
            label_prefix.layoutParams.height = 0
            (label_prefix.layoutParams as LinearLayout.LayoutParams).weight = 1F
            label_prefix.gravity = Gravity.START

            right_wrapper.addView(label_octave)
            label_octave.gravity = Gravity.START
            label_octave.layoutParams.height = 0
            (label_octave.layoutParams as LinearLayout.LayoutParams).weight = 1F

            sub_wrapper.addView(label_offset)
            label_offset.layoutParams.height = MATCH_PARENT

            label_prefix.text = if (event.note < 0) {
                context.getString(R.string.pfx_subtract)
            } else {
                context.getString(R.string.pfx_add)
            }
            label_octave.text = "${abs(event.note) / radix}"
            label_offset.text = "${abs(event.note) % radix}"
        } else {
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
    }

    override fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null || this._get_editor_table().needs_setup) {
            return drawableState
        }

        val opus_manager = this.get_opus_manager()
        val beat_key = try {
            this._get_beat_key()
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

        val new_state = mutableListOf<Int>()
        if (tree.is_event()) {
            new_state.add(R.attr.state_active)
            val abs_value = opus_manager.get_absolute_value(beat_key, position)
            if (abs_value == null || abs_value < 0) {
                new_state.add(R.attr.state_invalid)
            }
        }

        if (opus_manager.is_networked(beat_key)) {
            new_state.add(R.attr.state_linked)
        }
        if (opus_manager.is_selected(beat_key, position)) {
            new_state.add(R.attr.state_focused)
        }
        if (beat_key.channel % 2 == 0 && !opus_manager.is_ctl_level_visible(CtlLineLevel.Line)) {
            new_state.add(R.attr.state_alternate)
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }
}
