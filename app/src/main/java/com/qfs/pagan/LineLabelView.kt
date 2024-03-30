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
import androidx.appcompat.widget.LinearLayoutCompat
import com.qfs.pagan.ColorMap.Palette
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.opusmanager.OpusLayerLinks
import com.qfs.pagan.opusmanager.OpusManagerCursor
import com.qfs.pagan.OpusLayerInterface as OpusManager

class LineLabelView(context: Context, var row: Int): LinearLayoutCompat(context) {
    open class LineLabelInner(context: Context): AppCompatTextView(context), View.OnTouchListener {
        init {
            this._set_colors()
            this.setOnClickListener {
                this.on_click()
            }
        }

        override fun onCreateDrawableState(extraSpace: Int): IntArray? {
            val drawableState = super.onCreateDrawableState(extraSpace + 2)
            return this._build_drawable_state(drawableState)
        }

        open fun _build_drawable_state(drawableState: IntArray?): IntArray? {
            return null
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            this.set_height()
            this.set_text()
        }

        open fun _set_colors() {}
        open fun get_height(): Float {
            return 0F
        }

        open fun get_label_text(): String {
            return "TODO"
        }

        open fun on_click() { }

        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            TODO("Not yet implemented")
        }

        private fun set_height() {
            this.layoutParams.height = this.get_height().toInt()
        }

        private fun set_text() {
            val text = this.get_label_text()
            this.text = text
            this.contentDescription = text
        }

        fun get_opus_manager(): OpusManager {
            return (this.parent as LineLabelView).get_opus_manager()
        }

