package com.qfs.pagan

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.BaseLayer
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.LinksLayer
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.concurrent.thread
import kotlin.math.abs
import com.qfs.pagan.InterfaceLayer as OpusManager

class LeafButton(
    context: Context,
    initial_radix: Int,
    private var _event: OpusEvent?,
    var position: List<Int>,
    is_percussion: Boolean
) : LinearLayout(ContextThemeWrapper(context, R.style.leaf)) {

    init {
        this.isClickable = false
        this.minimumHeight = resources.getDimension(R.dimen.line_height).toInt()
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
        this.set_text(is_percussion, initial_radix)
        this.setOnClickListener {
            this.callback_click()
        }
        this.animate().alpha(1f)

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
                    is LinksLayer.SelfLinkError -> { }
                    is LinksLayer.MixedLinkException -> {
                        editor_table.notify_cell_change(beat_key)
                        (this.get_activity()).feedback_msg(context.getString(R.string.feedback_mixed_link))
                    }
                    is LinksLayer.LinkRangeOverlap,
                    is BaseLayer.RangeOverflow,
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

    // Prevents the child labels from blocking the parent onTouchListener events
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return true
    }

    private fun set_text(is_percussion: Boolean, _radix: Int? = null) {
        val event = this._event
        val base_context = (this.context as ContextThemeWrapper).baseContext
        val radix = _radix ?: this.get_opus_manager().tuning_map.size
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
        if (beat_key.channel % 2 == 0) {
            new_state.add(R.attr.state_channel_even)
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        return this.build_drawable_state(drawableState)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        var state = 0

        for (item in this.drawableState) {
            state += when (item) {
                R.attr.state_invalid -> 1
                R.attr.state_active -> 2
                R.attr.state_focused -> 4
                R.attr.state_linked -> 8
                R.attr.state_channel_even -> 16
                else -> 0
            }
        }

        val activity = this.get_activity()
        val background = (this.background as LayerDrawable).findDrawableByLayerId(R.id.leaf_background)
        (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_lines).setTint(activity.palette.lines)
        when (state) {
            0 -> {
                background.setTint(activity.palette.channel_odd)
            }
            2,18 -> {
                background.setTint(activity.palette.leaf)
            }
            4,20 -> {
                background.setTint(activity.palette.selection)
            }
            6,22 -> {
                background.setTint(activity.palette.leaf_selected)
            }
            8,24 -> {
                background.setTint(activity.palette.link_empty)
            }
            10,26 -> {
                background.setTint(activity.palette.link)
            }
            12, 28 -> {
                background.setTint(activity.palette.link_empty_selected)
            }
            14, 30 -> {
                background.setTint(activity.palette.link_selected)
            }
            16 -> {
                background.setTint(activity.palette.channel_even)
            }
            else -> {
                background.setTint(activity.palette.leaf_invalid)
            }
        }
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
