package com.qfs.pagan

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.AppCompatTextView
import com.qfs.pagan.ColorMap.Palette
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.OpusLayerInterface as OpusManager

class LineLabelView(context: Context, var row: Int): AppCompatTextView(ContextThemeWrapper(context, R.style.line_label)),
    View.OnTouchListener {

    /*
     * update_queued exists to handle the liminal state between being detached and being destroyed
     * If the cursor is pointed to a location in this space, but changed, then the recycler view doesn't handle it normally
     */
    private var _update_queued = false
    init {
        this._set_colors()

        this.setOnClickListener {
            this.on_click()
        }

        this.setOnTouchListener(this)

        this.setOnDragListener { view: View, dragEvent: DragEvent ->
            val adapter = (view.parent.parent as LineLabelColumnLayout)
            val opus_manager = this.get_opus_manager()
            val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(this.row)
            if (ctl_level != null) {
                return@setOnDragListener true
            }

            when (dragEvent.action) {
                DragEvent.ACTION_DROP -> {
                    if (adapter.is_dragging()) {
                        val (from_channel, from_line) = adapter.dragging_position!!
                        val (to_channel, to_line) = opus_manager.get_std_offset(pointer)
                        if (from_channel != to_channel || from_line != to_line) {
                            try {
                                opus_manager.swap_lines(
                                    from_channel,
                                    from_line,
                                    to_channel,
                                    to_line
                                )
                            } catch (e: OpusLayerBase.IncompatibleChannelException) {
                                this.get_activity().feedback_msg("Can't swap percussion with other instruments")
                            }
                            //opus_manager.move_line(
                            //    from_channel,
                            //    from_line,
                            //    to_channel,
                            //    to_line
                            //)
                        }
                    }
                    adapter.stop_dragging()
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    adapter.stop_dragging()
                }
                DragEvent.ACTION_DRAG_STARTED -> { }
                else -> { }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val line_height = resources.getDimension(R.dimen.line_height)
        this.layoutParams.height = line_height.toInt()
        this.layoutParams.width = WRAP_CONTENT
        this.set_text()
    }

    private fun get_label_text(): String {
        val opus_manager = this.get_opus_manager()

        val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(
            this.row
        )

        return when (ctl_level) {
            null -> {
                var (channel, line_offset) = opus_manager.get_std_offset(pointer)
                if (!opus_manager.is_percussion(channel)) {
                    "${channel}::${line_offset}"
                } else {
                    val instrument = opus_manager.get_percussion_instrument(line_offset)
                    "!$instrument"
                }
            }
            else -> {
                "TODO"
            }
            //CtlLineLevel.Line -> {
            //}
            //CtlLineLevel.Channel -> TODO()
            //CtlLineLevel.Global -> TODO()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        this._update_queued = true
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
        val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(this.row)

        val new_state = mutableListOf<Int>()
        when (ctl_level) {
            null -> {
                var (channel, line_offset) = opus_manager.get_std_offset(pointer)
                if (channel % 2 == 0) {
                    new_state.add(R.attr.state_channel_even)
                }

                when (opus_manager.cursor.mode) {
                    OpusManagerCursor.CursorMode.Single,
                    OpusManagerCursor.CursorMode.Row -> {
                        if (opus_manager.cursor.channel == channel && opus_manager.cursor.line_offset == line_offset) {
                            new_state.add(R.attr.state_focused)
                        }
                    }
                    OpusManagerCursor.CursorMode.Range -> {
                        val (first, second) = opus_manager.cursor.range!!
                        if ((channel > first.channel && channel < second.channel) || (channel == first.channel && line_offset >= first.line_offset) || (channel == second.channel && line_offset <= second.line_offset)) {
                            new_state.add(R.attr.state_focused)
                        }
                    }
                    else -> { }
                }
            }
            else -> {
            }
        }

        mergeDrawableStates(drawableState, new_state.toIntArray())
        return drawableState
    }

    fun set_text() {
        val text = this.get_label_text()
        this.text = text
        this.contentDescription = text
    }

    fun get_opus_manager(): OpusManager {
        return (this.parent.parent as LineLabelColumnLayout).get_opus_manager()
    }

    override fun onTouch(view: View?, touchEvent: MotionEvent?): Boolean {
        val column_layout = this.parent.parent as LineLabelColumnLayout

        val opus_manager = this.get_opus_manager()
        val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(this.row)

        // TODO; not sure what i'm going to do with this, might not be draggable
        if (ctl_level != null) {
            return false
        }

        return if (touchEvent == null) {
            true
        } else if (touchEvent.action == MotionEvent.ACTION_MOVE) {
            val (channel, line_offset) = opus_manager.get_std_offset(pointer)
            if (!column_layout.is_dragging()) {
                column_layout.set_dragging_line(channel, line_offset)
                (view as LineLabelView).startDragAndDrop(
                    null,
                    DragShadowBuilder(view),
                    null,
                    0
                )
            }
            true
        } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
            column_layout.stop_dragging()
            true
        } else {
            performClick()
        }
    }

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }


    private fun on_click() {
        val opus_manager = this.get_opus_manager()

        val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(this.row)

        // TODO
        if (ctl_level != null) {
            return
        }

        val (channel, line_offset) = opus_manager.get_std_offset(pointer)

        val cursor = opus_manager.cursor
        if (cursor.is_linking_range()) {
            val first_key = cursor.range!!.first
            try {
                when (this.get_activity().configuration.link_mode) {
                    PaganConfiguration.LinkMode.LINK -> {
                        opus_manager.link_beat_range_horizontally(
                            channel,
                            line_offset,
                            first_key,
                            cursor.range!!.second
                        )
                    }

                    else -> {
                        opus_manager.overwrite_beat_range_horizontally(
                            channel,
                            line_offset,
                            first_key,
                            cursor.range!!.second
                        )
                    }
                }
            } catch (e: OpusLayerLinks.BadRowLink) {
                // TODO: Feedback
                //(this.context as MainActivity).feedback_msg("Can only row-link from first beat")
            }
            cursor.is_linking = false
            opus_manager.cursor_select(first_key, opus_manager.get_first_position(first_key))
        } else if (cursor.is_linking) {
            val beat_key = opus_manager.cursor.get_beatkey()
            try {
                when (this.get_activity().configuration.link_mode) {
                    PaganConfiguration.LinkMode.LINK -> {
                        opus_manager.link_row(channel, line_offset, beat_key)
                    }
                    else -> {
                        opus_manager.overwrite_row(channel, line_offset, beat_key)
                    }
                }
            } catch (e: OpusLayerLinks.BadRowLink) {
                // TODO: Feedback
                //(this.context as MainActivity).feedback_msg("Can only row-link from first beat")
            }
            cursor.is_linking = false
            opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
        } else {
            opus_manager.cursor_select_row(channel, line_offset)
        }
    }

    private fun _set_colors() {
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map
        (this.background as LayerDrawable).findDrawableByLayerId(R.id.tintable_lines).setTint(color_map[Palette.Lines])
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
                    color_map[Palette.Selection],
                    color_map[Palette.ChannelOdd],
                    color_map[Palette.ChannelEven]
                )
            )
        )
        this.setTextColor(
            ColorStateList(
                states,
                intArrayOf(
                    color_map[Palette.SelectionText],
                    color_map[Palette.ChannelOddText],
                    color_map[Palette.ChannelEvenText]
                )
            )
        )
    }
}