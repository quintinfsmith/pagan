package com.qfs.radixulous

import android.graphics.Color
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import com.qfs.radixulous.opusmanager.BeatKey

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_opusbeat.view.*
import kotlinx.android.synthetic.main.item_opusbutton.view.*
import kotlinx.android.synthetic.main.table_cell_label.view.*


class MainActivity : AppCompatActivity() {
    private lateinit var opus_manager: OpusManager
    var tree_view_cache = HashMap<Pair<BeatKey, List<Int>>, View>()

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
         //       rowView.setBackgroundColor(listOf(Color.parseColor("#eaeaea"), Color.parseColor("#888888"))[(line_offset + channel) % 2])
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

        for (view in this.getTreeViews(middleView.llBeat, beat_key, listOf())) {
            middleView.llBeat.addView(view)
        }
        rowView.addView(middleView)
    }

    fun setContextMenu(menu_index: Int) {
        this.tlContextRow.setVisibility(View.INVISIBLE)
        this.tlContextColumn.setVisibility(View.INVISIBLE)
        this.tlContextLeaf.setVisibility(View.INVISIBLE)

        when (menu_index) {
            1 -> {
                this.tlContextRow.setVisibility(View.VISIBLE)
            }
            2 -> {
                this.tlContextColumn.setVisibility(View.VISIBLE)
            }
            3 -> {
                this.tlContextLeaf.setVisibility(View.VISIBLE)
            }
        }
    }

    fun getTreeViews(parent: LinearLayout, beat_key: BeatKey, position: List<Int>): List<View> {
        var output: MutableList<View> = mutableListOf()
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
            output.add(leafView)
        } else {
            if (tree.size > 1) {
                var open_brace = TextView(parent.context)
                open_brace.text = "["
                output.add(open_brace)
            }

            for (i in 0 until tree.size) {
                var new_position = position.toMutableList()
                new_position.add(i)

                for (subview in this.getTreeViews(parent, beat_key, new_position)) {
                    output.add(subview)
                }
            }

            if (tree.size > 1) {
                var close_brace = TextView(parent.context)
                close_brace.text = "]"
                output.add(close_brace)
            }
        }


        return output
    }

}