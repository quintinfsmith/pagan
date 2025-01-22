package com.qfs.pagan

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.LinearLayoutCompat
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.OpusLayerBase
import com.qfs.pagan.OpusLayerInterface as OpusManager

class LineLabelView(context: Context, var row: Int): LinearLayoutCompat(context) {
    init {

        this.setOnDragListener { view: View, dragEvent: DragEvent ->
            val adapter = (view.parent.parent as LineLabelColumnLayout)
            val opus_manager = this.get_opus_manager()
            val (pointer, ctl_level, _) = opus_manager.get_ctl_line_info(
                opus_manager.get_ctl_line_from_row( this.row )
            )

            if (ctl_level != null) {
                return@setOnDragListener true
            }

            when (dragEvent.action) {
                DragEvent.ACTION_DROP -> {
                    if (adapter.is_dragging()) {
                        val (from_channel, from_line) = adapter.dragging_position!!
                        val (to_channel, to_line) = opus_manager.get_channel_and_line_offset(pointer)
                        if (from_channel != to_channel || from_line != to_line) {
                            try {
                                opus_manager.swap_lines(
                                    from_channel,
                                    from_line,
                                    to_channel,
                                    to_line
                                )
                            } catch (e: OpusLayerBase.IncompatibleChannelException) {
                                this.get_activity().feedback_msg(context.getString(R.string.std_percussion_swap))
                            }
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
        this.set_inner_label()
        this.layoutParams.width = WRAP_CONTENT
        this.layoutParams.height = WRAP_CONTENT

    }

    private fun set_inner_label() {
        this.removeAllViews()
        val opus_manager = this.get_opus_manager()
        val ctl_line = opus_manager.get_ctl_line_from_row(this.row)
        val (pointer, ctl_level, ctl_type) = opus_manager.get_ctl_line_info(ctl_line)

        this.setBackgroundColor(resources.getColor(R.color.table_lines))
        this.addView(
            when (ctl_level) {
                null -> {
                    val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                    LineLabelStd(this.context, channel, line_offset)
                }
                CtlLineLevel.Global -> {
                    LineLabelCtlGlobal(this.context, ctl_type!!)
                }
                CtlLineLevel.Channel -> {
                    LineLabelCtlChannel(this.context, ctl_type!!, pointer)
                }
                CtlLineLevel.Line -> {
                    val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                    LineLabelCtlLine(this.context, ctl_type!!, channel, line_offset)
                }
            }
        )
    }

    fun get_opus_manager(): OpusManager {
        return (this.parent.parent as LineLabelColumnLayout).get_opus_manager()
    }

    fun reset_row(new_row: Int) {
        this.row = new_row

        val opus_manager = this.get_opus_manager()
        val (pointer, ctl_level, _) = opus_manager.get_ctl_line_info(
            opus_manager.get_ctl_line_from_row(this.row)
        )
        val channel_gap_size = context.resources.getDimension(R.dimen.channel_gap_size).toInt()
        if (ctl_level == null) {
            val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
            if (channel != 0 && line_offset == 0) {
                this.setPadding(0, channel_gap_size, 0, 0)
            } else {
                this.setPadding(0, 0, 0, 0)
            }
            // Kludge: Only works because Only one global control is in use (Tempo)
        } else if (ctl_level == CtlLineLevel.Global) {
            this.setPadding(0, channel_gap_size, 0, 0)
        } else {
            this.setPadding(0, 0, 0, 0)
        }

        this.set_inner_label()
    }

    fun get_activity(): MainActivity {
        var context = this.context
        while (context !is MainActivity) {
            context = (context as ContextThemeWrapper).baseContext
        }
        return context
    }

}