package com.qfs.radixulous

import android.graphics.Color
import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.qfs.radixulous.opusmanager.BeatKey

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_opustree.view.*


class MainActivity : AppCompatActivity() {
    private lateinit var opus_manager: OpusManager
    var tree_view_cache = HashMap<Pair<BeatKey, List<Int>>, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.opus_manager  = OpusManager()
        opus_manager.new()
        opus_manager.split_tree(BeatKey(0,0,0), listOf(), 2)
        //opus_manager.split_tree(BeatKey(0,0,0), listOf(0,0), 3)

        opus_manager.set_event(BeatKey(0,0,0), listOf(0), OpusEvent(
            35,
            12,
            0,
            false
        ))

        opus_manager.set_event(BeatKey(0,0,0), listOf(1), OpusEvent(
        36,
        12,
        0,
        false
        ))

        opus_manager.set_event(BeatKey(0,0,1), listOf(), OpusEvent(
            21,
            12,
            0,
            false
        ))

        opus_manager.new_line(0)

        this.populateTable()
    }

    fun populateTable() {
        for (channel in 0 until this.opus_manager.channel_lines.size) {
            for (line_offset in 0 until this.opus_manager.channel_lines[channel].size) {
                var rowView = TableRow(this.tlOpusLines.context)
                this.tlOpusLines.addView(rowView)
                for (i in 0 until this.opus_manager.channel_lines[channel][line_offset].size) {
                    var beat_key = BeatKey(channel, line_offset, i)
                    this.inflateOpusTreeView(rowView, beat_key, listOf())
                }
            }
        }
    }

    fun inflateOpusTreeView(parent: ViewGroup, beat_key: BeatKey, position: List<Int>) {
        var view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_opustree,
            parent,
            false
        )
        val tree = this.opus_manager.get_tree(beat_key, position)
        if (tree.is_event()) {
            view.tvOpenBrace.setVisibility(View.GONE)
            view.tvCloseBrace.setVisibility(View.GONE)
            var event = tree.get_event()!!
            var numberstr: String
            if (event.relative) {
                numberstr = "T"
            } else {
                numberstr = get_number_string(event.note, event.radix, 2)
            }
            view.btnValue.text = numberstr

            view.btnValue.setOnClickListener {
                var cursor = this.opus_manager.cursor
                var previous_view = this.tree_view_cache[Pair(cursor.get_beatkey(), cursor.get_position())]
                previous_view?.btnValue?.setBackgroundColor(Color.parseColor("#adbeef"))

                cursor.set_by_beatkey_position(beat_key, position)
                it.btnValue.setBackgroundColor(Color.parseColor("#ff0000"))
            }
        } else if (tree.is_leaf()) {
            view.tvOpenBrace.setVisibility(View.GONE)
            view.tvCloseBrace.setVisibility(View.GONE)
            view.btnValue.text = ".."
            view.btnValue.setOnClickListener {
                var cursor = this.opus_manager.cursor
                var previous_view = this.tree_view_cache[Pair(cursor.get_beatkey(), cursor.get_position())]
                previous_view?.btnValue?.setBackgroundColor(Color.parseColor("#adbeef"))

                cursor.set_by_beatkey_position(beat_key, position)
                it.btnValue.setBackgroundColor(Color.parseColor("#ff0000"))
            }
        } else {
            view.btnValue.setVisibility(View.GONE)
            for (i in tree.divisions.keys) {
                var new_position = position.toMutableList()
                new_position.add(i)
                inflateOpusTreeView(view.llSubTree, beat_key, new_position)
            }
        }

        this.tree_view_cache[Pair(beat_key, position)] = view
        parent.addView(view)
    }
}