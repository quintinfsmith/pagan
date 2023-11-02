package com.qfs.pagan

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
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
    private var _activity: MainActivity,
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

    var invalid: Boolean = false

    init {
        this.isClickable = false
        this.minimumHeight = resources.getDimension(R.dimen.line_height).toInt()
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()

        this.set_text(is_percussion)
        this.setOnClickListener {
            this._activity.runOnUiThread {
                this.callback_click()
            }
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
            false
        }
    }

    private fun callback_click() {
        val beat_key = this.get_beat_key()
        val position = this.position
        val opus_manager = this.get_opus_manager()

        val editor_table = this.get_editor_table() // Will need if overflow exception is passed
        if (opus_manager.cursor.is_linking) {
            try {
                this.invalid = true

                opus_manager.link_beat(beat_key)
                opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
            } catch (e: Exception) {
                when (e) {
                    is LinksLayer.SelfLinkError -> { }
                    is LinksLayer.MixedLinkException -> {
                        editor_table.notify_cell_change(beat_key)
                        this._activity.feedback_msg(context.getString(R.string.feedback_mixed_link))
                    }
                    is LinksLayer.LinkRangeOverlap,
                    is LinksLayer.LinkRangeOverflow -> {
                        editor_table.notify_cell_change(beat_key)
                        opus_manager.cursor.is_linking = false
                        opus_manager.cursor_select(beat_key, this.position)
                        this._activity.feedback_msg(context.getString(R.string.feedback_bad_range))
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
        this.removeAllViews()
        var base_context = (this.context as ContextThemeWrapper).baseContext
        if (event == null) {
            val inner_wrapper: View = LayoutInflater.from(base_context)
                .inflate(
                    R.layout.leaf_empty,
                    this,
                    false
                )
            this.addView(inner_wrapper)
        } else if (is_percussion) {
            val inner_wrapper: View = LayoutInflater.from(base_context)
                .inflate(
                    R.layout.leaf_percussion,
                    this,
                    false
                )
            this.addView(inner_wrapper)
        } else if (event.relative) {
            val inner_wrapper: View = LayoutInflater.from(base_context)
                .inflate(
                    R.layout.leaf_relative,
                    this,
                    false
                )
            this.addView(inner_wrapper)

            val label_prefix = inner_wrapper.findViewById<TextView>(R.id.tvPrefix)
            val label_octave = inner_wrapper.findViewById<TextView>(R.id.tvOctave)
            val label_offset = inner_wrapper.findViewById<TextView>(R.id.tvOffset)
            label_prefix.text = if (event.note < 0) {
                "-"
            } else {
                "+"
            }
            label_octave.text = "${abs(event.note) / event.radix}"
            label_offset.text = "${abs(event.note) % event.radix}"
        } else {
            val inner_wrapper: View = LayoutInflater.from(base_context)
                .inflate(
                    R.layout.leaf_absolute,
                    this,
                    false
                )
            this.addView(inner_wrapper)
            val label_octave = inner_wrapper.findViewById<TextView>(R.id.tvOctave)
            val label_offset = inner_wrapper.findViewById<TextView>(R.id.tvOffset)
            label_octave.text = "${event.note / event.radix}"
            label_offset.text = "${event.note % event.radix}"
        }
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        val opus_manager = this.get_opus_manager()
        val beat_key = this.get_beat_key()
        if (beat_key.beat == -1) {
            return drawableState
        }
        if (this.invalid) {
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

    //override fun refreshDrawableState() {
    //    this.value_label_octave.refreshDrawableState()
    //    this.value_label_offset.refreshDrawableState()
    //    this.prefix_label.refreshDrawableState()
    //    this.inner_wrapper.refreshDrawableState()
    //    super.refreshDrawableState()
    //}

    // ------------------------------------------------------//
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