        fun get_activity(): MainActivity {
            return (this.parent as LineLabelView).get_activity()
        }
    }

    class LineLabelStd(context: Context, var channel: Int, var line_offset: Int): LineLabelInner(ContextThemeWrapper(context, R.style.line_label)) {
        override fun _build_drawable_state(drawableState: IntArray?): IntArray? {
            if (this.parent == null) {
                return drawableState
            }

            val opus_manager = this.get_opus_manager()

            val new_state = mutableListOf<Int>()
            if (this.channel % 2 == 0 && !opus_manager.is_ctl_level_visible(CtlLineLevel.Line)) {
                new_state.add(R.attr.state_channel_even)
            }

            when (opus_manager.cursor.mode) {
                OpusManagerCursor.CursorMode.Single,
                OpusManagerCursor.CursorMode.Row -> {
                    if (opus_manager.cursor.channel == this.channel && opus_manager.cursor.line_offset == this.line_offset) {
                        new_state.add(R.attr.state_focused)
                    }
                }
                OpusManagerCursor.CursorMode.Range -> {
                    val (first, second) = opus_manager.cursor.range!!
                    if ((this.channel > first.channel && this.channel < second.channel) || (this.channel == first.channel && this.line_offset >= first.line_offset) || (this.channel == second.channel && this.line_offset <= second.line_offset)) {
                        new_state.add(R.attr.state_focused)
                    }
                }
                else -> { }

            }

            mergeDrawableStates(drawableState, new_state.toIntArray())
            return drawableState
        }

        override fun on_click() {
            val opus_manager = this.get_opus_manager()

            val cursor = opus_manager.cursor
            if (cursor.is_linking_range()) {
                val first_key = cursor.range!!.first
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
                            opus_manager.link_row(this.channel, this.line_offset, beat_key)
                        }
                        else -> {
                            opus_manager.overwrite_row(this.channel, this.line_offset, beat_key)
                        }
                    }
                } catch (e: OpusLayerLinks.BadRowLink) {
                    // TODO: Feedback
                    //(this.context as MainActivity).feedback_msg("Can only row-link from first beat")
                }
                cursor.is_linking = false
                opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
            } else {
                opus_manager.cursor_select_row(this.channel, this.line_offset)
            }
        }

        override fun _set_colors() {
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

        override fun get_label_text(): String {
            val opus_manager = this.get_opus_manager()

            return if (!opus_manager.is_percussion(this.channel)) {
                "${this.channel}::${this.line_offset}"
            } else {
                val instrument = opus_manager.get_percussion_instrument(this.line_offset)
                "!$instrument"
            }
        }

        override fun get_height(): Float {
            return this.resources.getDimension(R.dimen.line_height)
        }
    }

    open class LineLabelCtl(context: Context, var ctl_level: CtlLineLevel, var ctl_type: ControlEventType): LineLabelInner(ContextThemeWrapper(context, R.style.line_label)) {

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

    class LineLabelCtlLine(context: Context, ctl_type: ControlEventType, val channel: Int, val line_offset: Int): LineLabelCtl(context, CtlLineLevel.Line, ctl_type) {
        override fun on_click() {
            this.get_opus_manager().cursor_select_ctl_row_at_line(this.ctl_type, this.channel, this.line_offset)
        }
    }
    class LineLabelCtlChannel(context: Context, ctl_type: ControlEventType, val channel: Int): LineLabelCtl(context, CtlLineLevel.Channel, ctl_type) {
        override fun on_click() {
            this.get_opus_manager().cursor_select_ctl_row_at_channel(this.ctl_type, this.channel)

        }
    }
    class LineLabelCtlGlobal(context: Context, ctl_type: ControlEventType): LineLabelCtl(context, CtlLineLevel.Global, ctl_type) {
        override fun on_click() {
            this.get_opus_manager().cursor_select_ctl_row_at_global(this.ctl_type)
        }
    }

    init {
        this.setOnDragListener { view: View, dragEvent: DragEvent ->
            val adapter = (view.parent.parent as LineLabelColumnLayout)
            val opus_manager = this.get_opus_manager()
            val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(
                opus_manager.get_ctl_line_from_visible_row( this.row )
            )
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
        this.removeAllViews()
        val opus_manager = this.get_opus_manager()
        val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(
            opus_manager.get_ctl_line_from_visible_row(this.row)
        )
        this.addView(
            when (ctl_level) {
                null -> {
                    val (channel, line_offset) = opus_manager.get_std_offset(pointer)
                    LineLabelStd(this.context, channel, line_offset)
                }
                CtlLineLevel.Global -> {
                    LineLabelCtlGlobal(this.context, ctl_type!!)
                }
                CtlLineLevel.Channel -> {
                    LineLabelCtlChannel(this.context, ctl_type!!, pointer)
                }
                CtlLineLevel.Line -> {
                    val (channel, line_offset) = opus_manager.get_std_offset(pointer)
                    LineLabelCtlLine(this.context, ctl_type!!, channel, line_offset)
                }
            }
        )

        this.layoutParams.width = WRAP_CONTENT
        this.layoutParams.height = WRAP_CONTENT
    }

    fun get_opus_manager(): OpusManager {
        return (this.parent.parent as LineLabelColumnLayout).get_opus_manager()
    }

    //override fun onTouch(view: View?, touchEvent: MotionEvent?): Boolean {
    //    val column_layout = this.parent.parent as LineLabelColumnLayout

    //    val opus_manager = this.get_opus_manager()
    //    val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(
    //        opus_manager.get_ctl_line_from_visible_row( this.row )
    //    )

    //    // TODO; not sure what i'm going to do with this, might not be draggable
    //    if (ctl_level != null) {
    //        return false
    //    }

    //    return if (touchEvent == null) {
    //        true
    //    } else if (touchEvent.action == MotionEvent.ACTION_MOVE) {
    //        val (channel, line_offset) = opus_manager.get_std_offset(pointer)
    //        if (!column_layout.is_dragging()) {
    //            column_layout.set_dragging_line(channel, line_offset)
    //            (view as LineLabelView).startDragAndDrop(
    //                null,
    //                DragShadowBuilder(view),
    //                null,
    //                0
    //            )
    //        }
    //        true
    //    } else if (touchEvent.action == MotionEvent.ACTION_DOWN) {
    //        column_layout.stop_dragging()
    //        true
    //    } else {
    //        performClick()
    //    }
    //}

    fun get_activity(): MainActivity {
        return (this.context as ContextThemeWrapper).baseContext as MainActivity
    }

}