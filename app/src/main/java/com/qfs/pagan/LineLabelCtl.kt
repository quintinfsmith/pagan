package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel

open class LineLabelCtl(context: Context, var ctl_level: CtlLineLevel, var ctl_type: ControlEventType): LineLabelInner(
    ContextThemeWrapper(context, R.style.line_label)
) {
    override fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null) {
            return drawableState
        }

        val new_state = mutableListOf<Int>()

        if (this.is_selected()) {
            new_state.add(R.attr.state_focused)
        }

        new_state.add(R.attr.state_channel_even)

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    open fun is_selected(): Boolean {
        TODO()
    }

    override fun get_label_text(): String {
        return when (this.ctl_type) {
            ControlEventType.Tempo -> "BPM"
            ControlEventType.Volume -> "VOL"
            ControlEventType.Reverb -> "RVB"
        }
    }

    override fun get_height(): Float {
        return this.resources.getDimension(R.dimen.ctl_line_height)
    }

    override fun on_click() {
        TODO("Define Lower")
    }

    override fun _set_colors() {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map
        (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_lines).setTint(color_map[ColorMap.Palette.Lines])
        val states = arrayOf<IntArray>(
            intArrayOf(
                R.attr.state_focused,
            ),
            intArrayOf(
                -R.attr.state_focused,
                -R.attr.state_channel_even
            ),
            intArrayOf(
                -R.attr.state_focused,
                R.attr.state_channel_even
            )
        )

        (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_background).setTintList(
            ColorStateList(
                states,
                intArrayOf(
                    color_map[ColorMap.Palette.Selection],
                    color_map[ColorMap.Palette.ChannelOdd],
                    color_map[ColorMap.Palette.ChannelEven]
                )
            )
        )
        this.setTextColor(
            ColorStateList(
                states,
                intArrayOf(
                    color_map[ColorMap.Palette.SelectionText],
                    color_map[ColorMap.Palette.ChannelOddText],
                    color_map[ColorMap.Palette.ChannelEvenText]
                )
            )
        )
    }
}
