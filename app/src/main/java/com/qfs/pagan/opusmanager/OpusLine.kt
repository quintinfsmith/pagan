package com.qfs.pagan.opusmanager

import com.qfs.pagan.structure.OpusTree
import com.qfs.json.*

class OpusLine(beats: MutableList<OpusTree<OpusEventSTD>>): OpusLineAbstract(beats) {
    constructor(beat_count: Int) : this(Array<OpusTree<OpusEventSTD>>(beat_count) { OpusTree() }.toMutableList())

    override fun to_json(): ParsedHashMap {
        val line_map = HashMap<String, ParsedObject?>()

        val static_value = this.static_value
        line_map["static_value"] = if (static_value == null) {
            null
        } else {
            ParsedInt(static_value)
        }

        val line_beats = mutableListOf<OpusTreeJSON<OpusEventSTD>?>()
        for (beat in line.beats) {
            if (channel.midi_channel == 9) {
                beat.traverse { _: OpusTree<OpusEventSTD>, event: OpusEventSTD? ->
                    if (event != null) {
                        event.note = channel.static_value ?: OpusLayerBase.DEFAULT_PERCUSSION
                    }
                }
            }
            line_beats.add(beat.to_json())
        }

        lines.add(
            LineJSONData(
                beats = line_beats,
                controllers = line.controllers.to_json()
            )
        )

        return ParsedHashMap(line_map)
    }
}

