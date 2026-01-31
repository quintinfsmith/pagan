package com.qfs.pagan.structure.opusmanager

import androidx.compose.ui.graphics.Color
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.pagan.jsoninterfaces.OpusTreeJSONInterface
import com.qfs.pagan.structure.opusmanager.base.OpusColorPalette.OpusColorPalette
import com.qfs.pagan.structure.opusmanager.base.InstrumentEvent
import com.qfs.pagan.structure.opusmanager.base.OpusLine
import com.qfs.pagan.structure.opusmanager.base.OpusLineAbstract
import com.qfs.pagan.structure.opusmanager.base.OpusLinePercussion
import com.qfs.pagan.structure.opusmanager.base.PercussionEvent
import com.qfs.pagan.structure.opusmanager.base.TunedInstrumentEvent
import com.qfs.pagan.structure.rationaltree.ReducibleTree

object OpusLineJSONInterface {
    fun to_json(line: OpusLineAbstract<*>): JSONHashMap {
        var output = JSONHashMap()

        val beats = JSONList()
        for (i in line.beats.indices) {
            val generalized_tree = OpusTreeJSONInterface.to_json(line.beats[i]) { opus_event: InstrumentEvent ->
                InstrumentEventJSONInterface.to_json(opus_event)
            } ?: continue

            beats.add(
                JSONList(
                    JSONInteger(i),
                    generalized_tree
                )
            )
        }

        output["beats"] = beats
        output["controllers"] = ActiveControlSetJSONInterface.to_json(line.controllers)
        output["muted"] = line.muted
        output["palette"] = line.palette.to_json()

        when (line) {
            is OpusLinePercussion -> {
                output["instrument"] = JSONInteger(line.instrument)
            }
        }

        return output
    }

    fun percussion_line(input: JSONHashMap, size: Int): OpusLinePercussion {
        val beats = input.get_list("beats")
        val beat_list = MutableList<ReducibleTree<PercussionEvent>>(size) { ReducibleTree() }
        for (i in 0 until beats.size) {
            val pair = beats.get_list(i)
            val beat_index = pair.get_int(0)
            val tree = OpusTreeJSONInterface.from_json(pair.get_hashmap(1)) { event: JSONHashMap? ->
                if (event != null) {
                    InstrumentEventJSONInterface.from_json(event) as PercussionEvent
                } else {
                    null
                }
            }

            beat_list[beat_index] = tree
        }

        val output = OpusLinePercussion(
            input.get_int("instrument"),
            beat_list
        )

        this._interpret_general(input, output)

        return output
    }

    fun opus_line(input: JSONHashMap, size: Int): OpusLine {
        val beats = input.get_list("beats")

        val beat_list = MutableList<ReducibleTree<TunedInstrumentEvent>>(size) { ReducibleTree() }
        for (i in 0 until beats.size) {
            val pair = beats.get_list(i)
            val beat_index = pair.get_int(0)
            val tree = OpusTreeJSONInterface.from_json(pair.get_hashmap(1)) { event: JSONHashMap? ->
                event?.let {
                    InstrumentEventJSONInterface.from_json(it) as TunedInstrumentEvent
                }
            }

            beat_list[beat_index] = tree
        }

        val output = OpusLine(beat_list)

        this._interpret_general(input, output)

        return output
    }

    private fun _interpret_general(input: JSONHashMap, output: OpusLineAbstract<*>) {
        output.controllers = ActiveControlSetJSONInterface.from_json(input.get_hashmap("controllers"), output.beat_count())
        output.muted = input.get_boolean("muted", false)

        input.get_hashmapn("palette")?.let {
            output.palette = OpusColorPalette.from_json(it)
        }

        // Previous Line Colors ------------
        input.get_stringn("color")?.let {
            output.palette.event = Color(
                alpha = 0xFF,
                red = it.substring(1, 3).toInt(16),
                green = it.substring(3, 5).toInt(16),
                blue = it.substring(5, 7).toInt(16)
            )
        }
        // ------------------------------------
    }
}
