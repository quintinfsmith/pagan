package com.qfs.pagan

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.LinksLayer
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.concurrent.thread
import kotlin.math.abs
import com.qfs.pagan.InterfaceLayer as OpusManager

class LeafButton(
    context: Context,
    private var _event: OpusEvent?,
    var position: List<Int>,
    is_percussion: Boolean
) : LinearLayout(ContextThemeWrapper(context, R.style.leaf)) {

    companion object {
        private val STATE_LINKED = intArrayOf(R.attr.state_linked)
        private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
        private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
        private val STATE_INVALID = intArrayOf(R.attr.state_invalid)
        private val STATE_CHANNEL_EVEN = intArrayOf(R.attr.state_channel_even)
    }

    init {
        this.isClickable = false
        this.minimumHeight = resources.getDimension(R.dimen.line_height).toInt()
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()

        this.set_text(is_percussion)
        this.setOnClickListener {
            this.callback_click()
        }

        this.setOnLongClickListener {
            val opus_manager = this.get_opus_manager()
            val cursor = opus_manager.cursor
            val beat_key = this.get_beat_key()
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
            true
        }
    }

    private fun callback_click() {
        val beat_key = this.get_beat_key()
        val position = this.position
        val opus_manager = this.get_opus_manager()

        val editor_table = this.get_editor_table() // Will need if overflow exception is passed
        if (opus_manager.cursor.is_linking) {
            try {
                opus_manager.link_beat(beat_key)
                opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
            } catch (e: Exception) {
                when (e) {
                    is LinksLayer.SelfLinkError -> { }
                    is LinksLayer.MixedLinkException -> {
                        editor_table.notify_cell_change(beat_key)
                        (this.context as MainActivity).feedback_msg(context.getString(R.string.feedback_mixed_link))
                    }
                    is LinksLayer.LinkRangeOverlap,
                    is LinksLayer.LinkRangeOverflow -> {
                        editor_table.notify_cell_change(beat_key)
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
                    if ((abs_value != null) && abs_value in (0 .. 90)) {
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

    // Prevents the child labels from blocking the parent onTouchListener events
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return true
    }

    private fun set_text(is_percussion: Boolean) {
        val event = this._event
        var base_context = (this.context as ContextThemeWrapper).baseContext
        this.removeAllViews()
        if (event == null) {
        } else if (is_percussion) {
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
                "-"
            } else {
                "+"
            }
            label_octave.text = "${abs(event.note) / event.radix}"
            label_offset.text = "${abs(event.note) % event.radix}"
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

            label_octave.text = "${event.note / event.radix}"
            label_offset.text = "${event.note % event.radix}"
        }
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
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
        }

        if (tree.is_event()) {
            mergeDrawableStates(drawableState, LeafButton.STATE_ACTIVE)
            val abs_value = opus_manager.get_absolute_value(beat_key, position)
            if (abs_value == null || abs_value < 0) {
                mergeDrawableStates(drawableState, LeafButton.STATE_INVALID)
            }
        }

        if (opus_manager.is_networked(beat_key)) {
            mergeDrawableStates(drawableState, LeafButton.STATE_LINKED)
        }
        if (opus_manager.is_selected(beat_key, position)) {
            mergeDrawableStates(drawableState, LeafButton.STATE_FOCUSED)
        }
        if (beat_key.channel % 2 == 0) {
            mergeDrawableStates(drawableState, LeafButton.STATE_CHANNEL_EVEN)
        }

        return drawableState
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        return this.build_drawable_state(drawableState)
    }

    // ------------------------------------------------------//
    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return (this.parent as CellLayout).get_opus_manager()
    }
    fun get_beat_key(): BeatKey {
        return (this.parent as CellLayout).get_beat_key()
    }
    fun get_beat_tree(): OpusTree<OpusEvent> {
        return (this.parent as CellLayout).get_beat_tree()
    }
    fun get_editor_table(): EditorTable {
        return (this.parent as CellLayout).get_editor_table()
    }
}
