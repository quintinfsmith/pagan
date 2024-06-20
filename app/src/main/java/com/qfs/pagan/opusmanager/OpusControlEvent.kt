package com.qfs.pagan.opusmanager

import com.qfs.json.*

class OpusControlEventParser() {
    companion object {
        fun to_json(input: OpusControlEvent): ParsedHashMap {
            val output = ParsedHashMap()
            when (input) {
                is OpusTempoEvent -> {
                    output.hash_map["tempo"] = ParsedFloat(input.value)
                }
                is OpusVolumeEvent -> {
                    output.hash_map["volume"] = ParsedInt(input.value)
                    output.hash_map["transition"] = ParsedInt(input.transition)
                }
                is OpusReverbEvent -> {
                    output.hash_map["wetness"] = ParsedFloat(input.value)
                }
            }
            return output
        }

        // from_jsons-----------------

        fun tempo_event(map: ParsedHashMap): OpusTempoEvent {
            return OpusTempoEvent(map.get_int("tempo")))
        }
        fun volume_event(map: ParsedHashMap): OpusVolumeEvent {
            return OpusVolumeEvent(
                map.get_int("volume"),
                map.get_int("transition", 0)
            )
        }
        fun reverb_event(map: ParsedHashMap): OpusReverbEvent {
            return OpusReverbEvent(
                map.get_float("wetness")
            )
        }
        // ------------------------
    }
}

abstract class OpusControlEvent()

enum class ControlEventType {
    Tempo,
    Volume,
    Reverb,
}

class OpusTempoEvent(var value: Float): OpusControlEvent()
class OpusVolumeEvent(var value: Int, var transition: Int = 0): OpusControlEvent()
class OpusReverbEvent(var value: Float): OpusControlEvent()

