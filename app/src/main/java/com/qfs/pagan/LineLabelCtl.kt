package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusManagerCursor

open class LineLabelCtl(context: Context, var ctl_level: CtlLineLevel, var ctl_type: ControlEventType): LineLabelInner(
    ContextThemeWrapper(context, R.style.line_label)
) {
    override fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null) {
            return drawableState
        }

        val opus_manager = this.get_opus_manager()

        val new_state = mutableListOf<Int>()
        when (opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = opus_manager.cursor.range!!
                val visible_line_index = opus_manager.get_ctl_line_from_visible_row((this.parent as LineLabelView).row)
                val first_line = opus_manager.get_visible_row_from_ctl_line(
                    opus_manager.get_ctl_line_index(
                        opus_manager.get_abs_offset(first.channel, first.line_offset)
                    )
                )!!
                val second_line = opus_manager.get_visible_row_from_ctl_line(
                    opus_manager.get_ctl_line_index(
                        opus_manager.get_abs_offset(second.channel, second.line_offset)
                    )
                )!!

                if ((first_line .. second_line).contains(visible_line_index)) {
                    new_state.add(R.attr.state_focused)
                }
            }
            else -> { }

        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    override fun get_label_text(): String {
        return "${this.ctl_level}".substring(0..2)
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
