package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.view.MotionEvent
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.ColorMap.Palette
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.OpusLayerInterface as OpusManager

abstract class LeafButton(context: Context) : LinearLayout(context) {

    init {
        this.isClickable = false
        this.minimumWidth = resources.getDimension(R.dimen.base_leaf_width).toInt()
        this.setOnClickListener {
            this.callback_click()
        }
        this.animate().alpha(1f)
        this._setup_colors()

        this.setOnLongClickListener {
            this.long_click()
        }
    }

    private fun get_states(): Array<IntArray> {
        return arrayOf(
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
            )
        )
    }

    abstract fun get_tint_list(): IntArray

    private fun _setup_colors() {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map

        (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_lines).setTint(
            color_map[Palette.Lines]
        )

        (this.background as LayerDrawable).findDrawableByLayerId(R.id.leaf_background).setTintList(
            ColorStateList(
                this.get_states(),
                this.get_tint_list()
            )
        )
    }

    // Prevents the child labels from blocking the parent onTouchListener events
    override fun onInterceptTouchEvent(touchEvent: MotionEvent): Boolean {
        return true
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 5)
        return this._build_drawable_state(drawableState)
    }

    abstract fun long_click(): Boolean
    abstract fun callback_click()
    abstract fun is_selected(): Boolean
    abstract fun _build_drawable_state(drawableState: IntArray?): IntArray?
    // ------------------------------------------------------//
    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return (this.parent as CellLayout).get_opus_manager()
    }

    internal fun get_coord(): EditorTable.Coordinate {
        return (this.parent as CellLayout).get_coord()
    }

    internal fun _get_beat_key(): BeatKey {
        return (this.parent as CellLayout).get_beat_key()!!
    }

    internal fun get_beat(): Int {
        return (this.parent as CellLayout).get_beat()
    }

    internal fun _get_editor_table(): EditorTable {
        return (this.parent as CellLayout).get_editor_table()
    }
}
