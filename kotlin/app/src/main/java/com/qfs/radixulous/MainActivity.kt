package com.qfs.radixulous

import android.graphics.Color
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.qfs.radixulous.opusmanager.BeatKey

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.contextmenu_cell.*
import kotlinx.android.synthetic.main.contextmenu_cell.view.*
import kotlinx.android.synthetic.main.contextmenu_row.view.*
import kotlinx.android.synthetic.main.item_opusbeat.view.*
import kotlinx.android.synthetic.main.item_opusbutton.view.*
import kotlinx.android.synthetic.main.table_cell_label.view.*


class MainActivity : AppCompatActivity() {
    private lateinit var opus_manager: OpusManager
    // TODO: a clean cache system
    var view_cache: MutableList<Pair<LinearLayout, HashMap<Pair<Int, List<Int>>, View>>> = mutableListOf()
    var line_label_cache: MutableList<Button> = mutableListOf()
    private var current_cursor_position: Triple<Int, Int, List<Int>>? = null
    private var active_context_menu_index: Int = 0
    private var active_context_menu_view: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.opus_manager  = OpusManager()
        opus_manager.new()
        opus_manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        //opus_manager.split_tree(BeatKey(0,0,0), listOf(0), 3)

        opus_manager.set_event(BeatKey(0,0,0), listOf(0), OpusEvent(
            35,
            12,
            0,
            false
        ))

        opus_manager.set_event(BeatKey(0,0,0), listOf(1), OpusEvent(
            35,
            12,
            0,
            false
        ))
        opus_manager.split_tree(BeatKey(0,0,0), listOf(0),3)

        opus_manager.set_event(BeatKey(0,0,0), listOf(0,1), OpusEvent(
        36,
        12,
        0,
        false
        ))

        opus_manager.new_line(0)

