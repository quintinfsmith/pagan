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
        this.set_text()
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

    private fun set_text() {
        this.removeAllViews()
        val event = this._event ?: return

        val value_text = LeafText(ContextThemeWrapper(this.context, R.style.leaf_value))
        value_text.text = "${event.value}"

        this.addView(value_text)
    }

    private fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null || this._get_editor_table().needs_setup) {
            return drawableState
        }
        val new_state = mutableListOf<Int>()

        val opus_manager = this.get_opus_manager()
        val position = this.position

        //val tree = try {
        //    opus_manager.get_tree(beat_key, position)
        //} catch (e: OpusTree.InvalidGetCall) {
        //    return drawableState
        //} catch (e: IndexOutOfBoundsException) {
        //    return drawableState
        //}

        //if (tree.is_event()) {
        //    new_state.add(R.attr.state_active)
        //    val abs_value = opus_manager.get_absolute_value(beat_key, position)
        //    if (abs_value == null || abs_value < 0) {
        //        new_state.add(R.attr.state_invalid)
        //    }
        //}

        //if (opus_manager.is_selected(beat_key, position)) {
        //    new_state.add(R.attr.state_focused)
        //}

        new_state.add(R.attr.state_channel_even)
        new_state.add(R.attr.state_alternate)
        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
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

    private fun _get_beat_key(): BeatKey? {
        return (this.parent as CellLayout).get_beat_key()
    }

    private fun _get_editor_table(): EditorTable {
        return (this.parent as CellLayout).get_editor_table()
    }
}
