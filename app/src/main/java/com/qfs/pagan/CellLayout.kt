package com.qfs.pagan

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.view.children
import com.qfs.pagan.opusmanager.BeatKey
import com.qfs.pagan.opusmanager.CtlLineLevel
import com.qfs.pagan.opusmanager.InstrumentEvent
import com.qfs.pagan.opusmanager.OpusControlEvent
import com.qfs.pagan.opusmanager.OpusEvent
import com.qfs.pagan.structure.OpusTree
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager

class CellLayout(private val _column_layout: ColumnLayout, val row: Int): LinearLayout(_column_layout.context) {
    init {
        this.isClickable = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        this.removeAllViews()
        val opus_manager = this.get_opus_manager()
        val (pointer, control_level, control_type) = try {
            opus_manager.get_ctl_line_info(opus_manager.get_ctl_line_from_row(this.row))
        } catch (e: NullPointerException) {
            return // Caused by an unfortunately timed refresh, shouldn't be a problem to ignore
        }

        this.layoutParams.height = WRAP_CONTENT
        val activity = this.get_activity()
        val color_map = activity.view_model.color_map
        this.setBackgroundColor(color_map[ColorMap.Palette.Lines])

        val width = (this._column_layout.column_width_factor * resources.getDimension(R.dimen.base_leaf_width).roundToInt())
        this.layoutParams.width = width

        val beat = this.get_beat()
        val tree = when (control_level) {
            CtlLineLevel.Line -> {
                val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                opus_manager.get_line_ctl_tree(
                    control_type!!,
                    BeatKey(channel, line_offset, beat),
                    listOf()
                )
            }
            CtlLineLevel.Channel -> {
                opus_manager.get_channel_ctl_tree(
                    control_type!!,
                    pointer,
                    beat,
                    listOf()
                )
            }
            CtlLineLevel.Global -> {
                // Kludge: Only works because Only one global control is in use (Tempo)
                this.setPadding(0, 20, 0, 0)

                opus_manager.get_global_ctl_tree(
                    control_type!!,
                    beat
                )
            }
            null -> {
                val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                if (channel != 0 && line_offset == 0) {
                    this.setPadding(0, 20, 0, 0)
                }
                this.get_beat_tree(BeatKey(channel, line_offset, beat))
            }
        }

        this.buildTreeView(
            tree as OpusTree<OpusEvent>,
            listOf(),
            listOf()
        )
    }

    fun invalidate_all() {
        val view_stack = mutableListOf<View>(this)
        while (view_stack.isNotEmpty()) {
            val current_view = view_stack.removeAt(0)
            if (current_view is ViewGroup) {
                for (child in current_view.children) {
                    view_stack.add(child)
                }
            }
            current_view.postInvalidate()
            current_view.refreshDrawableState()
        }
    }

    fun get_activity(): MainActivity {
        return this.context as MainActivity
    }

    fun get_opus_manager(): OpusManager {
        return this.get_activity().get_opus_manager()
    }

    fun is_control_line(): Boolean {
        val opus_manager = this.get_opus_manager()
        return this.get_opus_manager().ctl_line_level(
            opus_manager.get_ctl_line_from_row(this.row)
        ) != null
    }

    fun get_editor_table(): EditorTable {
        return this.get_activity().findViewById(R.id.etEditorTable)
    }

    private fun buildTreeView(tree: OpusTree<OpusEvent>, position: List<Int>, divisions: List<Int>) {
        if (tree.is_leaf()) {
            val opus_manager = this.get_opus_manager()
            val (pointer, control_level, control_type) = opus_manager.get_ctl_line_info(
                opus_manager.get_ctl_line_from_row(this.row)
            )

            val tvLeaf = when (control_level) {
                null -> {
                    LeafButtonStd(
                        this.context,
                        opus_manager.tuning_map.size,
                        tree.get_event() as InstrumentEvent?,
                        position
                    )
                }
                CtlLineLevel.Global -> {
                    LeafButtonCtlGlobal(
                        this.context,
                        tree.get_event() as OpusControlEvent?,
                        position,
                        control_type!!
                    )
                }
                CtlLineLevel.Channel -> {
                    LeafButtonCtlChannel(
                        this.context,
                        tree.get_event() as OpusControlEvent?,
                        pointer,
                        position,
                        control_type!!
                    )
                }
                CtlLineLevel.Line -> {
                    val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                    LeafButtonCtlLine(
                        this.context,
                        tree.get_event() as OpusControlEvent?,
                        channel,
                        line_offset,
                        position,
                        control_type!!
                    )
                }
            }

            this.addView(tvLeaf)

            (tvLeaf.layoutParams as LayoutParams).gravity = Gravity.CENTER
            (tvLeaf.layoutParams as LayoutParams).height = MATCH_PARENT
            var new_width_factor = this._column_layout.column_width_factor.toFloat()
            for (d in divisions) {
                new_width_factor /= d.toFloat()
            }

            (tvLeaf.layoutParams as LayoutParams).weight = new_width_factor
            (tvLeaf.layoutParams as LayoutParams).width = 0
            val base_leaf_width = resources.getDimension(R.dimen.base_leaf_width)
            tvLeaf.minimumWidth = base_leaf_width.roundToInt()
        } else {
            val new_divisions = divisions.toMutableList()
            new_divisions.add(tree.size)
            for (i in 0 until tree.size) {
                val new_position = position.toMutableList()
                new_position.add(i)

                this.buildTreeView(
                    tree[i],
                    new_position,
                    new_divisions
                )
            }
        }
    }

    fun get_coord(): EditorTable.Coordinate {
        return EditorTable.Coordinate(
            this.row,
            this.get_beat()
        )
    }

    fun get_beat(): Int {
        return (this.parent as ColumnLayout).get_beat()
    }

    private fun get_beat_tree(beat_key: BeatKey): OpusTree<out InstrumentEvent> {
        val opus_manager = this.get_opus_manager()
        return opus_manager.get_tree(beat_key)
    }
}