        this.populateTable()
    }

    fun populateTable() {
        var rowView = TableRow(this.tlOpusLines.context)
        this.tlOpusLines.addView(rowView)
        rowView.addView(TextView(rowView.context))
        for (i in 0 until this.opus_manager.opus_beat_count) {
            var headerCellView = LayoutInflater.from(rowView.context).inflate(
                R.layout.table_cell_label,
                rowView,
                false
            )
            headerCellView.textView.text = "${i}"
            headerCellView.textView.setOnClickListener {
                this.opus_manager.jump_to_beat(i)
                this.setContextMenu(2)
                var cursor = this.opus_manager.cursor
                this.set_cursor_position(cursor.y, cursor.x, cursor.get_position())
            }
            rowView.addView(headerCellView)
        }

        var y = 0
        for (channel in 0 until this.opus_manager.channel_lines.size) {
            for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                this.buildLineView(y)
                y += 1
            }
        }
    }

    private fun getLineViewIndex(view: LinearLayout): Int? {
        for (i in 0 until this.view_cache.size) {
            if (this.view_cache[i].first == view) {
                return i
            }
        }
        return null
    }

    private fun getTreeViewYXPosition(view: View): Triple<Int, Int, List<Int>>? {
        for (y in 0 until this.view_cache.size) {
            for (key in this.view_cache[y].second.keys) {
                if (this.view_cache[y].second[key] == view) {
                    return Triple(y, key.first, key.second)
                }
            }
        }
        return null
    }

    private fun addLineViewToCache(view: LinearLayout, y: Int) {
        if (y < this.view_cache.size) {
            this.view_cache.add(y, Pair(view, HashMap<Pair<Int, List<Int>>, View>()))
        } else {
            this.view_cache.add(Pair(view, HashMap<Pair<Int, List<Int>>, View>()))
        }
    }
    private fun addTreeViewToCache(view: View, y: Int, x: Int, position: List<Int>) {
        this.view_cache[y].second[Pair(x, position)] = view
    }

    fun buildLineView(y: Int) {
        var clo = this.opus_manager.get_channel_index(y)
        var channel = clo.first
        var line_offset = clo.second

        var rowView = TableRow(this.tlOpusLines.context)
        rowView.setPadding(0,0,0,0)

        this.tlOpusLines.addView(rowView)
        this.addLineViewToCache(rowView, y)

        var rowLabel = LayoutInflater.from(rowView.context).inflate(
            R.layout.table_cell_label,
            rowView,
            false
        )
        var that = this
        rowLabel.textView.text = "${channel}:${line_offset}"
        rowLabel.textView.setOnClickListener {
            var y: Int? = that.getLineViewIndex(rowView)
            var cursor = that.opus_manager.cursor

            this.setContextMenu(1)
            if (y != null) {
                this.set_cursor_position(y!!, cursor.x, cursor.get_position())
            }
        }
        this.line_label_cache.add(y, rowLabel.textView)
        rowView.addView(rowLabel)

        for (x in 0 until this.opus_manager.channel_lines[channel][line_offset].size) {
            this.buildTreeView(rowView, y, x, listOf())
        }
    }

    private fun buildTreeView(parent: LinearLayout, y: Int, x: Int, position: List<Int>, offset: Int? = null) {
        var channel_index = this.opus_manager.get_channel_index(y)
        var tree = this.opus_manager.get_tree(BeatKey(channel_index.first, channel_index.second, x), position)

        if (tree.is_leaf()) {
            var leafView = LayoutInflater.from(parent.context).inflate(
                R.layout.item_opusbutton,
                parent,
                false
            )

            if (tree.is_event()) {
                var event = tree.get_event()!!
                leafView.button.text = if (event.relative) {
                    "T"
                } else {
                    get_number_string(event.note, event.radix, 2)
                }
            } else {
                leafView.button.text = ".."
            }

            var that = this
            leafView.button.setOnClickListener {
                var key = that.getTreeViewYXPosition(leafView)
                if (key != null) {
                    this.cellClickListener(key.first, key.second, key.third)
                }
            }

            this.addTreeViewToCache(leafView, y, x, position)
            if (offset != null) {
                parent.addView(leafView, offset + 1) // (+1 considers row label)
            } else {
                parent.addView(leafView)
            }

        } else {
            var treeLayout = LinearLayout(parent.context)
            if (tree.size > 1) {
                var open_brace = TextView(parent.context)
                open_brace.text = "["
                treeLayout.addView(open_brace)
            }

            for (i in 0 until tree.size) {
                var new_position = position.toMutableList()
                new_position.add(i)
                this.buildTreeView(treeLayout, y, x, new_position)
            }

            if (tree.size > 1) {
                var close_brace = TextView(parent.context)
                close_brace.text = "]"
                treeLayout.addView(close_brace)
            }
            if (offset != null) {
                parent.addView(treeLayout, offset + 1) // (+1 considers row label)
            } else {
                parent.addView(treeLayout)
            }
            this.addTreeViewToCache(treeLayout, y, x, position)
        }
    }

    fun setContextMenu(menu_index: Int) {
        this.active_context_menu_index = menu_index
        var view = this.active_context_menu_view
        (view?.parent as? ViewGroup)?.removeView(view)

        var opus_manager = opus_manager
        var current_tree = opus_manager.get_tree_at_cursor()
        var position = opus_manager.cursor.get_position()
        var that = this

        when (menu_index) {
            1 -> {
                this.active_context_menu_view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_row,
                    this.llContextMenu,
                    false
                )

                this.active_context_menu_view!!.apply {
                    this.btnRemoveLine.setOnClickListener {
                        var cursor = that.opus_manager.cursor

                        that.opus_manager.remove_line_at_cursor()

                        var y = cursor.y
                        var viewpair = that.view_cache[y]

                        if (viewpair != null) {
                            that.tlOpusLines.removeView(viewpair.first)
                            that.view_cache.removeAt(y)
                            that.line_label_cache.removeAt(y)
                        }

                        that.set_cursor_position(cursor.y, cursor.x, cursor.get_position())

                        var beatkey = cursor.get_beatkey()
                        that.line_label_cache[cursor.y].text = "${beatkey.channel}:${beatkey.line_offset}"
                    }

                    this.btnInsertLine.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var beat_key = cursor.get_beatkey()
                        that.opus_manager.new_line_at_cursor()
                        var new_line_channel = beat_key.channel
                        var new_line_offset = that.opus_manager.channel_lines[new_line_channel].size - 1
                        var line_y = that.opus_manager.get_y(new_line_channel, new_line_offset)
                        that.buildLineView(line_y)
                    }
                }

                this.llContextMenu.addView(this.active_context_menu_view)
            }
            2 -> {
                this.active_context_menu_view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_column,
                    this.llContextMenu,
                    false
                )
                this.llContextMenu.addView(this.active_context_menu_view)
            }
            3 -> {
                this.active_context_menu_view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_cell,
                    this.llContextMenu,
                    false
                )

                this.active_context_menu_view!!.apply {
                    if (current_tree.is_event()) {
                        var event = current_tree.get_event()!!
                        this.sbOffset.progress = event.note % event.radix
                        this.sbOctave.progress = event.note / event.radix
                    }
                    this.sbOffset?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                            if (! fromUser) {
                                return
                            }
                            var cursor = opus_manager.cursor
                            var position = cursor.position
                            var beatkey = cursor.get_beatkey()

                            var event = if (current_tree.is_event()) {
                                var event = current_tree.get_event()!!
                                var old_octave = event.note / event.radix
                                event.note = (old_octave * event.radix) + progress

                                event
                            } else {
                                OpusEvent(
                                    progress,
                                    opus_manager.RADIX,
                                    beatkey.channel,
                                    false
                                )
                            }

                            that.opus_manager.set_event(beatkey, position, event)
                            that.view_cache[cursor.y].second[Pair(cursor.x, position)]?.button?.text = get_number_string(event.note, event.radix, 2)
                        }

                        override fun onStartTrackingTouch(seek: SeekBar) {

                        }

                        override fun onStopTrackingTouch(seek: SeekBar) {

                        }
                    }
                    )

                    this.sbOctave?.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                                if (! fromUser) {
                                    return
                                }
                                var cursor = opus_manager.cursor
                                var position = cursor.position
                                var beatkey = cursor.get_beatkey()

                                var event = if (current_tree.is_event()) {
                                    var event = current_tree.get_event()!!
                                    event.note = (progress * event.radix) + (event.note % event.radix)
                                    event
                                } else {
                                    OpusEvent(
                                        progress * opus_manager.RADIX,
                                        opus_manager.RADIX,
                                        beatkey.channel,
                                        false
                                    )
                                }

                                that.opus_manager.set_event(beatkey, position, event)
                                that.view_cache[cursor.y].second[Pair(cursor.x, position)]?.button?.text = get_number_string(event.note, event.radix, 2)
                            }

                            override fun onStartTrackingTouch(seek: SeekBar) {

                            }

                            override fun onStopTrackingTouch(seek: SeekBar) {

                            }
                        }
                    )

                    this.clButtons.btnSplit?.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var beat_key = cursor.get_beatkey()
                        var position = cursor.get_position()
                        that.opus_manager.split_tree_at_cursor()

                        var view = that.view_cache[cursor.y].second[Pair(cursor.x, position)]
                        if (view != null) {
                            var view_index: Int? = null

                            var parent = view.parent as LinearLayout
                            for (i in 0 until parent.getChildCount()) {
                                if (view == parent.getChildAt(i)) {
                                    parent.removeView(view)
                                    view_index = i
                                    break
                                }
                            }

                            that.buildTreeView(parent, cursor.y, cursor.x, position, view_index)
                            that.cellClickListener(cursor.y, cursor.x, cursor.get_position())
                        }
                    }

                    this.clButtons.btnUnset?.setOnClickListener {
                        that.opus_manager.unset_at_cursor()

                        var cursor = that.opus_manager.cursor
                        var position = cursor.get_position()
                        that.sbOffset.progress = 0
                        that.sbOctave.progress = 0

                        that.view_cache[cursor.y].second[Pair(cursor.x, position)]?.button?.text = ".."
                        that.cellClickListener(cursor.y, cursor.x, position)
                    }

                    this.clButtons.btnRemove?.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var position = cursor.get_position()


                        if (position.isNotEmpty()) {
                            that.opus_manager.remove_tree_at_cursor()
                            that.rebuildBeatView(cursor.y, cursor.x)
                            that.cellClickListener(cursor.y, cursor.x, cursor.get_position())
                        }
                    }

                    this.clButtons.btnInsert?.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var position = cursor.get_position()
                        if (position.isEmpty()) {
                            that.opus_manager.split_tree_at_cursor()
                        } else {
                            that.opus_manager.insert_after_cursor()
                        }

                        that.rebuildBeatView(cursor.y, cursor.x)
                        that.cellClickListener(cursor.y, cursor.x, cursor.get_position())
                    }
                }

                this.llContextMenu.addView(this.active_context_menu_view)
            }
        }
    }

    fun rebuildBeatView(y: Int, x: Int) {
        var beat_view = this.view_cache[y].second[Pair(x, listOf())]

        var delkeys: MutableList<Pair<Int, List<Int>>> = mutableListOf()
        for (key in this.view_cache[y].second.keys) {
            if (key.first == x) {
                delkeys.add(key)
            }
        }
        (beat_view?.parent as ViewGroup).removeView(beat_view)
        for (key in delkeys) {
            this.view_cache[y].second.remove(key)
        }

        var rowView = this.view_cache[y].first
        this.buildTreeView(rowView, y, x, listOf(), x)
    }

    fun set_cursor_position(y: Int, x: Int, position: List<Int>) {
        var c = this.current_cursor_position
        if (c != null) {
            if (c.first < this.opus_manager.line_count() && c.second < this.opus_manager.opus_beat_count) {
                var previous_view = this.view_cache[c.first].second[Pair(c.second, c.third)]
                previous_view?.button?.setBackgroundColor(Color.parseColor("#adbeef"))
            }
        }

        var cursor = this.opus_manager.cursor
        cursor.y = y
        cursor.x = x
        cursor.position = position.toMutableList()
        cursor.settle()

        this.current_cursor_position = Triple(y, x, cursor.get_position())
        var view = this.view_cache[y].second[Pair(x, cursor.position)]

        view?.button?.setBackgroundColor(Color.parseColor("#ff0000"))
    }

    fun cellClickListener(y: Int, x: Int, position: List<Int>) {
        this.set_cursor_position(y, x, position)
        this.setContextMenu(3)
    }
}
