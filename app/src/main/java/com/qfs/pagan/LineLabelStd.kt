package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.opusmanager.OpusManagerCursor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class LineLabelStd(context: Context, var channel: Int, var line_offset: Int): AppCompatTextView(ContextThemeWrapper(context, R.style.line_label)) {
    val click_threshold_millis = 250
    val click_threshold_pixels = 5
    var press_position: Pair<Float, Float>? = null
    var press_timestamp: Long = 0
    init {
        this._set_colors()
        this.setOnClickListener {
            this.on_click()
        }

        this.setOnTouchListener { view: View?, touchEvent: MotionEvent? ->
            this.touch_callback(view, touchEvent)
        }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.layoutParams.height = this.resources.getDimension(R.dimen.line_height).roundToInt()
        this.layoutParams.width = this.resources.getDimension(R.dimen.base_leaf_width).roundToInt()
        this.set_text()
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
        return this._build_drawable_state(drawableState)
    }

    private fun _build_drawable_state(drawableState: IntArray?): IntArray? {
        if (this.parent == null) {
            return drawableState
        }

        val opus_manager = this.get_opus_manager()

        val new_state = mutableListOf<Int>()
        if (this.channel % 2 == 0) {
            new_state.add(R.attr.state_channel_even)
        }

        when (opus_manager.cursor.mode) {
            OpusManagerCursor.CursorMode.Single,
            OpusManagerCursor.CursorMode.Line -> {
                if (opus_manager.cursor.ctl_level == null && opus_manager.cursor.channel == this.channel && opus_manager.cursor.line_offset == this.line_offset) {
                    new_state.add(R.attr.state_focused)
                }
            }
            OpusManagerCursor.CursorMode.Range -> {
                val (first, second) = opus_manager.cursor.get_ordered_range()!!
                if ((this.channel > first.channel && this.channel < second.channel) || (this.channel == first.channel && this.line_offset >= first.line_offset) || (this.channel == second.channel && this.line_offset <= second.line_offset)) {
                    new_state.add(R.attr.state_focused)
                }
            }
            else -> { }
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    private fun on_click() {
        val opus_manager = this.get_opus_manager()

        val cursor = opus_manager.cursor
        if (cursor.is_linking_range()) {
            val (first_key, second_key) = cursor.range!!
            if (first_key != second_key) {
                try {
                    when (this.get_activity().configuration.link_mode) {
                        PaganConfiguration.LinkMode.LINK -> {
                            opus_manager.link_beat_range_horizontally(
                                    this.channel,
                                    this.line_offset,
                                    first_key,
                                    cursor.range!!.second
                                    )
                        }

                        else -> {
                            opus_manager.overwrite_beat_range_horizontally(
                                    this.channel,
                                    this.line_offset,
                                    first_key,
                                    cursor.range!!.second
                                    )
                        }
                    }
                } catch (e: OpusLayerLinks.BadRowLink) {
                    // No Feedback.  feels Redundant
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    // No Feedback.  feels Redundant
                }
            } else {
                try {
                    when (this.get_activity().configuration.link_mode) {
                        PaganConfiguration.LinkMode.LINK -> {
                            opus_manager.link_row(this.channel, this.line_offset, first_key)
                        }

                        else -> {
                            opus_manager.overwrite_line(this.channel, this.line_offset, first_key)
                        }
                    }
                } catch (e: OpusLayerLinks.BadRowLink) {
                    // No Feedback.  feels Redundant
                } catch (e: OpusLayerBase.InvalidOverwriteCall) {
                    // No Feedback.  feels Redundant
                }
            }
        }

        opus_manager.cursor_select_line(this.channel, this.line_offset)
    }

    private fun _set_colors() {
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

    fun touch_callback(view: View?, touchEvent: MotionEvent?): Boolean {
        var parent = view?.parent ?: return false
        while (parent !is LineLabelColumnLayout) {
            parent = parent.parent
        }

        val column_layout = parent as LineLabelColumnLayout

        return if (touchEvent == null) {
            true
        } else if (touchEvent.action == MotionEvent.ACTION_MOVE) {
            if (!column_layout.is_dragging()) {
                val d = sqrt((touchEvent.x - this.press_position!!.first).pow(2f) + (touchEvent.y - this.press_position!!.second).pow(2f))
                if (d > this.click_threshold_pixels) {
                    column_layout.set_dragging_line(this.channel, this.line_offset)
                    (view!!.parent as LineLabelView).startDragAndDrop(
                        null,
                        DragShadowBuilder(view),
                        null,
                        0
                    )
                }
            }
            true
        } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
            column_layout.stop_dragging()
            this.press_timestamp = System.currentTimeMillis()
            this.press_position = Pair(touchEvent.x, touchEvent.y)
            true
        } else if (touchEvent.action == MotionEvent.ACTION_UP) {
            if (System.currentTimeMillis() - this.press_timestamp < this.click_threshold_millis && !column_layout.is_dragging()) {
                performClick()
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    fun set_text() {
        val opus_manager = this.get_opus_manager()

        val text = if (!opus_manager.is_percussion(this.channel)) {
            "${this.channel}::${this.line_offset}"
        } else {
            val instrument = opus_manager.get_percussion_instrument(this.line_offset)
            "!$instrument"
        }

        this.text = text
        this.contentDescription = text
    }

    fun get_opus_manager(): OpusLayerInterface {
        return (this.parent as LineLabelView).get_opus_manager()
    }

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }
}
