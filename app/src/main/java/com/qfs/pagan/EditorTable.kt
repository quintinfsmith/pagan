package com.qfs.pagan

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_MOVE
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.ScrollView
import android.widget.Space
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import com.qfs.pagan.Activity.ActivityEditor
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.AbsoluteNoteEvent
import com.qfs.pagan.structure.opusmanager.base.BeatKey
import com.qfs.pagan.structure.opusmanager.base.CtlLineLevel
import com.qfs.pagan.structure.opusmanager.base.InvalidMergeException
import com.qfs.pagan.structure.opusmanager.base.InvalidOverwriteCall
import com.qfs.pagan.structure.opusmanager.base.MixedInstrumentException
import com.qfs.pagan.structure.opusmanager.base.OpusEvent
import com.qfs.pagan.structure.opusmanager.base.OpusLayerBase
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.RangeOverflow
import com.qfs.pagan.structure.opusmanager.base.RelativeNoteEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectType
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent
import com.qfs.pagan.structure.opusmanager.cursor.CursorMode
import com.qfs.pagan.structure.opusmanager.cursor.InvalidCursorState
import com.qfs.pagan.structure.opusmanager.cursor.OpusManagerCursor
import com.qfs.pagan.structure.rationaltree.ReducibleTree
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import com.qfs.pagan.OpusLayerInterface as OpusManager

class EditorTable(context: Context, attrs: AttributeSet): ScrollView(context, attrs) {
    data class Coordinate(var y: Int, var x: Int)
    data class Rectangle(var x: Int, var y: Int, var width: Int, var height: Int)

    private val _column_width_map = mutableListOf<MutableList<Int>>()
    private val _column_width_maxes = mutableListOf<Int>()
    val _inv_column_map = HashMap<Int, Int>() // x position by number of leaf-widths:: actual column
    //private val _row_height_map = mutableListOf<Int>()

