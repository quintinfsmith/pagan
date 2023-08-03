package com.qfs.pagan

import android.content.Context
import android.util.Log
import android.view.Gravity.CENTER
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.concurrent.thread
import com.qfs.pagan.InterfaceLayer as OpusManager

class LeafButton(
    context: Context,
    private var activity: MainActivity,
    private var event: OpusEvent?,
    var position: List<Int>,
    is_percussion: Boolean
) : LinearLayout(ContextThemeWrapper(context, R.style.leaf)) {

    // LeafText exists to make the text consider the state of the LeafButton
    class InnerWrapper(context: Context): LinearLayout(context) {
        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 4)
            val parent = this.parent ?: return drawableState
            return (parent as LeafButton).drawableState
        }
    }

    class LeafText(context: Context): androidx.appcompat.widget.AppCompatTextView(context) {
        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 4)
            var parent = this.parent ?: return drawableState
            while (parent !is LeafButton) {
                parent = parent.parent
            }
            return parent.drawableState
        }
    }

    private val STATE_LINKED = intArrayOf(R.attr.state_linked)
    private val STATE_ACTIVE = intArrayOf(R.attr.state_active)
    private val STATE_FOCUSED = intArrayOf(R.attr.state_focused)
    private val STATE_INVALID = intArrayOf(R.attr.state_invalid)

    private var value_wrapper: LinearLayout
    private var value_label_octave: TextView
    private var value_label_offset: TextView
    private var prefix_label: TextView
    private var inner_wrapper: InnerWrapper = InnerWrapper(ContextThemeWrapper(this.context, R.style.leaf_inner))

    init {
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
        this.inner_wrapper.orientation = VERTICAL
        this.value_wrapper = LinearLayout(ContextThemeWrapper(this.context, R.style.leaf_value))
        this.value_wrapper.orientation = HORIZONTAL

        this.value_label_octave = LeafText(ContextThemeWrapper(this.context, R.style.leaf_value_octave))
        this.value_label_offset = LeafText(ContextThemeWrapper(this.context, R.style.leaf_value_offset))
        this.prefix_label = LeafText(ContextThemeWrapper(this.context, R.style.leaf_prefix))
        (this.inner_wrapper as LinearLayout).addView(this.prefix_label)
        (this.inner_wrapper as LinearLayout).addView(this.value_wrapper)
        this.value_wrapper.addView(this.value_label_octave)
        this.value_wrapper.addView(this.value_label_offset)

        this.addView(this.inner_wrapper)
        this.inner_wrapper.layoutParams.apply {
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        this.set_text(is_percussion)
        this.setOnClickListener {
            this.callback_click()
        }
    }

    private fun callback_click() {
        val beat_key = this.get_beat_key()
        val position = this.position
        val editor_table = this.get_editor_table()
        val opus_manager = this.get_opus_manager()

        if (editor_table.is_linking()) {
           // // If a second link point hasn't been selected, assume just one beat is being linked
           // if (opus_manager.cursor.mode != Cursor.CursorMode.Range) {
           //     try {
           //         opus_manager.link_beats(beatkey, this.linking_beat!!)
           //         opus_manager.cursor_select(
           //             beatkey,
           //             opus_manager.get_first_position(beatkey)
           //         )
           //     } catch (e: Exception) {
           //         when (e) {
           //             is LinksLayer.SelfLinkError -> { }
           //             is LinksLayer.MixedLinkException -> {
           //                 this.get_main_activity().feedback_msg("Can't link percussion to non-percussion")
           //             }
           //             else -> {
           //                 throw e
           //             }
           //         }
           //     }
           // } else {
           //     try {
           //         opus_manager.link_beat_range(
           //             beatkey,
           //             this.linking_beat!!,
           //             this.linking_beat_b!!
           //         )
           //         opus_manager.cursor_select(beatkey, position)
           //     } catch (e: Exception) {
           //         when (e) {
           //             is LinksLayer.SelfLinkError -> { }
           //             is LinksLayer.LinkRangeOverlap -> { }
           //             is LinksLayer.LinkRangeOverflow -> { }
           //             is LinksLayer.MixedLinkException -> {
           //                 this.get_main_activity().feedback_msg(
           //                     "Can't link percussion to non-percussion"
           //                 )
           //             }
           //             else -> {
           //                 throw e
           //             }
           //         }

           //     }
           // }
           // this.cancel_linking()
        } else {
            opus_manager.cursor_select(beat_key, position)
            val tree = opus_manager.get_tree()

            thread {
                if (tree.is_event()) {
                    val abs_value = opus_manager.get_absolute_value(beat_key, position)
                    if ((abs_value != null) && abs_value in (0 .. 90)) {
                        (this.get_editor_table().context as MainActivity).play_event(
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

    private fun unset_text() {
        this.prefix_label.visibility = View.GONE
        this.value_label_octave.visibility = View.GONE
        this.value_label_offset.visibility = View.GONE
    }

    private fun set_text(is_percussion: Boolean) {
        if (this.event == null) {
            this.unset_text()
            return
        }

        val event = this.event!!

        var use_note = event.note
        this.prefix_label.text = if (!is_percussion && (event.relative && event.note != 0)) {
            this.prefix_label.visibility = View.VISIBLE
            if (event.note < 0) {
                use_note = 0 - event.note
                this.activity.getString(R.string.pfx_subtract)
            } else {
                this.activity.getString(R.string.pfx_add)
            }
        } else {
            this.prefix_label.visibility = View.GONE
            ""
        }


        if (is_percussion) {
            this.value_label_octave.visibility = View.GONE
            this.value_label_offset.text = this.activity.getString(R.string.percussion_label)
        } else if (event.relative && event.note == 0) {
            this.value_label_octave.visibility = View.GONE
            this.value_label_offset.text = this.activity.getString(R.string.repeat_note)
        } else {
            this.value_label_octave.visibility = View.VISIBLE
            this.value_label_octave.text = get_number_string(use_note / event.radix, event.radix, 1)
            this.value_label_offset.text = get_number_string(use_note % event.radix, event.radix, 1)
        }

        if (event.relative && event.note != 0) {
            (this.prefix_label.layoutParams as LayoutParams).apply {
                height = WRAP_CONTENT
                setMargins(0,-24,0,0)
                gravity = CENTER
            }
            (this.value_wrapper.layoutParams as LayoutParams).apply {
                weight = 1F
                height = 0
                gravity = CENTER
                setMargins(0,-30,0,0)
            }
        } else {
            (this.prefix_label.layoutParams as LayoutParams).apply {
                height = WRAP_CONTENT
                setMargins(0,0,0,0)
                gravity = CENTER
            }
            (this.value_wrapper.layoutParams as LayoutParams).apply {
                weight = 1F
                height = 0
                gravity = CENTER
                setMargins(0,0,0,0)
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val line_height = resources.getDimension(R.dimen.line_height)
        this.layoutParams.height = line_height.toInt()
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
    }

    fun build_drawable_state(drawableState: IntArray?): IntArray? {
        val opus_manager = this.get_opus_manager()
        val beat_key = this.get_beat_key()
        if (beat_key.beat == -1) {
            return drawableState
        }

        val position = this.position
        val tree = opus_manager.get_tree(beat_key, position)

        if (tree.is_event()) {
            mergeDrawableStates(drawableState, STATE_ACTIVE)
            val abs_value = opus_manager.get_absolute_value(beat_key, position)
            if (abs_value == null || abs_value < 0) {
                mergeDrawableStates(drawableState, STATE_INVALID)
            }
        }

        if (opus_manager.is_networked(beat_key)) {
            mergeDrawableStates(drawableState, STATE_LINKED)
        }
        if (opus_manager.is_selected(beat_key, position)) {
            mergeDrawableStates(drawableState, STATE_FOCUSED)
        }


        return drawableState
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 4)
        return this.build_drawable_state(drawableState)
    }

    override fun refreshDrawableState() {
        this.value_label_octave.refreshDrawableState()
        this.value_label_offset.refreshDrawableState()
        this.prefix_label.refreshDrawableState()
        this.inner_wrapper.refreshDrawableState()
        super.refreshDrawableState()
    }

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
