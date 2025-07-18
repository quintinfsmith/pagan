package com.qfs.pagan.LineLabel

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageView
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.OpusLayerInterface
import com.qfs.pagan.R
import com.qfs.pagan.structure.opusmanager.ControlEventType
import com.qfs.pagan.structure.opusmanager.CtlLineLevel

abstract class LineLabelCtl(context: Context, var ctl_level: CtlLineLevel, var ctl_type: ControlEventType): AppCompatImageView(
    ContextThemeWrapper(context, R.style.ctl_line_label)
) {
    init {
        this.setOnClickListener {
            this.on_click()
        }
        this.setOnLongClickListener {
            this.on_long_click()
        }
    }

    abstract fun on_click()
    abstract fun on_long_click(): Boolean

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = this.resources.getDimension(R.dimen.ctl_line_height).toInt()
        this.layoutParams.width = this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        this.setImageResource(this.get_label_icon())
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 3)
        return this._build_drawable_state(drawableState)
    }

    private fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null) {
            return drawableState
        }

        val new_state = mutableListOf<Int>()

        if (this.is_selected()) {
            new_state.add(R.attr.state_focused)
        } else if (this.is_selected_secondary()) {
            new_state.add(R.attr.state_focused_secondary)
        }

        if (this.is_muted()) {
            new_state.add(R.attr.state_muted)
        }

        new_state.add(R.attr.state_channel_even)

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    abstract fun is_selected(): Boolean
    abstract fun is_selected_secondary(): Boolean
    abstract fun is_muted(): Boolean

    fun get_label_icon(): Int {
        return when (this.ctl_type) {
            ControlEventType.Tempo -> R.drawable.tempo
            ControlEventType.Volume -> R.drawable.volume
            ControlEventType.Reverb -> R.drawable.volume // Placeholder TODO
            ControlEventType.Pan -> R.drawable.pan_icon
        }
    }

    fun get_opus_manager(): OpusLayerInterface {
        return (this.parent as LineLabelView).get_opus_manager()
    }

    fun get_activity(): ActivityEditor {
        return (this.context as ContextThemeWrapper).baseContext as ActivityEditor
    }
}
