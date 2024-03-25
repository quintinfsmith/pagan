package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.ColorMap.Palette
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.OpusLayerInterface as OpusManager

class ControlLeafButton(
    context: Context,
    private var _event: OpusControlEvent?,
    var position: List<Int>,
    var control_type: ControlEventType
) : LinearLayout(ContextThemeWrapper(context, R.style.leaf)) {

    init {
        this.isClickable = false
        this.minimumHeight = resources.getDimension(R.dimen.line_height).toInt()
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
        //this.set_text(initial_radix)
        this.setOnClickListener {
            this.callback_click()
        }
        this.animate().alpha(1f)

        this._setup_colors()
    }

    private fun _setup_colors() {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map

        val states = arrayOf(
            //------------------------------
            intArrayOf(
                R.attr.state_invalid,
                R.attr.state_focused
            ),
            intArrayOf(
                R.attr.state_invalid,
                -R.attr.state_focused
            ),
            //------------------------------
            intArrayOf(
                -R.attr.state_invalid,
                -R.attr.state_linked,
                -R.attr.state_active,
                -R.attr.state_focused,
                R.attr.state_alternate
            ),
            intArrayOf(
                -R.attr.state_invalid,
                -R.attr.state_linked,
                -R.attr.state_active,
                -R.attr.state_focused,
                -R.attr.state_alternate
            ),
            //----------------------------
            intArrayOf(
                -R.attr.state_invalid,
                -R.attr.state_linked,
                R.attr.state_active,
                R.attr.state_focused
            ),
            intArrayOf(
                -R.attr.state_invalid,
                -R.attr.state_linked,
                R.attr.state_active,
                -R.attr.state_focused
            ),
            intArrayOf(
                -R.attr.state_invalid,
                -R.attr.state_linked,
                -R.attr.state_active,
                R.attr.state_focused
            ),
            // ------------------------
            intArrayOf(
                -R.attr.state_invalid,
                R.attr.state_linked,
                R.attr.state_active,
                R.attr.state_focused
            ),
            intArrayOf(
                -R.attr.state_invalid,
                R.attr.state_linked,
                R.attr.state_active,
                -R.attr.state_focused
            ),
            intArrayOf(
                -R.attr.state_invalid,
                R.attr.state_linked,
                -R.attr.state_active,
                R.attr.state_focused
            ),
            intArrayOf(
                -R.attr.state_invalid,
                R.attr.state_linked,
                -R.attr.state_active,
                -R.attr.state_focused
            ),
        )

        (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_lines).setTint(color_map[Palette.Lines])
        (this.background as LayerDrawable).findDrawableByLayerId(R.id.leaf_background).setTintList(
            ColorStateList(
                states,
                intArrayOf(
                    color_map[Palette.LeafInvalidSelected],
                    color_map[Palette.LeafInvalid],
                    color_map[Palette.ChannelEven],
                    color_map[Palette.ChannelOdd],

                    color_map[Palette.LeafSelected],
                    color_map[Palette.Leaf],
                    color_map[Palette.Selection],

                    color_map[Palette.LinkSelected],
                    color_map[Palette.Link],
                    color_map[Palette.LinkEmptySelected],
                    color_map[Palette.LinkEmpty]
                )
            )
        )
    }

    private fun callback_click() {
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
            return
        }

       // if (is_percussion) {
       //     val label_percussion = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value))
       //     this.addView(label_percussion)
       //     label_percussion.gravity = Gravity.CENTER
       //     label_percussion.text = resources.getString(R.string.percussion_label)
       // } else if (event.relative) {
       //     val sub_wrapper = LinearLayout(base_context)
       //     val right_wrapper = LinearLayout(base_context)
       //     right_wrapper.orientation = VERTICAL
       //     val label_octave = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_octave))
       //     val label_offset = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_offset))
       //     val label_prefix = LeafText(ContextThemeWrapper(base_context, R.style.leaf_prefix))

       //     this.addView(sub_wrapper)
       //     sub_wrapper.layoutParams.width = WRAP_CONTENT
       //     sub_wrapper.layoutParams.height = MATCH_PARENT
       //     (sub_wrapper.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER

       //     sub_wrapper.addView(right_wrapper)
       //     right_wrapper.layoutParams.height = MATCH_PARENT

       //     right_wrapper.addView(label_prefix)
       //     label_prefix.layoutParams.height = 0
       //     (label_prefix.layoutParams as LinearLayout.LayoutParams).weight = 1F
       //     label_prefix.gravity = Gravity.START

       //     right_wrapper.addView(label_octave)
       //     label_octave.gravity = Gravity.START
       //     label_octave.layoutParams.height = 0
       //     (label_octave.layoutParams as LinearLayout.LayoutParams).weight = 1F

       //     sub_wrapper.addView(label_offset)
       //     label_offset.layoutParams.height = MATCH_PARENT

       //     label_prefix.text = if (event.note < 0) {
       //         context.getString(R.string.pfx_subtract)
       //     } else {
       //         context.getString(R.string.pfx_add)
       //     }
       //     label_octave.text = "${abs(event.note) / radix}"
       //     label_offset.text = "${abs(event.note) % radix}"
       // } else {
       //     val sub_wrapper = LinearLayout(base_context)
       //     val label_octave = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_octave))
       //     val label_offset = LeafText(ContextThemeWrapper(base_context, R.style.leaf_value_offset))

       //     this.addView(sub_wrapper)
       //     sub_wrapper.layoutParams.width = WRAP_CONTENT
       //     sub_wrapper.layoutParams.height = MATCH_PARENT
       //     (sub_wrapper.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER

       //     sub_wrapper.addView(label_octave)
       //     label_octave.gravity = Gravity.BOTTOM
       //     label_octave.layoutParams.height = MATCH_PARENT

       //     sub_wrapper.addView(label_offset)
       //     label_offset.layoutParams.height = MATCH_PARENT

       //     label_octave.text = "${event.note / radix}"
       //     label_offset.text = "${event.note % radix}"
       // }
    }

    private fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null || this._get_editor_table().needs_setup) {
            return drawableState
        }

        return drawableState

        //val opus_manager = this.get_opus_manager()
        //val beat_key = try {
        //    this._get_beat_key()
        //} catch (e: IndexOutOfBoundsException) {
        //    return drawableState
        //}
        //if (beat_key.beat == -1) {
        //    return drawableState
        //}
        //val position = this.position

        //val tree = try {
        //    opus_manager.get_tree(beat_key, position)
        //} catch (e: OpusTree.InvalidGetCall) {
        //    return drawableState
        //} catch (e: IndexOutOfBoundsException) {
        //    return drawableState
        //}

        //val new_state = mutableListOf<Int>()
        //if (tree.is_event()) {
        //    new_state.add(R.attr.state_active)
        //    val abs_value = opus_manager.get_absolute_value(beat_key, position)
        //    if (abs_value == null || abs_value < 0) {
        //        new_state.add(R.attr.state_invalid)
        //    }
        //}

        //if (opus_manager.is_networked(beat_key)) {
        //    new_state.add(R.attr.state_linked)
        //}
        //if (opus_manager.is_selected(beat_key, position)) {
        //    new_state.add(R.attr.state_focused)
        //}
        //if (beat_key.channel % 2 == 0) {
        //    new_state.add(R.attr.state_alternate)
        //}

        //mergeDrawableStates(drawableState, new_state.toIntArray())
        //return drawableState
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        return this._build_drawable_state(drawableState)
    }

    // ------------------------------------------------------//
    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return (this.parent as CellLayout).get_opus_manager()
    }

    private fun _get_beat_key(): BeatKey {
        return (this.parent as CellLayout).get_beat_key()
    }

    private fun _get_editor_table(): EditorTable {
        return (this.parent as CellLayout).get_editor_table()
    }
}
