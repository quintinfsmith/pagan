package com.qfs.radixulous

import com.qfs.radixulous.structure.OpusTree
import com.qfs.radixulous.opusmanager.OpusEvent
import com.qfs.radixulous.opusmanager.HistoryLayer as OpusManager
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.qfs.radixulous.opusmanager.BeatKey

import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var opusline_adapter: OpusLineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val opus_manager  = OpusManager()
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

        var mutlist: MutableList<MutableList<OpusTree<OpusEvent>>> = mutableListOf()
        for (i in 0 until opus_manager.channel_lines[0].size) {
            mutlist.add(opus_manager.channel_lines[0][i])
        }
        opusline_adapter = OpusLineAdapter(mutlist)

        rvOpusLines.adapter = opusline_adapter
        rvOpusLines.layoutManager = LinearLayoutManager(this)

    }
}