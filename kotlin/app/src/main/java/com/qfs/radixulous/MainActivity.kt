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
import com.qfs.radixulous.opusmanager.BeatKey

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.contextmenu_cell.*
import kotlinx.android.synthetic.main.contextmenu_cell.view.*
import kotlinx.android.synthetic.main.item_opusbeat.view.*
import kotlinx.android.synthetic.main.item_opusbutton.view.*
import kotlinx.android.synthetic.main.table_cell_label.view.*


class MainActivity : AppCompatActivity() {
    private lateinit var opus_manager: OpusManager
    var tree_view_cache = HashMap<Pair<BeatKey, List<Int>>, View>()
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
                this.setContextMenu(2)
            }
            rowView.addView(headerCellView)
        }

        for (channel in 0 until this.opus_manager.channel_lines.size) {
            for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                rowView = TableRow(this.tlOpusLines.context)
                rowView.setPadding(0,0,0,0)

                this.tlOpusLines.addView(rowView)
                var rowLabel = LayoutInflater.from(rowView.context).inflate(
                    R.layout.table_cell_label,
                    rowView,
                    false
                )

                rowLabel.textView.text = "${channel}:${line_offset}"
                rowLabel.textView.setOnClickListener {
                    this.setContextMenu(1)
                }

                rowView.addView(rowLabel)

                for (i in 0 until this.opus_manager.channel_lines[channel][line_offset].size) {
                    var beat_key = BeatKey(channel, line_offset, i)
                    this.inflateOpusBeatView(rowView, beat_key)
                }
            }
        }
    }

    fun inflateOpusBeatView(rowView: TableRow, beat_key: BeatKey) {
        var middleView = LayoutInflater.from(rowView.context).inflate(
            R.layout.item_opusbeat,
            rowView,
            false
        )
        this.tree_view_cache[Pair(beat_key, listOf())] = middleView.llBeat
        this.buildTreeView(middleView.llBeat, beat_key, listOf())
        rowView.addView(middleView)
    }

    fun buildTreeView(parent: LinearLayout, beat_key: BeatKey, position: List<Int>, offset: Int? = null) {
        var tree = this.opus_manager.get_tree(beat_key, position)

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

            leafView.button.setOnClickListener {
                var cursor = this.opus_manager.cursor
                var previous_view = this.tree_view_cache[Pair(cursor.get_beatkey(), cursor.get_position())]
                previous_view?.button?.setBackgroundColor(Color.parseColor("#adbeef"))

                cursor.set_by_beatkey_position(beat_key, position)
                it.button.setBackgroundColor(Color.parseColor("#ff0000"))
                this.setContextMenu(3)
            }

            this.tree_view_cache[Pair(beat_key, position)] = leafView
            if (offset != null) {
                parent.addView(leafView, offset)
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
                this.buildTreeView(treeLayout, beat_key, new_position)
            }

            if (tree.size > 1) {
                var close_brace = TextView(parent.context)
                close_brace.text = "]"
                treeLayout.addView(close_brace)
            }
            if (offset != null) {
                parent.addView(treeLayout, offset)
            } else {
                parent.addView(treeLayout)
            }
        }
    }

    fun setContextMenu(menu_index: Int) {
        this.active_context_menu_index = menu_index
        var view = this.active_context_menu_view
        (view?.parent as? ViewGroup)?.removeView(view)

        when (menu_index) {
            1 -> {
                this.active_context_menu_view = LayoutInflater.from(this.llContextMenu.context).inflate(
                    R.layout.contextmenu_row,
                    this.llContextMenu,
                    false
                )
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

                var opus_manager = opus_manager
                var current_tree = opus_manager.get_tree_at_cursor()
                var that = this

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
                            that.tree_view_cache[Pair(beatkey, position)]?.button?.text = get_number_string(event.note, event.radix, 2)
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
                                that.tree_view_cache[Pair(beatkey, position)]?.button?.text = get_number_string(event.note, event.radix, 2)
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

                        var view = that.tree_view_cache[Pair(beat_key, position)]!!
                        var view_index: Int? = null

                        var parent = view.parent as LinearLayout
                        for (i in 0 until parent.getChildCount()) {
                            if (view == parent.getChildAt(i)) {
                                parent.removeView(view)
                                view_index = i
                                break
                            }
                        }

                        that.buildTreeView(parent as LinearLayout, beat_key, position, view_index)
                    }

                    this.clButtons.btnUnset?.setOnClickListener {
                        that.opus_manager.unset_at_cursor()

                        var cursor = that.opus_manager.cursor
                        var beat_key = cursor.get_beatkey()
                        var position = cursor.get_position()
                        that.sbOffset.progress = 0
                        that.sbOctave.progress = 0
                        that.tree_view_cache[Pair(beat_key, position)]?.button?.text = ".."
                    }

                    this.clButtons.btnRemove?.setOnClickListener {
                        var cursor = that.opus_manager.cursor
                        var beat_key = cursor.get_beatkey()
                        var position = cursor.get_position()

                        var view = that.tree_view_cache[Pair(beat_key, position)]!!
                        var view_parent = view.parent as LinearLayout
                        view_parent.removeView(view)

                        that.opus_manager.remove_tree_at_cursor()
                        var new_position = position.toMutableList()
                        new_position.removeLast()
                        var beat_view = that.tree_view_cache[Pair(beat_key, listOf())] as LinearLayout
                        for (_i in 0 until beat_view.getChildCount()) {
                            beat_view.removeViewAt(0)
                        }

                        that.buildTreeView(beat_view, beat_key, listOf())

                    }
                }

                this.llContextMenu.addView(this.active_context_menu_view)
            }
        }
    }
}