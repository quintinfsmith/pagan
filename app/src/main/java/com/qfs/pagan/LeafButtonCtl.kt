package com.qfs.pagan

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.structure.OpusTree

abstract class LeafButtonCtl(
    context: Context,
    private var _event: OpusControlEvent?,
    var position: List<Int>,
    var control_level: CtlLineLevel,
    var control_type: ControlEventType
) : LeafButton(context) {
    init {
        this.minimumHeight = resources.getDimension(R.dimen.line_height).toInt()
        this.set_text()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (this.layoutParams as LayoutParams).gravity = Gravity.CENTER_VERTICAL
        this.setPadding(0,0,0,0)
    }

    override fun long_click(): Boolean {
        TODO("Not yet implemented")
    }

    override fun callback_click() {
        TODO("Not Yet Implemented")
    }

    override fun is_selected(): Boolean {
        TODO("Not yet implemented")
    }

    fun set_text() {
        this.removeAllViews()
        val event = this._event ?: return

        val value_text = LeafText(
            ContextThemeWrapper(this.context, R.style.leaf_value)
        )
        value_text.text = when (this.control_type) {
            ControlEventType.Tempo -> "${event.value.toInt()}"
            ControlEventType.Volume -> "${event.value.toInt()}"
            ControlEventType.Reverb -> TODO()
        }
        this.addView(value_text)

        (value_text.layoutParams as LayoutParams).gravity = Gravity.CENTER
        value_text.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    override fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null || this._get_editor_table().needs_setup) {
            return drawableState
        }
        val new_state = mutableListOf<Int>()

        val tree = try {
            this.get_tree()
        } catch (e: OpusTree.InvalidGetCall) {
            return drawableState
        } catch (e: IndexOutOfBoundsException) {
            return drawableState
        }

        if (tree.is_event()) {
            new_state.add(R.attr.state_active)
        }

        if (this.is_selected()) {
            new_state.add(R.attr.state_focused)
        }

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
    open fun get_tree(): OpusTree<OpusControlEvent> {
        throw UninitializedPropertyAccessException()
    }
}