    var scale_factor = 1F
    private val scale_gesture_detector = ScaleGestureDetector(
        this.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                this@EditorTable.scale_factor *= detector.scaleFactor

                this@EditorTable.scale_factor = max(0.1f, min(this@EditorTable.scale_factor, 1f))
                // this@EditorTable.painted_layer.set_text_scale(this@EditorTable.scale_factor)
                // this@EditorTable.editor_table.reset_table_size()
                this@EditorTable.painted_layer.invalidate()
                return true
            }
        }
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        this.inner_scroll_view.overScrollMode = OVER_SCROLL_NEVER
        this.inner_scroll_view.isHorizontalScrollBarEnabled = false

        // Add padding layer so we can scroll the bottom of the table to the middle of the screen
        val padding_layer = LinearLayout(this.context)
        padding_layer.orientation = VERTICAL
        val padder_bottom = Space(this.context)

        padding_layer.addView(this.painted_layer)
        padding_layer.addView(padder_bottom)

        val activity = this.get_activity()
        padder_bottom.layoutParams.height = activity.get_bottom_padding()

        this.inner_scroll_view.addView(padding_layer)
        this.addView(this.inner_scroll_view)

        this.inner_scroll_view.layoutParams.height = MATCH_PARENT
        this.inner_scroll_view.layoutParams.width = WRAP_CONTENT

        this.overScrollMode = OVER_SCROLL_NEVER
        this.isVerticalScrollBarEnabled = false

        this.update_global_ctl_button()
    }

    fun get_column_from_leaf(x: Int): Int? {
        return this._inv_column_map[x]
    }

    fun get_visible_row_from_pixel(y: Float): Int? {
        val line_height = floor(this.resources.getDimension(R.dimen.line_height))
        val ctl_line_height = floor(this.resources.getDimension(R.dimen.ctl_line_height))
        val channel_gap_size = floor(this.resources.getDimension(R.dimen.channel_gap_size))
        var output = 0
        val opus_manager = this.get_opus_manager()
        val channels = opus_manager.get_all_channels()

        if (y - this.scrollY < line_height) return -1

        var check_y = line_height // consider column labels
        for (i in channels.indices) {
            val channel = channels[i]
            for (j in channel.lines.indices) {
                val line = channel.lines[j]

                check_y += line_height
                if (check_y >= y) return output

                output += 1

                for ((_, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue

                    check_y += ctl_line_height
                    if (check_y >= y) return output

                    output += 1
                }
            }

            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue

                check_y += ctl_line_height
                if (check_y >= y) return output

                output += 1
            }

            check_y += channel_gap_size
            if (check_y >= y) return null
        }

        for ((_, controller) in opus_manager.controllers.get_all()) {
            if (!controller.visible) continue

            check_y += ctl_line_height
            if (check_y >= y) return output

            output += 1
        }

        return -2
    }

    fun clear() {
        this.get_activity().runOnUiThread {
            this.reset_table_size()

            this.inner_scroll_view.scrollX = 0
            this.inner_scroll_view.scrollY = 0
            this.scrollX = 0
            this.scrollY = 0
            this.painted_layer.clear()
        }
    }

    fun setup(height: Int, width: Int) {
        // NOTE: Needs column map initialized first
        this.add_columns(0, width)
        this.reset_table_size()
    }

    fun reset_table_size() {
        val (pix_width, pix_height) = this._calculate_table_size()
        // Include extra line of height for the top row, and for the bottom global ctl button
        // and line label width for the label column
        val line_height = this.resources.getDimension(R.dimen.line_height).toInt()
        this.set_size(
            pix_width + this.resources.getDimension(R.dimen.line_label_width).toInt(),
            pix_height + line_height
        )
    }

    fun new_row(y: Int) {
        this.reset_table_size()
        this.insert_row(y)
    }

    fun remove_rows(y: Int, count: Int) {
        this.reset_table_size()
        this.painted_layer.remove_rows(y, count)
    }

    fun new_column(index: Int) {
        this.reset_table_size()
        this.add_column(index)
    }

    fun remove_column(index: Int) {
        this.reset_table_size()
        this.painted_layer.remove_column(index)
    }

    fun notify_cell_state_changes(cell_coords: List<Coordinate>) {
        // TODO: This may need optimization
        for (coord in cell_coords) {
            this.notify_cell_changed(coord, null)
        }
    }
    fun notify_cell_changes(cell_coords: List<Pair<ReducibleTree<*>,Coordinate>>) {
        // TODO: This may need optimization
        for ((rtree, coord) in cell_coords) {
            this.notify_cell_changed(coord, rtree)
        }
    }

    fun notify_column_changed(x: Int, state_only: Boolean = false) {
        if (!state_only) {
            this.reset_table_size()
        }
        this.painted_layer.notify_column_changed(x, state_only)
    }

    // FIXME: Redundant
    fun notify_row_change(y: Int, percussion: Boolean, channel: Int, offset: Int, level: CtlLineLevel? = null, type: EffectType? = null) {
        // if (!state_only) {
        //     this.reset_table_size()
        // }
        this.painted_layer.notify_row_change(y)
    }

    fun get_column_map_size(): Int {
        return this._column_width_map.size
    }

    fun recalculate_column_max(x: Int) {
        this._column_width_maxes[x] = this._column_width_map[x].max()
        this._update_inv_column_map(x)
    }

    fun recalculate_column_maxes(): List<Int> {
        val output = mutableListOf<Int>()
        for (x in 0 until this._column_width_map.size) {
            val new_max = this._column_width_map[x].max()
            if (new_max != this._column_width_maxes[x]) {
                output.add(new_max)
                this._column_width_maxes[x] = new_max
            }
        }
        if (output.isNotEmpty()) {
            this._update_inv_column_map(output.min())
        }
        return output
    }

    fun get_column_width(column: Int): Int {
        /**
         *  TODO: I just noticed that this failing is required for the correct formatting of the table.
         *      This needs to change.
         */
        return this._column_width_maxes[column]
    }

    fun get_activity(): ActivityEditor {
        return this.context as ActivityEditor
    }

    fun get_opus_manager(): OpusManager {
        return (this.context as ActivityEditor).get_opus_manager()
    }

    fun update_column_label(x: Int) {
        // TODO?
        //this.column_label_container.notify_column_changed(x)
    }

    fun scroll_to_position(x: Int? = null, y: Int? = null, offset: Float = 0f, offset_width: Float = 1f, force: Boolean = false) {
        x?.let {
            if (it >= this.get_opus_manager().length) {
                return
            } else if (! force) {
                this._scroll_to_x(it, offset, offset_width)
            } else {
                this._forced_scroll_to_beat(it)
            }
        }

        y?.let { this._scroll_to_y(it) }
    }

    private fun _forced_scroll_to_beat(x: Int) {
        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        val pixel_x = this._column_width_maxes.subList(0, x).sum() * base_width.toInt()
        this.scroll(pixel_x, null)
    }

    /**
     * Given an offset [x] in pixels, get the
     */
    fun get_column_offset(x: Int): Int {
        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        return (this._column_width_maxes.subList(0, x).sum() * base_width.toInt())
    }

    fun get_column_rect(x: Int): Rectangle? {
        if (this._column_width_maxes.size <= x) {
            return null
        }

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        return Rectangle(
            this.get_column_offset(x),
            0,
            this._column_width_maxes[x] * base_width.toInt(),
            1 // TODO
        )
    }

    fun get_scroll_x(): Int {
        return this.inner_scroll_view.scrollX
    }

    fun get_scroll_y(): Int {
        return this.scrollY
    }

    private fun _scroll_to_x(x: Int, offset: Float = 0F, offset_width: Float = 1F) {
        val line_label_width = this.resources.getDimension(R.dimen.line_label_width).toInt()
        val box_width = this.inner_scroll_view.measuredWidth - line_label_width

        val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
        val max_width = (this._column_width_maxes[x] * base_width).toInt()
        val target_width = (this._column_width_maxes[x] * this.resources.getDimension(R.dimen.base_leaf_width) * offset_width).toInt()
        val visible_range = this.get_first_visible_column_index() .. this.get_last_visible_column_index()
        val target_offset = (max_width * offset).toInt()
        val POSITION_ON_SCREEN: Int = 0
        val POSITION_TO_RIGHT: Int = 1
        val POSITION_TO_LEFT: Int = 2
        val FITS_ON_SCREEN: Int = 3

        val column_state = Array(4) { false }
        val subdiv_state = Array(4) { false }

        subdiv_state[FITS_ON_SCREEN] = target_width <= box_width
        column_state[FITS_ON_SCREEN] = max_width <= box_width

        val scroll_x = this.inner_scroll_view.scrollX + line_label_width // account for the line label being on the same plane as the rest of the table
        val target_rect = this.get_column_rect(x) ?: return
        if (x in visible_range) {
            if (target_rect.x + target_width + target_offset > box_width + scroll_x) {
                subdiv_state[POSITION_TO_RIGHT] = true
                subdiv_state[POSITION_ON_SCREEN] = target_rect.x + target_offset < box_width + scroll_x
                subdiv_state[POSITION_TO_LEFT] = target_rect.x + target_offset < scroll_x
            } else if (target_rect.x + target_width + target_offset > scroll_x) {
                subdiv_state[POSITION_ON_SCREEN] = true
                subdiv_state[POSITION_TO_LEFT] = target_rect.x + target_offset < scroll_x
            } else {
                subdiv_state[POSITION_TO_LEFT] = true
            }


            if (target_rect.x > box_width + scroll_x) {
                column_state[POSITION_TO_RIGHT] = true
            } else if (target_rect.x > scroll_x) {
                column_state[POSITION_ON_SCREEN] = true
                column_state[POSITION_TO_RIGHT] = target_rect.x + max_width > box_width + scroll_x
            } else {
                column_state[POSITION_TO_LEFT] = true
                column_state[POSITION_ON_SCREEN] = target_rect.x + max_width > scroll_x
                column_state[POSITION_TO_RIGHT] = target_rect.x + max_width > box_width + scroll_x
            }
        } else if (x > visible_range.last) {
            column_state[POSITION_TO_RIGHT] = true
            subdiv_state[POSITION_TO_RIGHT] = true
        } else {
            column_state[POSITION_TO_LEFT] = true
            subdiv_state[POSITION_TO_LEFT] = true
        }

        var subdiv_int = 0
        var column_int = 0
        var working_offset = 1
        for (i in 0 until 4) {
            if (subdiv_state[i]) {
                subdiv_int += working_offset
            }
            if (column_state[i]) {
                column_int += working_offset
            }
            working_offset *= 2
        }

        // FITS, LEFT, RIGHT, ON SCREEN
        val adj_offset = when (subdiv_int) {
            // Center the section
            0b0011,
            0b0101,
            0b0010,
            0b0100 -> (box_width - target_width) / 2

            // Try to scroll the column onto screen, then the section
            0b1010 -> {
                if (column_state[FITS_ON_SCREEN]) {
                    box_width - max_width
                } else {
                    (0 - target_offset) + ((box_width - target_width) / 2)
                }
            }

            // Align the end of the section with the end of the screen
            0b1011 -> box_width - target_offset - target_width

            // Try to scroll the column onto screen, then the section
            0b1100 -> {
                if (column_state[FITS_ON_SCREEN]) {
                    0
                } else {
                    box_width - target_offset - target_width - ((box_width - target_width) / 2)
                }
            }

            // Align the start of the section with the start of the screen
            0b1101 -> 0 - target_offset

            0b0111,   // Overflowing,
            0b1001 -> return // No need to scroll
            // 0b0000 -> { }   // Invalid
            // 0b0001 -> { }   // Invalid
            // 0b0110 -> { }   // Invalid
            // 0b1000 -> { }   // Invalid
            // 0b1110 -> { }   // Invalid
            // 0b1111 -> { }   // Invalid
            else -> return     // Unreachable
        }


        this.scroll(target_rect.x - adj_offset, null)
    }

    fun get_row_y_position_and_height(row: Int): Pair<Int, Int> {
        val channel_gap_size = this.resources.getDimension(R.dimen.channel_gap_size).toInt()
        val controller_height = this.resources.getDimension(R.dimen.ctl_line_height).toInt()
        val line_height = this.resources.getDimension(R.dimen.line_height).toInt()

        val opus_manager = this.get_opus_manager()
        val channels = opus_manager.get_all_channels()
        var y = 0
        var working_y_offset = line_height // consider column label offset
        for (i in channels.indices) {
            val channel = channels[i]
            for (j in channel.lines.indices) {
                val line = channel.lines[j]
                if (y == row) {
                    return Pair(working_y_offset, line_height)
                } else {
                    working_y_offset += line_height
                    y += 1
                }

                for ((_, controller) in line.controllers.get_all()) {
                    if (!controller.visible) {
                        continue
                    }
                    if (y == row) {
                        return Pair(working_y_offset, controller_height)
                    } else {
                        working_y_offset += controller_height
                        y += 1
                    }
                }
            }
            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                if (y == row) {
                    return Pair(working_y_offset, controller_height)
                } else {
                    working_y_offset += controller_height
                    y += 1
                }
            }
            working_y_offset += channel_gap_size
        }

        for ((_, controller) in opus_manager.controllers.get_all()) {
            if (!controller.visible) continue
            if (y == row) {
                return Pair(working_y_offset, controller_height)
            } else {
                working_y_offset += controller_height
                y += 1
            }
        }
        return Pair(working_y_offset, 0)
    }

    fun force_scroll_to_cursor_vertical() {
        val opus_manager = this.get_opus_manager()
        val cursor = opus_manager.cursor

        val row = when (cursor.mode) {
            CursorMode.Single,
            CursorMode.Line -> {
                when (cursor.ctl_level) {
                    null -> opus_manager.get_visible_row_from_ctl_line(
                        opus_manager.get_actual_line_index(
                            opus_manager.get_instrument_line_index(
                                cursor.channel,
                                cursor.line_offset
                            )
                        )
                    )

                    CtlLineLevel.Line -> {
                        opus_manager.get_visible_row_from_ctl_line_line(
                            cursor.ctl_type!!,
                            cursor.channel,
                            cursor.line_offset
                        )
                    }
                    CtlLineLevel.Channel -> {
                        opus_manager.get_visible_row_from_ctl_line_channel(
                            cursor.ctl_type!!,
                            cursor.channel
                        )
                    }
                    CtlLineLevel.Global -> {
                        opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                    }
                }
            }

            CursorMode.Range -> {
                when (cursor.ctl_level) {
                    null -> {
                        val (first, second) = cursor.get_ordered_range()!!
                        opus_manager.get_visible_row_from_ctl_line(
                            opus_manager.get_actual_line_index(
                                max(
                                    opus_manager.get_instrument_line_index(first.channel, first.line_offset),
                                    opus_manager.get_instrument_line_index(second.channel, second.line_offset)
                                )
                            )
                        )
                    }

                    CtlLineLevel.Line -> opus_manager.get_visible_row_from_ctl_line_line(cursor.ctl_type!!, cursor.range!!.second.channel, cursor.range!!.second.line_offset)
                    CtlLineLevel.Channel -> opus_manager.get_visible_row_from_ctl_line_channel(cursor.ctl_type!!, cursor.range!!.second.channel)
                    CtlLineLevel.Global -> opus_manager.get_visible_row_from_ctl_line_global(cursor.ctl_type!!)
                }
            }

            CursorMode.Channel-> {
                opus_manager.get_visible_row_from_ctl_line(
                    opus_manager.get_actual_line_index(
                        opus_manager.get_instrument_line_index(
                            cursor.channel,
                            opus_manager.get_channel(cursor.channel).lines.size - 1
                        )
                    )
                )
            }
            // No need to force_scroll in these modes
            CursorMode.Column,
            CursorMode.Unset -> null
        } ?: return

        this._scroll_to_y(row)
    }

    private fun _scroll_to_y(row: Int) {
        val (target_y, row_height) = this.get_row_y_position_and_height(row)
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val context_menu_top = if (opus_manager.cursor.mode == CursorMode.Unset) {
            this.measuredHeight
        } else {
            when (this.resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    val secondary = activity.findViewById<View>(R.id.llContextMenuSecondary)
                    if (secondary.isGone) {
                        this.measuredHeight
                    } else {
                        secondary.y
                    }
                }
                else -> activity.findViewById<View>(R.id.llContextMenuPrimary).y
            }
        }.toInt()

        // kludge: view hasn't been measured yet. skip.
        if (context_menu_top == 0) return

        if (context_menu_top + this.scrollY < target_y + row_height) {
            val adj_y = (target_y + row_height) - context_menu_top.toInt()
            this.scroll(null, adj_y)
        } else if (target_y < this.scrollY) {
            val line_height = this.resources.getDimension(R.dimen.line_height).toInt()
            this.scroll(null, target_y - line_height)
        }
    }

    fun get_scroll_offset(): Pair<Int, Int> {
        // NOTE: Used to be based on recycler view positions. So now we just set the Pairs to Pair(0, scroll[X/Y])
        return Pair(
            this.inner_scroll_view.scrollX,
            this.scrollY
        )
    }

    fun precise_scroll(scroll_x: Int = 0, scroll_y: Int? = null) {
        this.scroll(scroll_x, scroll_y)
    }

    fun get_first_visible_column_index(): Int {
        val line_label_width = this.resources.getDimension(R.dimen.line_label_width).toInt()
        val scroll_container_offset = this.inner_scroll_view.scrollX - line_label_width
        val min_leaf_width = this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x) ?: 0
        return column_position
    }

    fun get_last_visible_column_index(): Int {
        val line_label_width = this.resources.getDimension(R.dimen.line_label_width).toInt()
        val scroll_container_offset = this.get_scroll_x_max() - line_label_width
        val min_leaf_width = this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        val reduced_x = scroll_container_offset / min_leaf_width
        val column_position = this.get_column_from_leaf(reduced_x) ?: (this._column_width_map.size - 1)
        return column_position
    }

    // width map functions ---------------------------------------
    fun clear_column_map() {
        this._column_width_map.clear()
        this._column_width_maxes.clear()
        this._inv_column_map.clear()
    }

    fun swap_mapped_lines(line_a: Int, line_b: Int) {
        for (i in 0 until this._column_width_map.size) {
            val tmp = this._column_width_map[i][line_a]
            this._column_width_map[i][line_a] = this._column_width_map[i][line_b]
            this._column_width_map[i][line_b] = tmp
        }
    }

    fun swap_mapped_channels(line_a: Int, count_a: Int, line_b: Int, count_b: Int) {
        val (first, second) = if (line_a < line_b) {
            Pair(
                Pair(line_a, count_a),
                Pair(line_b, count_b)
            )
        } else {
            Pair(
                Pair(line_b, count_b),
                Pair(line_a, count_a)
            )
        }

        for (i in 0 until second.second) {
            for (column in this._column_width_map) {
                val elm = column.removeAt(second.first + i)
                column.add(first.first + i, elm)
            }
        }

        for (i in 0 until first.second) {
            for (column in this._column_width_map) {
                val elm = column.removeAt(first.first + second.second)
                column.add(second.first + second.second - 1, elm)
            }
        }
    }

    fun remove_mapped_lines(y: Int, count: Int): List<Int> {
        val output = mutableListOf<Int>()
        for (j in 0 until this._column_width_map.size) {
            this._column_width_map[j] = this._column_width_map[j].filterIndexed { i: Int, _: Int ->
                !(i >= y && i < y + count)
            }.toMutableList()

            val new_max = this._column_width_map[j].max()
            if (new_max != this._column_width_maxes[j]) {
                output.add(j)
                this._column_width_maxes[j] = this._column_width_map[j].max()
            }
        }
        if (output.isNotEmpty()) {
            this._update_inv_column_map(output.min())
        }
        return output
    }

    fun remove_mapped_column(x: Int) {
        this._column_width_map.removeAt(x)
        this._column_width_maxes.removeAt(x)
        this._update_inv_column_map(x)
    }

    fun add_column_to_map(x: Int, column: List<Int>) {
        this._column_width_map.add(x, column.toMutableList())
        this._column_width_maxes.add(x, if (column.isNotEmpty()) {
            column.max()
        } else {
            1
        })

        this._update_inv_column_map(x)
    }

    private fun _update_inv_column_map(x: Int = 0) {
        var working_leaf_x = this._column_width_maxes.subList(0, x).sum()

        for (k in (this._inv_column_map.keys).toList()) {
            if (k >= working_leaf_x) {
                this._inv_column_map.remove(k)!!
            }
        }

        for (column in x until this._column_width_maxes.size) {
            val w = this._column_width_maxes[column]
            for (i in 0 until w) {
                this._inv_column_map[working_leaf_x++] = column
            }
        }
    }

    fun add_line_to_map(y: Int, line: List<Int>): List<Int> {
        val output = mutableListOf<Int>()
        for (x in line.indices) {
            this._column_width_map[x].add(y, line[x])
            val new_max = this._column_width_map[x].max()
            if (new_max != this._column_width_maxes[x]) {
                this._column_width_maxes[x] = new_max
                output.add(x)
            }
        }
        if (output.isNotEmpty()) {
            this._update_inv_column_map(output.min())
        }
        return output
    }

    fun _calculate_table_size(): Pair<Int, Int> {
        val base_width = this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        val channel_gap_size = this.resources.getDimension(R.dimen.channel_gap_size).toInt()
        val controller_height = this.resources.getDimension(R.dimen.ctl_line_height).toInt()
        val line_height = this.resources.getDimension(R.dimen.line_height).toInt()

        val size = if (this._inv_column_map.keys.isNotEmpty()) {
            this._inv_column_map.keys.max() + 1
        } else {
            0
        }

        val width = size * base_width
        var vis_channel_count = 0
        var controller_count = 0
        var line_count = 0

        val opus_manager = this.get_opus_manager()
        for (channel in opus_manager.get_all_channels()) {
            for (line in channel.lines) {
                line_count += 1
                for ((_, controller) in line.controllers.get_all()) {
                    if (controller.visible) {
                        controller_count += 1
                    }
                }
            }
            vis_channel_count += 1
            for ((_, controller) in channel.controllers.get_all()) {
                if (controller.visible) {
                    controller_count += 1
                }
            }
        }

        for ((_, controller) in opus_manager.controllers.get_all()) {
            if (controller.visible) {
                controller_count += 1
            }
        }

        val height = (vis_channel_count * channel_gap_size) + (line_count * line_height) + (controller_count * controller_height)
        return Pair(width, height)
    }

    fun get_column_width_map(): List<List<Int>> {
        return this._column_width_map
    }


    /////////////////////////////////////////////////

    val cell_map = HashMap<EditorTable.Coordinate, ReducibleTree<*>>()

    enum class RowType {
        Top,
        UI,
        Bottom
    }
    class PaintedLayer(val table_ui: EditorTable): View(table_ui.context) {
        val section_radius_x = 20F
        val section_radius_y = 10F
        val table_line_paint = Paint()
        val text_paint_offset = Paint()
        val text_paint_octave = Paint()
        val text_paint_ctl = Paint()
        val text_paint_column = Paint()
        val text_paint_line_label_std = Paint()
        val text_paint_line_label_ctl = Paint()
        val tagged_paint_column = Paint()
        val leaf_drawable: Drawable
        val line_label_drawable: Drawable
        val ctl_label_drawable: Drawable
        val corner_drawable: Drawable

        val drag_highlight_paint = Paint()

        var invalidate_queued = false
        init {
            val typed_value = TypedValue()
            this.context.theme.resolveAttribute(R.attr.leaf, typed_value, true)
            this.leaf_drawable = ContextCompat.getDrawable(this.context, typed_value.resourceId) ?: throw Exception("TODO")

            this.context.theme.resolveAttribute(R.attr.line_label, typed_value, true)
            this.line_label_drawable = ContextCompat.getDrawable(this.context, typed_value.resourceId) ?: throw Exception("TODO")

            this.context.theme.resolveAttribute(R.attr.ctl_line_label, typed_value, true)
            this.ctl_label_drawable = ContextCompat.getDrawable(this.context, typed_value.resourceId) ?: throw Exception("TODO")

            this.context.theme.resolveAttribute(R.attr.corner_button, typed_value, true)
            this.corner_drawable = ContextCompat.getDrawable(this.context, typed_value.resourceId) ?: throw Exception("TODO")

            this.context.theme.resolveAttribute(R.attr.font_main, typed_value, true)
            val font = ResourcesCompat.getFont(this.context, typed_value.resourceId)

            this.table_line_paint.color = ContextCompat.getColor(this.context, R.color.table_lines)
            this.table_line_paint.strokeWidth = this.context.resources.getDimension(R.dimen.stroke_leaf)

            this.text_paint_line_label_std.textSize = this.resources.getDimension(R.dimen.text_size_line_label)
            this.text_paint_line_label_std.color = ContextCompat.getColor(this.context, R.color.line_label_text)
            this.text_paint_line_label_std.isAntiAlias = true
            this.text_paint_line_label_std.typeface = font

            this.text_paint_line_label_ctl.textSize = this.resources.getDimension(R.dimen.text_size_line_label)
            this.text_paint_line_label_ctl.color = ContextCompat.getColor(this.context, R.color.line_label_text)
            this.text_paint_line_label_ctl.isAntiAlias = true
            this.text_paint_line_label_ctl.typeface = font

            this.text_paint_offset.textSize = this.resources.getDimension(R.dimen.text_size_offset)
            this.text_paint_offset.color = ContextCompat.getColor(this.context, R.color.leaf_text_selector)
            this.text_paint_offset.isFakeBoldText = true
            this.text_paint_offset.isAntiAlias = true
            this.text_paint_offset.typeface = font

            this.text_paint_octave.textSize = this.resources.getDimension(R.dimen.text_size_octave)
            this.text_paint_octave.color = ContextCompat.getColor(this.context, R.color.leaf_text_selector)
            this.text_paint_octave.isAntiAlias = true
            this.text_paint_octave.typeface = font

            this.text_paint_ctl.textSize = this.resources.getDimension(R.dimen.text_size_ctl)
            this.text_paint_ctl.color = ContextCompat.getColor(this.context, R.color.ctl_leaf_text_selector)
            this.text_paint_ctl.isAntiAlias = true
            this.text_paint_ctl.typeface = font

            this.text_paint_column.isFakeBoldText = true
            this.text_paint_column.isAntiAlias = true
            this.text_paint_column.strokeWidth = 3F
            this.text_paint_column.textSize = this.resources.getDimension(R.dimen.text_size_octave)
            this.text_paint_column.typeface = font

            this.tagged_paint_column.style = Paint.Style.STROKE
            this.tagged_paint_column.strokeWidth = 3F
            this.tagged_paint_column.isAntiAlias = true
            this.tagged_paint_column.isDither = true

            this.drag_highlight_paint.color = Color.YELLOW
            this.drag_highlight_paint.strokeWidth = 8f

            this.setWillNotDraw(false)

            this.setOnTouchListener { view: View?, motion_event: MotionEvent? ->
                this.table_ui.set_touch_position(motion_event)
                false
            }

            this.setOnClickListener {
                this.table_ui._drag_handle.clear()
                val (row_type, line_info, beat_position) = this.table_ui._get_current_line_info_and_position(this.table_ui.touch_position_x, this.table_ui.touch_position_y) ?: return@setOnClickListener
                val (beat, position) = beat_position
                this.table_ui.on_click(row_type,line_info, beat, position)
            }

            this.setOnLongClickListener {
                this.table_ui._drag_handle.clear()
                val (row_type, line_info, beat_position) = this.table_ui._get_current_line_info_and_position(this.table_ui.touch_position_x, this.table_ui.touch_position_y) ?: return@setOnLongClickListener false
                val (beat, position) = beat_position
                this.table_ui.on_long_click(row_type, line_info, beat, position)
            }
        }

        fun get_standard_line_state(channel: Int, line_offset: Int): IntArray {
            val opus_manager = this.table_ui.get_opus_manager()

            val new_state = mutableListOf<Int>()
            if (opus_manager.is_line_selected(channel, line_offset)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_line_selected_secondary(channel, line_offset)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            if (opus_manager.get_channel(channel).get_line(line_offset).muted) {
                new_state.add(R.attr.state_muted)
            }

            return new_state.toIntArray()
        }

        fun get_standard_leaf_state(beat_key: BeatKey, position: List<Int>): IntArray {
            val opus_manager = this.table_ui.get_opus_manager()

            val tree = opus_manager.get_tree(beat_key, position)
            val original_position = opus_manager.get_actual_position(beat_key, position)
            val tree_original = opus_manager.get_tree(original_position.first, original_position.second)

            val new_state = mutableListOf<Int>()
            if (tree.has_event()) {
                new_state.add(R.attr.state_active)
                val match_cursor = OpusManagerCursor(
                    CursorMode.Single,
                    original_position.first.channel,
                    original_position.first.line_offset,
                    original_position.first.beat,
                    original_position.second
                )

                if (opus_manager.temporary_blocker == match_cursor) {
                    new_state.add(R.attr.state_invalid)
                } else {
                    when (tree.get_event()) {
                        is RelativeNoteEvent -> {
                            val abs_value = opus_manager.get_absolute_value(beat_key, position)
                            if (abs_value == null || abs_value < 0 || abs_value >= opus_manager.tuning_map.size * 8) {
                                new_state.add(R.attr.state_invalid)
                            }
                        }
                        else -> {}
                    }
                }
            } else if (tree_original != tree) {
                when (tree_original.get_event()) {
                    is RelativeNoteEvent -> {
                        val abs_value = opus_manager.get_absolute_value(original_position.first, original_position.second)
                        if (abs_value == null || abs_value < 0 || abs_value >= opus_manager.tuning_map.size * 8) {
                            new_state.add(R.attr.state_invalid)
                        }
                    }
                    else -> {}
                }
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_selected(beat_key, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_secondary_selection(beat_key, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            if (beat_key.channel % 2 == 0) {
                new_state.add(R.attr.state_alternate)
            }

            val working_channel = opus_manager.get_channel(beat_key.channel)
            if (working_channel.muted || working_channel.get_line(beat_key.line_offset).muted) {
                new_state.add(R.attr.state_muted)
            }


            return new_state.toIntArray()
        }

        fun get_global_control_leaf_state(type: EffectType, beat: Int, position: List<Int>): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.table_ui.get_opus_manager()
            val controller = opus_manager.get_controller<EffectEvent>(type)

            val tree = controller.get_tree(beat, position)
            val original_position = controller.get_blocking_position(beat, position) ?: Pair(beat, position)
            val tree_original = controller.get_tree(original_position.first, original_position.second)


            if (tree.has_event()) {
                val match_cursor = OpusManagerCursor(
                    mode = CursorMode.Single,
                    ctl_type = type,
                    ctl_level = CtlLineLevel.Global,
                    beat = original_position.first,
                    position = original_position.second
                )

                if (opus_manager.temporary_blocker == match_cursor) {
                    new_state.add(R.attr.state_invalid)
                }
                new_state.add(R.attr.state_active)
            } else if (tree_original != tree) {
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_global_control_selected(type, beat, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_global_control_secondary_selected(type, beat, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            return new_state.toIntArray()
        }

        fun get_global_control_line_state(type: EffectType): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.table_ui.get_opus_manager()

            if (opus_manager.is_global_control_line_selected(type)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_global_control_line_selected_secondary(type)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            new_state.add(R.attr.state_channel_even)

            return new_state.toIntArray()
        }

        fun get_channel_control_leaf_state(type: EffectType, channel: Int, beat: Int, position: List<Int>): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.table_ui.get_opus_manager()
            val controller = opus_manager.get_all_channels()[channel].get_controller<EffectEvent>(type)

            val tree = controller.get_tree(beat, position)
            val original_position = controller.get_blocking_position(beat, position) ?: Pair(beat, position)
            val tree_original = controller.get_tree(original_position.first, original_position.second)


            if (tree.has_event()) {
                val match_cursor = OpusManagerCursor(
                    mode = CursorMode.Single,
                    ctl_type = type,
                    ctl_level = CtlLineLevel.Channel,
                    channel = channel,
                    beat = original_position.first,
                    position = original_position.second
                )

                if (opus_manager.temporary_blocker == match_cursor) {
                    new_state.add(R.attr.state_invalid)
                }
                new_state.add(R.attr.state_active)
            } else if (tree_original != tree) {
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_channel_control_selected(type, channel, beat, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_channel_control_secondary_selected(type, channel, beat, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            if (opus_manager.get_channel(channel).muted) {
                new_state.add(R.attr.state_muted)
            }

            return new_state.toIntArray()
        }

        fun get_channel_control_line_state(type: EffectType, channel: Int): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.table_ui.get_opus_manager()
            if (opus_manager.is_channel_control_line_selected(type, channel)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_channel_control_line_selected_secondary(type, channel)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            if (opus_manager.channels[channel].muted) {
                new_state.add(R.attr.state_muted)
            }

            return new_state.toIntArray()
        }

        fun get_line_control_leaf_state(type: EffectType, beat_key: BeatKey, position: List<Int>): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.table_ui.get_opus_manager()
            val controller = opus_manager.get_line_controller<EffectEvent>(type, beat_key.channel, beat_key.line_offset)

            val beat = beat_key.beat
            val tree = controller.get_tree(beat, position)
            val original_position = controller.get_blocking_position(beat, position) ?: Pair(beat, position)
            val tree_original = controller.get_tree(original_position.first, original_position.second)

            if (tree.has_event()) {
                val match_cursor = OpusManagerCursor(
                    mode = CursorMode.Single,
                    ctl_type = type,
                    ctl_level = CtlLineLevel.Line,
                    channel = beat_key.channel,
                    line_offset = beat_key.line_offset,
                    beat = original_position.first,
                    position = original_position.second
                )

                if (opus_manager.temporary_blocker == match_cursor) {
                    new_state.add(R.attr.state_invalid)
                }

                new_state.add(R.attr.state_active)
            } else if (tree_original != tree) {
                new_state.add(R.attr.state_spill)
            }

            if (opus_manager.is_line_control_selected(type, beat_key, position)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_line_control_secondary_selected(type, beat_key, position)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            val channel = opus_manager.get_channel(beat_key.channel)
            if (channel.muted || channel.get_line(beat_key.line_offset).muted) {
                new_state.add(R.attr.state_muted)
            }

            return new_state.toIntArray()
        }

        fun get_line_control_line_state(type: EffectType, channel: Int, line_offset: Int): IntArray {
            val new_state = mutableListOf<Int>()
            val opus_manager = this.table_ui.get_opus_manager()

            if (opus_manager.is_line_control_line_selected(type, channel, line_offset)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_line_control_line_selected_secondary(type, channel, line_offset)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            if (opus_manager.channels[channel].get_line(line_offset).muted) {
                new_state.add(R.attr.state_muted)
            }

            return new_state.toIntArray()
        }

        fun <T: OpusEvent> draw_tree(canvas: Canvas, tree: ReducibleTree<T>, position: List<Int>, x: Float, y: Float, width: Float, callback: (T?, List<Int>, Canvas, Float, Float, Float) -> Unit) {
            if (tree.is_leaf()) {
                val horizontal_scroll_view = (this.parent.parent as HorizontalScrollView)
                // Don't draw outside of the view
                if (x + width >= horizontal_scroll_view.scrollX && x <= horizontal_scroll_view.scrollX + horizontal_scroll_view.measuredWidth) {
                    callback(tree.get_event(), position, canvas, x, y, width)
                }
            } else {
                val new_width = width / tree.size
                for (i in 0 until tree.size) {
                    val child = tree[i]
                    val next_position = OpusLayerBase.next_position(position, i)
                    this.draw_tree(canvas, child, next_position, x + (i * new_width), y, new_width, callback)
                }
            }
        }

        fun draw_std_tree_wrapper(canvas: Canvas, beat_key: BeatKey, x_offset: Float, y_offset: Float, line_height: Float, initial_width: Float) {
            val opus_manager = this.table_ui.get_activity().get_opus_manager()
            val line = opus_manager.get_channel(beat_key.channel).get_line(beat_key.line_offset)
            val tree = opus_manager.get_tree(beat_key, listOf())
            val colored_line_paint = Paint()
            this.draw_tree(canvas, tree, listOf(), x_offset, y_offset, initial_width) { event, position, canvas: Canvas, x: Float, y: Float, width: Float ->
                // It's technically possible for this to be called during a project change. I've only had it happen once, but...
                // TODO: ensure that *cannot* happen
                val state = try {
                    this.get_standard_leaf_state(beat_key, position)
                } catch (_: IndexOutOfBoundsException) {
                    return@draw_tree
                }

                this.draw_drawable(canvas, this.leaf_drawable, state, x, y, width, line_height)

                if (line.color != null && (state.contains(R.attr.state_spill) || state.contains(R.attr.state_active))) {
                    colored_line_paint.color = line.color!!
                    canvas.drawRect(
                        x,
                        y + (line_height * 1 / 16),
                        x + width - this.resources.getDimension(R.dimen.stroke_leaf),
                        y + (line_height * 4 / 16),
                        colored_line_paint
                    )
                }

                val color_list = ContextCompat.getColorStateList(this.context, R.color.leaf_text_selector)!!
                this.text_paint_octave.color = color_list.getColorForState(state, Color.MAGENTA)
                this.text_paint_offset.color = color_list.getColorForState(state, Color.MAGENTA)

                when (event) {
                    is AbsoluteNoteEvent -> {
                        val offset_text = "${event.note % opus_manager.tuning_map.size}"
                        val offset_text_bounds = Rect()
                        this.text_paint_offset.getTextBounds(
                            offset_text,
                            0,
                            offset_text.length,
                            offset_text_bounds
                        )

                        val octave_text = "${event.note / opus_manager.tuning_map.size}"
                        val octave_text_bounds = Rect()
                        this.text_paint_octave.getTextBounds(
                            octave_text,
                            0,
                            octave_text.length,
                            octave_text_bounds
                        )

                        val padding_x = this.resources.getDimension(R.dimen.octave_label_padding_x)
                        val text_height = (offset_text_bounds.height() * 2 / 3) + octave_text_bounds.height()
                        val offset_text_y = y + ((line_height - text_height) / 2)
                        val offset_text_x = x + ((width - (offset_text_bounds.width() + octave_text_bounds.width() + padding_x)) / 2)
                        canvas.drawText(
                            offset_text,
                            offset_text_x + octave_text_bounds.width() + padding_x,
                            offset_text_y + offset_text_bounds.height(),
                            this.text_paint_offset
                        )

                        canvas.drawText(
                            octave_text,
                            offset_text_x,
                            offset_text_y + text_height,
                            this.text_paint_octave
                        )
                    }

                    is RelativeNoteEvent -> {
                        val offset_text = "${abs(event.offset) % opus_manager.tuning_map.size}"
                        val offset_text_bounds = Rect()
                        this.text_paint_offset.getTextBounds(
                            offset_text,
                            0,
                            offset_text.length,
                            offset_text_bounds
                        )

                        val octave_text = "${abs(event.offset) / opus_manager.tuning_map.size}"
                        val octave_text_bounds = Rect()
                        this.text_paint_octave.getTextBounds(
                            octave_text,
                            0,
                            octave_text.length,
                            octave_text_bounds
                        )

                        val prefix_text = if (event.offset < 0) {
                            this.context.getString(R.string.pfx_subtract)
                        } else {
                            this.context.getString(R.string.pfx_add)
                        }

                        val prefix_text_bounds = Rect()
                        this.text_paint_octave.getTextBounds(
                            prefix_text,
                            0,
                            prefix_text.length,
                            prefix_text_bounds
                        )

                        val padding_x = this.resources.getDimension(R.dimen.octave_label_padding_x)

                        val text_height =
                            (offset_text_bounds.height() * 2 / 3) + octave_text_bounds.height()
                        val offset_text_y = y + ((line_height - text_height) / 2)
                        val offset_text_x =
                            x + ((width - (offset_text_bounds.width() + octave_text_bounds.width() + padding_x)) / 2)
                        canvas.drawText(
                            offset_text,
                            offset_text_x + octave_text_bounds.width() + padding_x,
                            offset_text_y + offset_text_bounds.height(),
                            this.text_paint_offset
                        )

                        canvas.drawText(
                            octave_text,
                            offset_text_x,
                            offset_text_y + text_height,
                            this.text_paint_octave
                        )

                        canvas.drawText(
                            prefix_text,
                            offset_text_x,
                            offset_text_y + if (event.offset < 0) {
                                prefix_text_bounds.height() * 4
                            } else {
                                prefix_text_bounds.height()
                            },
                            this.text_paint_octave
                        )

                    }

                    is PercussionEvent -> {
                        val offset_text =
                            this.resources.getString(R.string.percussion_label)
                        val bounds = Rect()
                        this.text_paint_offset.getTextBounds(
                            offset_text,
                            0,
                            offset_text.length,
                            bounds
                        )

                        canvas.drawText(
                            offset_text,
                            x + ((width - bounds.width()) / 2),
                            y + ((line_height + (bounds.height() / 2)) / 2),
                            this.text_paint_offset
                        )
                    }
                }
            }
        }

        fun draw_drawable(canvas: Canvas, drawable_id: Int, state: IntArray?, x: Float, y: Float, width: Float, height: Float) {
            val drawable = ContextCompat.getDrawable(this.context, drawable_id)!!
            this.draw_drawable(canvas, drawable, state, x, y, width, height)
        }
        fun draw_drawable(canvas: Canvas, drawable: Drawable, state: IntArray?, x: Float, y: Float, width: Float, height: Float) {
            state?.let {
                drawable.state = it
            }
            drawable.setBounds(x.toInt(), y.toInt(), (x + width).toInt(), (y + height).toInt())
            drawable.draw(canvas)
        }

        // TODO: Refactor this.
        override fun draw(canvas: Canvas) {
            // TODO: deal with draw Allocations. preallocate in different function?
            super.draw(canvas)
            val opus_manager = this.table_ui.get_opus_manager()

            // Don't Redraw while in flux
            if (opus_manager.project_changing) return

            val base_width = this.resources.getDimension(R.dimen.base_leaf_width)
            val line_height = floor(this.resources.getDimension(R.dimen.line_height))
            val ctl_line_height = floor(this.resources.getDimension(R.dimen.ctl_line_height))
            val channel_gap_height = floor(this.resources.getDimension(R.dimen.channel_gap_size))
            val line_label_width = floor(this.resources.getDimension(R.dimen.line_label_width))

            val first_column = this.table_ui.get_first_visible_column_index()
            val last_column = this.table_ui.get_last_visible_column_index()
            var offset = (this.table_ui.get_column_rect(first_column)?.x ?: 0).toFloat() + line_label_width

            val horizontal_scroll_view = (this.parent.parent as HorizontalScrollView)
            val vertical_scroll_view = (horizontal_scroll_view.parent as ScrollView)
            val scroll_y = vertical_scroll_view.scrollY.toFloat()
            val scroll_x = horizontal_scroll_view.scrollX.toFloat()

            val channels = opus_manager.get_all_channels()

            val dragging_from = Pair(
                this.table_ui._drag_handle.from_channel ?: -1,
                this.table_ui._drag_handle.from_line_offset ?: -1
            )
            val dragging_from_height = this.table_ui._drag_handle.from_selection_height

            val dragging_to = Pair(
                this.table_ui._drag_handle.to_channel ?: -1,
                this.table_ui._drag_handle.to_line_offset ?: -1
            )

            for (i in first_column .. last_column) {
                val beat_width = (this.table_ui.get_column_width(i) * floor(base_width))
                var y_offset = line_height
                for (j in channels.indices) {
                    if (dragging_to.first == j && dragging_to.second == -1) {
                        y_offset += dragging_from_height + channel_gap_height
                    }

                    val channel = channels[j]
                    for (k in channel.lines.indices) {
                        if (dragging_to.first == j && dragging_to.second == k) {
                            y_offset += dragging_from_height
                        }

                        val beat_key = BeatKey(j, k, i)
                        if (dragging_from.first != j || (dragging_from.second != -1 && dragging_from.second != k)) {
                            this.draw_std_tree_wrapper(canvas, beat_key, offset, y_offset, line_height, beat_width)
                            canvas.drawLine(offset, y_offset, offset + beat_width, y_offset, this.table_line_paint)
                            y_offset += line_height
                        }

                        for ((type, controller) in channel.lines[k].controllers.get_all()) {
                            if (!controller.visible) continue

                            if (dragging_from.first != j || (dragging_from.second != -1 && dragging_from.second != k)) {
                                this.draw_tree(canvas, controller.get_tree(i), listOf(), offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                                    val state = this.get_line_control_leaf_state(type, beat_key, position)
                                    this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                                }
                                canvas.drawLine(offset, y_offset, offset + beat_width, y_offset, this.table_line_paint)
                                y_offset += ctl_line_height
                            }
                        }
                    }

                    if (dragging_to.first == j && dragging_to.second == channel.lines.size) {
                        y_offset += dragging_from_height
                    }

                    for ((type, controller) in channel.controllers.get_all()) {
                        if (!controller.visible) continue

                        if (dragging_from.second != -1 || dragging_from.first != j) {
                            this.draw_tree(canvas, controller.get_tree(i), listOf(), offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                                val state = this.get_channel_control_leaf_state(type, j, i, position)
                                this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                            }
                            canvas.drawLine(offset, y_offset, offset + beat_width, y_offset, this.table_line_paint)
                            y_offset += ctl_line_height
                        }
                    }

                    if (dragging_from.second != -1 || dragging_from.first != j) {
                        canvas.drawRect(offset, y_offset, offset + beat_width, y_offset + channel_gap_height, this.table_line_paint)
                        y_offset += channel_gap_height
                    }
                }

                // Handle Gap between last channel and global ctl lines
                if (dragging_to.first == opus_manager.channels.size && dragging_to.second == -1) {
                    y_offset += dragging_from_height + channel_gap_height
                }

                for ((type, controller) in opus_manager.controllers.get_all()) {
                    if (!controller.visible) continue

                    this.draw_tree(canvas, controller.get_tree(i), listOf(), offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                        val state = this.get_global_control_leaf_state(type, i, position)
                        this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                    }
                    canvas.drawLine(offset, y_offset, offset + beat_width, y_offset, this.table_line_paint)

                    y_offset += ctl_line_height
                }

                // ------------------- Draw Column Labels ----------------------------
                val viewable_width = horizontal_scroll_view.measuredWidth
                val color_list = ContextCompat.getColorStateList(this.context, R.color.column_label_text)!!
                val state = this.get_column_label_state(i)
                this.text_paint_column.color = color_list.getColorForState(state, Color.MAGENTA)

                val column_width = this.table_ui.get_column_width(i) * floor(base_width)
                this.draw_drawable(canvas, R.drawable.editor_label_column, state, offset, scroll_y, column_width, line_height)

                val column_text = "$i"
                val bounds = Rect()

                this.text_paint_column.getTextBounds(column_text, 0, column_text.length, bounds)
                if (opus_manager.is_beat_tagged(i)) {
                    this.tagged_paint_column.color = color_list.getColorForState(state, Color.MAGENTA)
                    val x = if (column_width > viewable_width) {
                        if (offset <= scroll_x && offset + column_width >= scroll_x + viewable_width) {
                            (scroll_x + ((viewable_width - bounds.width()) / 2))
                        } else if (offset <= scroll_x) {
                            offset + column_width - ((viewable_width + bounds.width()) / 2)
                        } else {
                            offset + ((viewable_width - bounds.width()) / 2)
                        }
                    } else {
                        offset + ((column_width - bounds.width()) / 2)
                    }
                    val y = scroll_y + ((line_height - bounds.height()) / 2)

                    canvas.drawOval(
                        x - this.section_radius_x,
                        y - this.section_radius_y,
                        x + bounds.width() + this.section_radius_x,
                        y + bounds.height() + this.section_radius_y,
                        this.tagged_paint_column
                    )
                }

                canvas.drawText(
                    "$i",
                    // Keep the column number of huge columns on screen
                    if (column_width > viewable_width) {
                        if (offset <= scroll_x && offset + column_width >= scroll_x + viewable_width) {
                            (scroll_x + ((viewable_width - bounds.width()) / 2))
                        } else if (offset <= scroll_x) {
                            offset + column_width - ((viewable_width + bounds.width()) / 2)
                        } else {
                            offset + ((viewable_width - bounds.width()) / 2)
                        }
                    } else {
                        offset + ((column_width - bounds.width()) / 2)
                    },
                    scroll_y + ((line_height + bounds.height()) / 2),
                    this.text_paint_column
                )
                offset += beat_width
            }

            if (offset < this.width) {
                ContextCompat.getDrawable(this.context, R.drawable.icon_add_channel)?.let {
                    it.setBounds((offset).toInt(), (scroll_y).toInt(), (offset + base_width).toInt(), (scroll_y + line_height).toInt())
                    it.setTint(ContextCompat.getColor(this.context, R.color.table_foreground))
                    it.draw(canvas)
                }
            }

            // ------------------- Draw Line Labels ----------------------------
            var y_offset = line_height
            for (j in channels.indices) {
                if (dragging_to.first == j && dragging_to.second == -1) {
                    y_offset += dragging_from_height + channel_gap_height
                }

                for (k in channels[j].lines.indices) {
                    if (dragging_to.first == j && dragging_to.second == k) {
                        y_offset += dragging_from_height
                    }

                    if (dragging_from.first != j || (dragging_from.second != -1 && dragging_from.second != k)) {
                        this.draw_drawable(canvas, this.line_label_drawable, this.get_standard_line_state(j, k), scroll_x, y_offset, line_label_width, line_height)
                        this.draw_line_label_text(canvas, j, k, this.line_label_drawable.state, scroll_x, y_offset, line_label_width, line_height)
                        canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                        y_offset += line_height
                    }

                    for ((type, controller) in channels[j].lines[k].controllers.get_all()) {
                        if (!controller.visible) continue
                        if (dragging_from.first != j || (dragging_from.second != -1 && dragging_from.second != k)) {
                            this.draw_drawable(canvas, this.ctl_label_drawable, this.get_line_control_line_state(type, j, k), scroll_x, y_offset, line_label_width, ctl_line_height)
                            this.draw_ctl_label_text(canvas, type, this.ctl_label_drawable.state, scroll_x, y_offset, line_label_width, ctl_line_height)
                            canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                            y_offset += ctl_line_height
                        }
                    }
                }

                if (dragging_to.first == j && dragging_to.second == channels[j].lines.size) {
                    y_offset += dragging_from_height
                }

                for ((type, controller) in channels[j].controllers.get_all()) {
                    if (!controller.visible) continue

                    if (dragging_from.second != -1 || dragging_from.first != j) {
                        this.draw_drawable(canvas, this.ctl_label_drawable, this.get_channel_control_line_state(type, j), scroll_x, y_offset, line_label_width, ctl_line_height)
                        this.draw_ctl_label_text(canvas, type, this.ctl_label_drawable.state, scroll_x, y_offset, line_label_width, ctl_line_height)
                        canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                        y_offset += ctl_line_height
                    }
                }


                if (dragging_from.second != -1 || dragging_from.first != j) {
                    canvas.drawRect( scroll_x, y_offset, scroll_x + line_label_width, y_offset + channel_gap_height, this.table_line_paint)
                    y_offset += channel_gap_height
                }
            }

            if (dragging_to.first == opus_manager.channels.size && dragging_to.second == -1) {
                y_offset += dragging_from_height + channel_gap_height
            }

            for ((type, controller) in opus_manager.controllers.get_all()) {
                if (!controller.visible) continue

                this.draw_drawable(canvas, this.ctl_label_drawable, this.get_global_control_line_state(type), scroll_x, y_offset, line_label_width, ctl_line_height)
                this.draw_ctl_label_text(canvas, type, this.ctl_label_drawable.state, scroll_x, y_offset, line_label_width, ctl_line_height)
                canvas.drawLine( scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)

                y_offset += ctl_line_height
            }

            // Draw Global Control toggle button
            if (this.table_ui.global_ctl_button_visible) {
                this.draw_ctl_label_text(canvas, null, IntArray(0), scroll_x, y_offset, line_label_width, line_height)
            }

            // Draw Corner Button
            this.draw_drawable(canvas, this.corner_drawable, null, scroll_x, scroll_y, line_label_width, line_height)

            // Draw Floating/Dragged Channel/Line
            if (dragging_from != Pair(-1, -1)) {
                val x_start_offset = (this.table_ui.get_column_rect(first_column)?.x ?: 0).toFloat() + line_label_width
                val channel = opus_manager.channels[dragging_from.first]
                val y_start_offset = this.table_ui.touch_position_y - this.table_ui._drag_handle.y_offset

                if (dragging_from.second == -1) {
                    var x_offset = x_start_offset
                    for (i in first_column .. last_column) {
                        val beat_width = (this.table_ui.get_column_width(i) * floor(base_width))
                        var y_offset = y_start_offset
                        for (k in channel.lines.indices) {
                            val beat_key = BeatKey(dragging_from.first, k, i)
                            this.draw_std_tree_wrapper(canvas, beat_key, x_offset, y_offset, line_height, beat_width)
                            canvas.drawLine(x_offset, y_offset, x_offset + beat_width, y_offset, this.table_line_paint)

                            y_offset += line_height
                            for ((type, controller) in channel.lines[k].controllers.get_all()) {
                                if (!controller.visible) continue

                                this.draw_tree(canvas, controller.get_tree(i), listOf(), x_offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                                    val state = this.get_line_control_leaf_state(type, beat_key, position)
                                    this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                                }
                                canvas.drawLine(x_offset, y_offset, x_offset + beat_width, y_offset, this.table_line_paint)

                                y_offset += ctl_line_height
                            }
                        }
                        for ((type, controller) in channel.controllers.get_all()) {
                            if (!controller.visible) continue

                            this.draw_tree(canvas, controller.get_tree(i), listOf(), x_offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                                val state = this.get_channel_control_leaf_state(type, dragging_from.first, i, position)
                                this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                            }
                            canvas.drawLine(x_offset, y_offset, x_offset + beat_width, y_offset, this.table_line_paint)

                            y_offset += ctl_line_height
                        }
                        x_offset += beat_width
                    }

                    y_offset = y_start_offset
                    // Draw Floating Line labels
                    for (k in channel.lines.indices) {
                        this.get_standard_line_state(dragging_from.first, k).let { state ->
                            this.draw_drawable(canvas, this.line_label_drawable, state, scroll_x, y_offset, line_label_width, line_height)
                            this.draw_line_label_text(canvas, dragging_from.first, k, state, scroll_x, y_offset, line_label_width, line_height)
                            canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                        }

                        y_offset += line_height
                        for ((type, controller) in channel.lines[k].controllers.get_all()) {
                            if (!controller.visible) continue
                            this.get_line_control_line_state(type, dragging_from.first, k).let { state ->
                                this.draw_drawable(canvas, this.ctl_label_drawable, state, scroll_x, y_offset, line_label_width, ctl_line_height)
                                this.draw_ctl_label_text(canvas, type, state, scroll_x, y_offset, line_label_width, ctl_line_height)
                                canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                            }
                            y_offset += ctl_line_height
                        }
                    }
                    for ((type, controller) in channel.controllers.get_all()) {
                        if (!controller.visible) continue
                        this.get_channel_control_line_state(type, dragging_from.first).let { state ->
                            this.draw_drawable(canvas, this.ctl_label_drawable, state,scroll_x, y_offset, line_label_width, ctl_line_height)
                            this.draw_ctl_label_text(canvas, type, state, scroll_x, y_offset, line_label_width, ctl_line_height)
                            canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                        }
                        y_offset += ctl_line_height
                    }

                } else {
                    val line = opus_manager.channels[dragging_from.first].lines[dragging_from.second]
                    var x_offset = x_start_offset
                    for (i in first_column .. last_column) {
                        y_offset = y_start_offset
                        val beat_width = (this.table_ui.get_column_width(i) * floor(base_width))

                        val beat_key = BeatKey(dragging_from.first, dragging_from.second, i)
                        this.draw_std_tree_wrapper(canvas, beat_key, x_offset, y_offset, line_height, beat_width)
                        canvas.drawLine(x_offset, y_offset, x_offset + beat_width, y_offset, this.table_line_paint)

                        y_offset += line_height

                        for ((type, controller) in line.controllers.get_all()) {
                            if (!controller.visible) continue

                            this.draw_tree(canvas, controller.get_tree(i), listOf(), x_offset, y_offset, beat_width) { event, position, canvas, x, y, width ->
                                val state = this.get_line_control_leaf_state(type, beat_key, position)
                                this.process_ctl_event_layout(state, event, canvas, x, y, width, ctl_line_height)
                            }
                            canvas.drawLine(x_offset, y_offset, x_offset + beat_width, y_offset, this.table_line_paint)

                            y_offset += ctl_line_height
                        }

                        x_offset += beat_width
                    }

                    // Draw Floating selection line label/ctl labels
                    y_offset = y_start_offset
                    this.get_standard_line_state(dragging_from.first, dragging_from.second).let { state ->
                        this.draw_drawable(canvas, this.line_label_drawable, state, scroll_x, y_offset, line_label_width, line_height)
                        this.draw_line_label_text(canvas, dragging_from.first, dragging_from.second, state, scroll_x, y_offset, line_label_width, line_height)
                        canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                    }
                    y_offset += line_height
                    for ((type, controller) in channel.lines[dragging_from.second].controllers.get_all()) {
                        if (!controller.visible) continue
                        this.get_line_control_line_state(type, dragging_from.first, dragging_from.second).let { state ->
                            this.draw_drawable(canvas, this.ctl_label_drawable, state, scroll_x, y_offset, line_label_width, ctl_line_height)
                            this.draw_ctl_label_text(canvas, type, state, scroll_x, y_offset, line_label_width, ctl_line_height)
                            canvas.drawLine(scroll_x, y_offset, scroll_x + line_label_width, y_offset, this.table_line_paint)
                        }
                        y_offset += ctl_line_height
                    }
                }

                //canvas.drawLine(scroll_x, y_start_offset, (this.width.toFloat() - base_width), y_start_offset, this.table_line_paint)
            }
        }

        private fun draw_line_label_text(canvas: Canvas, channel: Int, line_offset: Int, state: IntArray, x: Float, y: Float, width: Float, height: Float) {
            val color_list = ContextCompat.getColorStateList(this.context, R.color.line_label_text)!!
            this.text_paint_line_label_std.color = color_list.getColorForState(state, Color.MAGENTA)

            val opus_manager = this.table_ui.get_activity().get_opus_manager()
            val (channel_text, line_offset_text) = if (opus_manager.is_percussion(channel)) {
                Pair(
                    "!$channel",
                    (opus_manager.get_channel(channel).get_line(line_offset) as OpusLinePercussion).instrument.toString()
                )
            } else {
                Pair(channel.toString(), line_offset.toString())
            }
            val channel_text_bounds = Rect()
            val line_offset_text_bounds = Rect()

            this.text_paint_line_label_std.getTextBounds(channel_text, 0, channel_text.length, channel_text_bounds)
            this.text_paint_line_label_std.getTextBounds(line_offset_text, 0, line_offset_text.length, line_offset_text_bounds)

            val padding = this.resources.getDimension(R.dimen.line_label_padding)
            canvas.drawText(channel_text, padding + x, padding + y + channel_text_bounds.height(), this.text_paint_line_label_std)
            canvas.drawText(line_offset_text, x + width - line_offset_text_bounds.width() - padding, y + height - padding, this.text_paint_line_label_std)
        }

        private fun draw_ctl_label_text(canvas: Canvas, type: EffectType?, state: IntArray, x: Float, y: Float, width: Float, height: Float) {
            val color_list = ContextCompat.getColorStateList(this.context, R.color.line_label_ctl_text)!!
            val ctl_drawable = ContextCompat.getDrawable(
                this.context,
                when (type) {
                    EffectType.Tempo -> R.drawable.icon_tempo
                    EffectType.Volume -> R.drawable.icon_volume
                    EffectType.Velocity -> R.drawable.icon_velocity
                    EffectType.Pan -> R.drawable.icon_pan
                    EffectType.Delay -> R.drawable.icon_echo
                    else -> R.drawable.icon_ctl
                    //EffectType.Reverb -> R.drawable.icon_volume // Placeholder TODO
                }
            )!!

            val padding = this.resources.getDimension(R.dimen.line_label_padding)
            val adj_height = height - (2 * padding)

            val ratio = ctl_drawable.intrinsicWidth.toFloat() / ctl_drawable.intrinsicHeight.toFloat()
            val adj_width = ratio * adj_height

            ctl_drawable.setBounds(
                (x + ((width - adj_width) / 2)).toInt(),
                (y + padding).toInt(),
                (x + ((width + adj_width) / 2)).toInt(),
                (y + height - padding).toInt()
            )

            ctl_drawable.setTint(color_list.getColorForState(state, Color.MAGENTA))
            ctl_drawable.draw(canvas)
        }

        private fun get_column_label_state(x: Int): IntArray {
            val new_state = mutableSetOf<Int>()

            val opus_manager = this.table_ui.get_opus_manager()

            if (opus_manager.is_beat_selected(x)) {
                new_state.add(R.attr.state_focused)
            } else if (opus_manager.is_beat_selected_secondary(x)) {
                new_state.add(R.attr.state_focused_secondary)
            }

            return new_state.toIntArray()
        }

        fun process_ctl_event_layout(state: IntArray, event: EffectEvent?, canvas: Canvas, x: Float, y: Float, width: Float, ctl_line_height: Float) {
            val ctl_drawable = ContextCompat.getDrawable(this.context, R.drawable.ctl_leaf)!!
            ctl_drawable.state = state
            ctl_drawable.setBounds(
                (x).toInt(),
                (y).toInt(),
                (x + width).toInt(),
                (y + ctl_line_height).toInt()
            )
            ctl_drawable.draw(canvas)

            val color_list = ContextCompat.getColorStateList(this.context, R.color.ctl_leaf_text_selector)!!
            this.text_paint_ctl.color = color_list.getColorForState(state, Color.MAGENTA)

            val text = when (event) {
                null -> return
                is OpusVolumeEvent -> (event.value).toString()
                is OpusVelocityEvent -> (event.value).toString()
                is OpusTempoEvent -> event.value.roundToInt().toString()
                is OpusPanEvent -> {
                    if (event.value > 0F) {
                        val n = (event.value * 10).roundToInt()
                        "<$n"
                    } else if (event.value < 0F) {
                        val n = 0 - (event.value * 10).roundToInt()
                        "$n>"
                    } else {
                        "0"
                    }
                }
                is DelayEvent -> {
                    if (event.echo == 0 || event.fade == 0f) {
                        "-"
                    } else {
                        val rational = Rational(event.numerator, event.denominator)
                        rational.reduce()
                        "${event.echo + 1}x${rational.numerator}/${rational.denominator}"
                    }
                }
                else -> "???"
            }

            val text_bounds = Rect()
            this.text_paint_ctl.getTextBounds(text, 0, text.length, text_bounds)

            canvas.drawText(
                text,
                x - text_bounds.left + ((width - text_bounds.width()) / 2),
                y + ((ctl_line_height + text_bounds.height()) / 2),
                this.text_paint_ctl
            )
        }

        fun insert_row(y: Int) {
            //for (i in 0 until this.childCount) {
            //    val column = this.get_column(i)
            //    column.insert_cells(y, 1)
            //}
            this.invalidate_wrapper()
        }

        fun remove_rows(y: Int, count: Int = 1) {
            //for (i in 0 until this.childCount) {
            //    val column = this.get_column(i)
            //    column.remove_cells(y, count)
            //}
            this.invalidate_wrapper()
        }

        fun add_column(x: Int) {
            //val new_column = ColumnLayout(this.editor_table, x)
            //this.addView(new_column, x)
            this.invalidate_wrapper()
        }

        fun add_columns(x: Int, count: Int) {
            //for (i in x until count) {
            //    this.add_column(i)
            //}
            this.invalidate_wrapper()
        }

        fun remove_column(x: Int) {
            //this.removeViewAt(x)
            this.invalidate_wrapper()
        }

        fun notify_cell_changed(y: Int, x: Int, state_only: Boolean = false) {
            // val column = this.get_column(x)
            // column.notify_item_changed(y, state_only)
            this.invalidate_wrapper()
        }

        fun notify_column_changed(x: Int, state_only: Boolean = false) {
            //val column = this.get_column(x)
            //if (state_only) {
            //    column.notify_state_changed()
            //} else {
            //    column.rebuild()
            //}
            this.invalidate_wrapper()
        }

        fun notify_row_change(y: Int, state_only: Boolean = false) {
            //for (x in 0 until this.childCount) {
            //    this.get_column(x).notify_item_changed(y, state_only)
            //}
            this.invalidate_wrapper()
        }

        fun clear() {
            this.invalidate_wrapper()
        }

        /* The layout is currently refresh solely by an invalidate call. use the wrapper to stop it being called for EVERY update to the table before it gets redrawn */
        fun invalidate_wrapper() {
            this.invalidate_queued = true
        }
    }

    companion object {
        fun <T: OpusEvent> calc_position(tree: ReducibleTree<T>, initial_width: Int, target_x: Float): List<Int> {
            var working_width = initial_width.toFloat()
            var working_tree = tree
            var working_x = target_x.toInt()
            val output = mutableListOf<Int>()
            while (!working_tree.is_leaf()) {
                val child_pos = (working_x * working_tree.size / working_width).toInt()
                output.add(child_pos)
                working_width /= working_tree.size
                working_x %= working_width.toInt()
                working_tree = working_tree[child_pos]
            }
            return output
        }
    }

    class DragHandle() {
        enum class DragMode {
            Channel,
            Line
        }
        var dragging_up: Boolean? = null
        var from_channel: Int? = null
        var from_line_offset: Int? = null
        var to_channel: Int? = null
        var to_line_offset: Int? = null
        var from_map_index: Int? = null
        var to_map_index: Int? = null
        var mode: DragMode? = null
        var y_offset: Float = 0F

        var from_selection_height: Int = 0
        var to_selection_height: Int = 0

        var line_drag_map = mutableListOf<Triple<IntRange, Pair<Int, Int>, Boolean>>()
        var channel_drag_map = mutableListOf<Triple<IntRange, Int, Boolean>>()


        fun is_dragging(): Boolean {
            return this.from_channel != null
        }

        fun set_from(y_offset: Float, channel: Int, line_offset: Int? = null) {
            this.clear()
            this.from_channel = channel
            this.from_line_offset = line_offset
            this.mode = if (line_offset == null) {
                for (i in this.channel_drag_map.indices) {
                    val (y_range, check_channel, _) = this.channel_drag_map[i]
                    if (check_channel == channel) {
                        this.from_map_index = i
                        this.y_offset = y_offset - y_range.first
                        break
                    }
                }
                DragMode.Channel
            } else {
                for (i in this.line_drag_map.indices) {
                    val (y_range, check_pair, _) = this.line_drag_map[i]
                    if (channel == check_pair.first && line_offset == check_pair.second) {
                        this.from_map_index = i
                        this.y_offset = y_offset - y_range.first
                        break
                    }
                }
                DragMode.Line
            }
            this.from_selection_height = this.get_section_height(this.from_map_index!!)
        }

        fun clear() {
            this.to_map_index = null
            this.y_offset = 0F
            this.from_channel = null
            this.from_line_offset = null
            this.to_channel = null
            this.to_line_offset = null
            this.from_map_index = null
            this.to_map_index = null
            this.mode = null
            this.from_selection_height = 0
            this.to_selection_height = 0
        }

        fun update_to(y: Int) {
            when (this.mode) {
                DragHandle.DragMode.Channel -> {
                    for (i in this.channel_drag_map.indices) {
                        val (y_range, channel, before) = this.channel_drag_map[i]
                        if (y_range.contains(y)) {
                            this.to_channel = channel + if (before) 0 else 1
                            this.to_line_offset = -1
                            this.to_map_index = i
                            break
                        }
                    }
                }
                DragHandle.DragMode.Line ->  {
                    for (i in this.line_drag_map.indices) {
                        val (y_range, pair, before) = this.line_drag_map[i]
                        if (y_range.contains(y)) {
                            this.to_channel = pair.first
                            this.to_line_offset = pair.second + if (before) 0 else 1
                            this.to_map_index = i
                            break
                        }
                    }
                }
                else -> return
            }
            this.to_selection_height = this.get_section_height(this.to_map_index!!)
            this.dragging_up = if (this.to_channel == this.from_channel) {
                if (this.to_line_offset == this.from_line_offset) {
                    null
                } else {
                    (this.to_line_offset ?: -1) < (this.from_line_offset ?: -1)
                }
            } else {
                (this.to_channel ?: -1) < (this.from_channel ?: -1)
            }
        }

        private fun _get_info(i: Int): Triple< Int, Int, Boolean>? {
            return when (this.mode) {
                DragMode.Channel -> {
                    val (_, channel, is_top) = this.channel_drag_map[i]
                    Triple(channel, -1, is_top)
                }
                DragMode.Line -> {
                    val (_, pair, is_top) = this.line_drag_map[i]
                    Triple(pair.first, pair.second, is_top)
                }
                else -> null
            }
        }

        fun get_from_info(): Triple<Int, Int, Boolean>? {
            return this._get_info(this.from_map_index ?: return null)
        }

        fun get_to_info(): Triple<Int, Int, Boolean>? {
            return this._get_info(this.to_map_index ?: return null)
        }

        private fun get_section_height(i: Int): Int {
            return when (this.mode) {
                DragMode.Channel -> {
                    val (y_range, _, top) = this.channel_drag_map[i]
                    if (top) {
                        this.channel_drag_map[i+1].first.last - y_range.first
                    } else {
                        y_range.first - this.channel_drag_map[i-1].first.first
                    }
                }
                DragMode.Line -> {
                    val (y_range, _, top) = this.line_drag_map[i]
                    if (top) {
                        this.line_drag_map[i+1].first.last - y_range.first
                    } else {
                        y_range.first - this.line_drag_map[i-1].first.first
                    }
                }
                null -> 0
            }
        }

        fun clear_drag_maps() {
            this.channel_drag_map.clear()
            this.line_drag_map.clear()
        }
    }

    val painted_layer = PaintedLayer(this)
    private var _scroll_locked: Boolean = false
    private var queued_scroll_x: Int? = null
    private var queued_scroll_y: Int? = null
    private var _last_x_position: Float? = null
    private var _drag_handle: DragHandle = DragHandle()
    var global_ctl_button_visible: Boolean = false

    var touch_position_x = 0F
    var touch_position_y = 0F

    val inner_scroll_view = object : HorizontalScrollView(this.context) {
        private var _initial_y_scroll_position: Pair<Float, Int>? = null
        override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
            super.onScrollChanged(l, t, oldl, oldt)
            this@EditorTable.painted_layer.invalidate()
        }

        override fun onTouchEvent(motion_event: MotionEvent?): Boolean {
            this@EditorTable.set_touch_position(motion_event, this@EditorTable.scrollX.toFloat())

            when (motion_event?.action) {
                ACTION_UP -> {
                    this._initial_y_scroll_position = null
                    this@EditorTable.do_action_up(motion_event)
                }
                ACTION_MOVE -> {
                    val y_relative = motion_event.y - this.y
                    if (this._initial_y_scroll_position == null) {
                        this._initial_y_scroll_position = Pair(y_relative, this@EditorTable.scrollY)
                    }

                    if (this@EditorTable._drag_handle.is_dragging()) {
                        this@EditorTable.update_cached_line_drag_position()
                        this@EditorTable.handle_in_drag_scroll()
                        return false
                    } else {
                        val diff = this._initial_y_scroll_position!!.first - y_relative
                        this@EditorTable.scrollBy(0, diff.roundToInt())
                    }
                }
            }

            return super.onTouchEvent(motion_event)
        }
    }

    private fun set_dragging() {
        if (this._drag_handle.is_dragging()) return
        val y_relative = this.touch_position_y

        this.recache_drag_maps()
        val row_position = this.get_visible_row_from_pixel(this.touch_position_y) ?: return
        if (row_position < 0) return

        val opus_manager = this.get_activity().get_opus_manager()
        val (pointer, ctl_line_level, _) = opus_manager.get_ctl_line_info(opus_manager.get_ctl_line_from_row(row_position))
        if (pointer == -1) return // Global row

        val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
        if (opus_manager.cursor.mode == CursorMode.Channel) {
            if (opus_manager.cursor.channel != channel) {
                opus_manager.cursor_select_channel(channel)
            }
            this._drag_handle.set_from(y_relative, channel)
        } else {
            when (ctl_line_level) {
                null,
                CtlLineLevel.Line ->{
                    this._drag_handle.set_from(y_relative, channel, line_offset)
                    if (opus_manager.cursor.mode != CursorMode.Line || opus_manager.cursor.channel != channel || opus_manager.cursor.line_offset != line_offset) {
                        opus_manager.cursor_select_line(channel, line_offset)
                    }
                }
                CtlLineLevel.Channel -> {
                    this._drag_handle.set_from(y_relative, channel)
                    if (opus_manager.cursor.channel != channel) {
                        opus_manager.cursor_select_channel(channel)
                    }
                }
                CtlLineLevel.Global -> null
            }
        }
    }

    private fun update_cached_line_drag_position() {
        if (!this._drag_handle.is_dragging()) {
            this._drag_handle.to_map_index = null
            return
        }

        this._drag_handle.update_to(this.touch_position_y.toInt())

        this.painted_layer.invalidate()
    }

    private fun do_action_up(motion_event: MotionEvent?): Boolean {
        if (motion_event?.action != ACTION_UP) return true
        if (this._drag_handle.is_dragging()) {
            this.update_cached_line_drag_position()

            val action_interface = this.get_action_interface()
            when (this._drag_handle.mode) {
                DragHandle.DragMode.Channel -> {
                    this._drag_handle.get_to_info()?.let { (to_channel, _, is_top) ->
                        action_interface.move_channel(this._drag_handle.from_channel!!, to_channel, is_top)
                    }
                }
                DragHandle.DragMode.Line -> {
                    this._drag_handle.get_to_info()?.let { (to_channel, to_line, is_top) ->
                        action_interface.move_line(
                            this._drag_handle.from_channel!!,
                            this._drag_handle.from_line_offset!!,
                            to_channel,
                            to_line,
                            is_top
                        )
                    }
                }
                null -> {}
            }
            this._drag_handle.clear()
            return false
        }
        this._last_x_position = null

        return true
    }

    fun set_touch_position(motion_event: MotionEvent?, offset_x: Float = 0F, offset_y: Float = 0F) {
        motion_event?.let {
            this.touch_position_y = it.y + offset_y
            this.touch_position_x = it.x + offset_x
        }
    }

    override fun onInterceptTouchEvent(motion_event: MotionEvent?): Boolean {
        val output = super.onInterceptTouchEvent(motion_event)
        this.set_touch_position(motion_event, this.scrollX.toFloat(), this.scrollY.toFloat())
        when (motion_event?.action) {
            MotionEvent.ACTION_UP -> this.do_action_up(motion_event)
        }
        return output
    }

    override fun onTouchEvent(motion_event: MotionEvent?): Boolean {
        this.set_touch_position(motion_event, this.scrollX.toFloat(), this.scrollY.toFloat())
        when (motion_event?.action) {
            MotionEvent.ACTION_MOVE -> {
                this._last_x_position = motion_event.x
                if (this._drag_handle.is_dragging()) {
                    this.update_cached_line_drag_position()
                    this.handle_in_drag_scroll()
                    return false
                }
            }
            MotionEvent.ACTION_UP -> this.do_action_up(motion_event)
        }
        return super.onTouchEvent(motion_event)
    }

    fun lock_scroll() {
        this._scroll_locked = true
    }

    fun unlock_scroll() {
        this._scroll_locked = false
    }

    private fun is_scroll_locked(): Boolean {
        return this._scroll_locked
    }


    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        this.painted_layer.invalidate()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (this.queued_scroll_x != null || this.queued_scroll_y != null) {
            this.scroll(this.queued_scroll_x, this.queued_scroll_y)
            this.queued_scroll_x = null
            this.queued_scroll_y = null
        }
    }

    fun scroll(x: Int? = null, y: Int? = null) {
        if (this.measuredWidth == 0) {
            this.queued_scroll_x = x
            this.queued_scroll_y = y
        } else {
            if (x != null) {
                this.inner_scroll_view.scrollTo(x, 0)
            }
            if (y != null) {
                this.scrollTo(0, y)
            }
        }
    }

    fun update_global_ctl_button() {
        this.global_ctl_button_visible = !this.get_activity().get_opus_manager().all_global_controllers_visible()
    }


    fun insert_row(y: Int) {
        this.painted_layer.insert_row(y)
    }

    fun add_column(x: Int) {
        this.painted_layer.add_column(x)
    }

    fun add_columns(x: Int, count: Int) {
        this.painted_layer.add_columns(x, count)
    }

    fun notify_cell_changed(coord: EditorTable.Coordinate, rtree: ReducibleTree<*>?) {
        rtree?.let { this.cell_map[coord] = it }
        this.painted_layer.notify_cell_changed(coord.y, coord.x)
    }

    fun set_size(width: Int, height: Int) {
        this.painted_layer.minimumWidth = width + this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        this.painted_layer.minimumHeight = height + this.resources.getDimension(R.dimen.line_height).toInt()
    }

    fun get_scroll_x_max(): Int {
        return if (this.painted_layer.width <= this.width) {
            this.painted_layer.width - 1
        } else {
            this.inner_scroll_view.scrollX + this.width
        }
    }

    /*
         Kludge. There is *currently* no spot-updating even though there are functions that make it look that way
         So updates are queued and this is how we prevent the view getting redrawn multiple times per update.
     */

    fun finalize_update() {
        this.update_global_ctl_button()
        if (this.painted_layer.invalidate_queued) {
            this.painted_layer.invalidate()
            this.painted_layer.invalidate_queued = false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        // Required when android keyboard is used to input a value. eg repeating a selection.
        // The bottom lines dont get drawn
        this.painted_layer.invalidate()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun on_long_click(row_type: RowType, line_info: Triple<Int, CtlLineLevel?, EffectType?>?, beat: Int?, position: List<Int>?): Boolean {
        this._drag_handle.clear()
        val action_interface = this.get_action_interface()
        val opus_manager = action_interface.get_opus_manager()
        return if (beat == null) {
            when (row_type) {
                RowType.Bottom -> { /* No Defined Behaviour */ }
                RowType.Top -> opus_manager.force_cursor_select_column(0)
                RowType.UI -> {
                    val opus_manager = this.get_activity().get_opus_manager()
                    val cursor = opus_manager.cursor
                    if (cursor.is_selecting_range()) {
                        val (pointer, ctl_level, ctl_type) = line_info!!
                        when (ctl_level) {
                            CtlLineLevel.Line -> {
                                val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                                action_interface.repeat_selection_ctl_line(ctl_type!!, channel, line_offset)
                            }

                            CtlLineLevel.Channel -> {
                                action_interface.repeat_selection_ctl_channel(ctl_type!!, pointer)
                            }

                            CtlLineLevel.Global -> {
                                if (cursor.is_selecting_range()) {
                                    action_interface.repeat_selection_ctl_global(ctl_type!!)
                                } else {
                                    action_interface.cursor_select_global_ctl_line(ctl_type!!)
                                }
                            }

                            null -> {
                                val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                                action_interface.repeat_selection_std(channel, line_offset)
                            }
                        }
                    } else {
                        this.set_dragging()
                        this.update_cached_line_drag_position()
                    }
                }
            }
            true
        } else {
            when (row_type) {
                RowType.UI -> {
                    val (pointer, ctl_line_level, ctl_type) = line_info!!
                    val cursor = opus_manager.cursor
                    when (ctl_line_level) {
                        null -> {
                            val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                            val beat_key = BeatKey(channel, line_offset, beat)

                            if (cursor.is_selecting_range() && cursor.ctl_level == null) {
                                action_interface.cursor_select_range(opus_manager.cursor.range!!.first, beat_key)
                            } else {
                                action_interface.cursor_select_range(beat_key, beat_key)
                            }
                        }

                        CtlLineLevel.Line -> {
                            val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                            val beat_key = BeatKey(channel, line_offset, beat)

                            if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Line && cursor.range!!.first.channel == beat_key.channel && cursor.range!!.first.line_offset == beat_key.line_offset && ctl_type == cursor.ctl_type) {
                                action_interface.cursor_select_line_ctl_range(ctl_type!!, cursor.range!!.first, beat_key)
                            } else {
                                action_interface.cursor_select_line_ctl_range(ctl_type!!, beat_key, beat_key)
                            }
                        }

                        CtlLineLevel.Channel -> {
                            val type = ctl_type!!
                            val channel = pointer

                            if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Channel && cursor.ctl_type == type) {
                                // Currently, can't select multiple channels in a range
                                if (channel == cursor.range!!.first.channel) {
                                    action_interface.cursor_select_channel_ctl_range(type, channel, cursor.range!!.first.beat, beat)
                                }
                            } else {
                                action_interface.cursor_select_channel_ctl_range(type, channel, beat, beat)
                            }
                        }

                        CtlLineLevel.Global -> {
                            if (cursor.is_selecting_range() && cursor.ctl_level == CtlLineLevel.Global && cursor.ctl_type == ctl_type) {
                                action_interface.cursor_select_global_ctl_range(ctl_type!!, cursor.range!!.first.beat, beat)
                            } else {
                                action_interface.cursor_select_global_ctl_range(ctl_type!!, beat, beat)
                            }
                        }
                    }
                    true
                }
                RowType.Top -> {
                    if (beat == opus_manager.length) {
                        action_interface.insert_beat(beat)
                        true
                    } else {
                        false
                    }
                }
                else -> false
                // RowType.Bottom -> {}
            }
        }
    }

    fun on_click(row_type: RowType, line_info: Triple<Int, CtlLineLevel?, EffectType?>?, beat: Int?, position: List<Int>?) {
        val action_interface = this.get_action_interface()
        val opus_manager = action_interface.get_opus_manager()
        if (beat == null) {
            when (row_type) {
                RowType.Top -> this.get_activity().shortcut_dialog()
                RowType.Bottom -> action_interface.show_hidden_global_controller()
                RowType.UI -> {
                    val opus_manager = this.get_activity().get_opus_manager()
                    val cursor = opus_manager.cursor
                    val (pointer, ctl_level, ctl_type) = line_info!!
                    when (ctl_level) {
                        CtlLineLevel.Line -> {
                            val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                            if (cursor.is_selecting_range()) {
                                action_interface.repeat_selection_ctl_line(ctl_type!!, channel, line_offset, -1)
                            } else {
                                action_interface.cursor_select_line_ctl_line(ctl_type!!, channel, line_offset)
                            }
                        }
                        CtlLineLevel.Channel -> {
                            if (cursor.is_selecting_range()) {
                                action_interface.repeat_selection_ctl_channel(ctl_type!!, pointer, -1)
                            } else {
                                action_interface.cursor_select_channel_ctl_line(ctl_type!!, pointer)
                            }
                        }
                        CtlLineLevel.Global -> {
                            if (cursor.is_selecting_range()) {
                                action_interface.repeat_selection_ctl_global(ctl_type!!, -1)
                            } else {
                                action_interface.cursor_select_global_ctl_line(ctl_type!!)
                            }
                        }
                        null -> {
                            val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                            if (cursor.is_selecting_range()) {
                                action_interface.repeat_selection_std(channel, line_offset, -1)
                            } else {
                                action_interface.cursor_select_line_std(channel, line_offset)
                            }
                        }
                    }
                }
            }
        } else if (beat < opus_manager.length) {
            when (row_type) {
                RowType.Top -> {
                    if (beat == opus_manager.length) {
                        action_interface.insert_beat(beat)
                    } else {
                        action_interface.cursor_select_column(beat)
                    }
                }
                RowType.UI -> {
                    val (pointer, ctl_line_level, ctl_type) = line_info!!
                    when (ctl_line_level) {
                        null -> {
                            val (channel, line_offset) = opus_manager.get_channel_and_line_offset(
                                pointer
                            )
                            this._process_standard_on_click(
                                BeatKey(channel, line_offset, beat),
                                position!!
                            )
                        }

                        CtlLineLevel.Line -> {
                            val (channel, line_offset) = opus_manager.get_channel_and_line_offset(
                                pointer
                            )
                            val beat_key = BeatKey(channel, line_offset, beat)
                            this._process_ctl_line_on_click(ctl_type!!, beat_key, position!!)
                        }

                        CtlLineLevel.Channel -> {
                            this._process_ctl_channel_on_click(
                                ctl_type!!,
                                pointer,
                                beat,
                                position!!
                            )
                        }

                        CtlLineLevel.Global -> {
                            this._process_ctl_global_on_click(ctl_type!!, beat, position!!)
                        }
                    }
                }
                RowType.Bottom -> { /* No defined behavior */ }
            }
        } else {
            when (row_type) {
                RowType.Top ->  action_interface.insert_beat(beat)
                else -> {}
            }
        }
    }

    private fun _process_standard_on_click(beat_key: BeatKey, position: List<Int>) {
        val context = this.get_activity()
        val opus_manager = context.get_opus_manager()

        val cursor = opus_manager.cursor
        val tracker = this.get_action_interface()
        if (cursor.is_selecting_range() && cursor.ctl_level == null) {
            try {
                when (context.configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> {
                        tracker.copy_selection_to_beat(beat_key)
                    }
                    PaganConfiguration.MoveMode.MOVE -> {
                        tracker.move_selection_to_beat(beat_key)
                    }
                    PaganConfiguration.MoveMode.MERGE -> {
                        tracker.merge_selection_into_beat(beat_key)
                    }
                }

                // TODO: This shouldn't be here. cursor_select should be handled in OpusLayerCursor
                if (opus_manager.temporary_blocker == null) {
                    opus_manager.cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                }

            } catch (e: Exception) {
                when (e) {
                    is MixedInstrumentException -> {
                        tracker.ignore().cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                        context.feedback_msg(context.getString(R.string.feedback_mixed_copy))
                    }
                    is RangeOverflow -> {
                        tracker.ignore().cursor_select(beat_key, position)
                        context.feedback_msg(context.getString(R.string.feedback_bad_range))
                    }
                    is InvalidCursorState -> {
                        // Shouldn't ever actually be possible
                        throw e
                    }
                    is InvalidMergeException -> {
                        tracker.ignore().cursor_select(beat_key, opus_manager.get_first_position(beat_key))
                    }
                    else -> {
                        throw e
                    }
                }
            }
        } else if (beat_key.beat < opus_manager.length) {
            tracker.cursor_select(beat_key, position)
        }
    }

    private fun _process_ctl_line_on_click(type: EffectType, beat_key: BeatKey, position: List<Int>) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val cursor = opus_manager.cursor
        val tracker = this.get_action_interface()

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (activity.configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> tracker.copy_line_ctl_to_beat(beat_key)
                    PaganConfiguration.MoveMode.MOVE -> tracker.move_line_ctl_to_beat(beat_key)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is InvalidOverwriteCall -> {
                        tracker.ignore().cursor_select_ctl_at_line(type, beat_key, position)
                    }
                    else -> throw e
                }
            }
        } else {
            tracker.cursor_select_ctl_at_line(type, beat_key, position)
        }
    }

    private fun _process_ctl_channel_on_click(type: EffectType, channel: Int, beat: Int, position: List<Int>) {
        val activity = this.get_activity()
        val opus_manager = activity.get_opus_manager()
        val cursor = opus_manager.cursor

        val tracker = this.get_action_interface()

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (activity.configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> tracker.copy_channel_ctl_to_beat(channel, beat)
                    PaganConfiguration.MoveMode.MOVE -> tracker.move_channel_ctl_to_beat(channel, beat)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is InvalidOverwriteCall -> {
                        tracker.ignore().cursor_select_ctl_at_channel(type, channel, beat, position)
                    }
                    else -> throw e
                }
            }
        } else {
            tracker.cursor_select_ctl_at_channel(type, channel, beat, position)
        }
    }

    private fun _process_ctl_global_on_click(type: EffectType, beat: Int, position: List<Int>) {
        val opus_manager = this.get_activity().get_opus_manager()
        val cursor = opus_manager.cursor
        val tracker = this.get_action_interface()

        if (cursor.is_selecting_range() && cursor.ctl_type == type) {
            try {
                when (this.get_activity().configuration.move_mode) {
                    PaganConfiguration.MoveMode.COPY -> tracker.copy_global_ctl_to_beat(beat)
                    PaganConfiguration.MoveMode.MOVE -> tracker.move_global_ctl_to_beat(beat)
                    PaganConfiguration.MoveMode.MERGE -> { /* Unreachable */ }
                }
            } catch (e: Exception) {
                when (e) {
                    is IndexOutOfBoundsException,
                    is InvalidOverwriteCall -> {
                        tracker.cursor_select_ctl_at_global(type, beat, position)
                    }
                    else -> {
                        throw e
                    }
                }
            }
        } else {
            tracker.cursor_select_ctl_at_global(type, beat, position)
        }
    }

    fun get_action_interface(): ActionTracker {
        return this.get_activity().get_action_interface()
    }

    private fun _get_current_line_info_and_position(x_touch: Float, y_touch: Float): Triple<RowType, Triple<Int, CtlLineLevel?, EffectType?>?, Pair<Int?, List<Int>?>>?  {
        val side_column_width = this.resources.getDimension(R.dimen.line_label_width)
        val x = x_touch - side_column_width

        val row_position = this.get_visible_row_from_pixel(y_touch) ?: return null

        val min_leaf_width = this.resources.getDimension(R.dimen.base_leaf_width).toInt()
        val opus_manager = this.get_opus_manager()
        val beat: Int? = if (x_touch - this.inner_scroll_view.scrollX < side_column_width) {
            null
        } else {
            this.get_column_from_leaf((x / min_leaf_width).toInt()) ?: opus_manager.length
        }

        return when (row_position) {
            -1 -> Triple(RowType.Top, null, Pair(beat, null))
            -2 -> Triple(RowType.Bottom, null, Pair(beat, null))
            else -> {
                val (pointer, ctl_line_level, ctl_type) = opus_manager.get_ctl_line_info(
                    opus_manager.get_ctl_line_from_row(row_position)
                )

                Triple(
                    RowType.UI,
                    Triple(pointer, ctl_line_level, ctl_type),
                    Pair(
                        beat,
                        when (beat) {
                            null -> null
                            opus_manager.length -> listOf()
                            else -> {
                                val inner_offset = x - this.get_column_offset(beat)
                                val column_width = this.get_column_width(beat) * min_leaf_width

                                when (ctl_line_level) {
                                    null -> {
                                        val (channel, line_offset) = opus_manager.get_channel_and_line_offset(pointer)
                                        val beat_key = BeatKey(channel, line_offset, beat)
                                        EditorTable.calc_position(
                                            opus_manager.get_tree(beat_key),
                                            column_width,
                                            inner_offset
                                        )
                                    }

                                    CtlLineLevel.Line -> {
                                        val (channel, line_offset) = opus_manager.get_channel_and_line_offset(
                                            pointer
                                        )
                                        val beat_key = BeatKey(channel, line_offset, beat)
                                        EditorTable.calc_position(
                                            opus_manager.get_line_ctl_tree(
                                                ctl_type!!,
                                                beat_key
                                            ), column_width, inner_offset
                                        )
                                    }

                                    CtlLineLevel.Channel -> {
                                        EditorTable.calc_position(
                                            opus_manager.get_channel_ctl_tree(
                                                ctl_type!!,
                                                pointer,
                                                beat
                                            ), column_width, inner_offset
                                        )
                                    }

                                    CtlLineLevel.Global -> {
                                        EditorTable.calc_position(
                                            opus_manager.get_global_ctl_tree(
                                                ctl_type!!,
                                                beat
                                            ), column_width, inner_offset
                                        )
                                    }
                                }
                            }
                        }
                    )
                )
            }
        }
    }

    /**
     * Drag maps are stored so that when dragging a line or channel, we don't need to keep calculating
     *  which channel or line the cursor occupies
     */
    fun recache_drag_maps() {
        this._drag_handle.clear_drag_maps()

        val line_height = floor(this.resources.getDimension(R.dimen.line_height))
        val ctl_line_height = floor(this.resources.getDimension(R.dimen.ctl_line_height))
        val channel_gap_height = floor(this.resources.getDimension(R.dimen.channel_gap_size))

        val opus_manager = this.get_opus_manager()
        var running_height = line_height // Include top line
        for (c in opus_manager.channels.indices) {
            var working_channel_height = 0f
            val channel = opus_manager.get_channel(c)
            for (l in channel.lines.indices) {
                var working_line_height = line_height
                val line  = channel.get_line(l)

                for ((_, controller) in line.controllers.get_all()) {
                    if (!controller.visible) continue
                    working_line_height += ctl_line_height
                }


                val pivot_a = (running_height + working_channel_height).toInt()
                val pivot_b = (running_height + ((working_channel_height + working_line_height) / 2F)).toInt()
                val pivot_c = (running_height + working_channel_height + working_line_height).toInt()
                this._drag_handle.line_drag_map.add(Triple(pivot_a until pivot_b, Pair(c, l), true))
                this._drag_handle.line_drag_map.add(Triple(pivot_b until pivot_c, Pair(c, l), false))
                working_channel_height += working_line_height
            }

            for ((_, controller) in channel.controllers.get_all()) {
                if (!controller.visible) continue
                working_channel_height += ctl_line_height
            }

            val pivot_a = running_height.toInt()
            val pivot_b = (running_height + (working_channel_height / 2F)).toInt()
            val pivot_c = (running_height + working_channel_height).toInt()
            this._drag_handle.channel_drag_map.add(Triple(pivot_a until pivot_b, c, true))
            this._drag_handle.channel_drag_map.add(Triple(pivot_b until pivot_c, c, false))

            running_height += working_channel_height + channel_gap_height
        }
    }

    /**
     * Scroll while dragging. logic different than standard touch and drag to scroll.
     * scroll up if toward top of view. scroll down if toward bottom of view.
     */
    private fun handle_in_drag_scroll() {
        if (!this._drag_handle.is_dragging()) return
        val div = 4F
        val active_zone_height = this.height.toFloat() / div
        val max_scroll_speed = 50

        val downscroll_y_position = active_zone_height * (div - 1F)
        val relative_y = this.touch_position_y - this.scrollY

        val factor: Float = if (relative_y < active_zone_height) {
            -1F * ((active_zone_height - relative_y) / active_zone_height).pow(2F)
        } else if (relative_y > downscroll_y_position) {
            ((active_zone_height - (relative_y - downscroll_y_position)) / active_zone_height).pow(2F)
        } else {
            return
        }

        this.scrollBy(0,  (max_scroll_speed * factor).toInt())
    }
}
