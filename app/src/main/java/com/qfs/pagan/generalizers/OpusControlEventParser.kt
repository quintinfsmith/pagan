package com.qfs.pagan.opusmanager
import com.qfs.json.ParsedHashMap
import com.qfs.json.ParsedInt

class OpusControlEventParser {
    companion object {
        fun to_json(input: OpusControlEvent): ParsedHashMap {
            val output = ParsedHashMap()
            when (input) {
                is OpusTempoEvent -> {
                    output["tempo"] = input.value
                }
                is OpusVolumeEvent -> {
                    output["volume"] = input.value
                    output["transition"] = input.transition
                }
                is OpusReverbEvent -> {
                    output["wetness"] = input.value
                }
            }
            return output
        }

        fun convert_v2_to_v3(input: ParsedHashMap): ParsedHashMap {
            val output = ParsedHashMap()
            when (input.get_string("type")) {
                "com.qfs.pagan.opusmanager.OpusTempoEvent" -> {
                    output["tempo"] = input["value"]
                }
                "com.qfs.pagan.opusmanager.OpusVolumeEvent" -> {
                    output["volume"] = input["value"]
                    output["transition"] = ParsedInt(0)
                }
                else -> throw Exception() // Unreachable, nothing else was implemented
            }

            return output
        }

        // from_jsons-----------------
        fun tempo_event(map: ParsedHashMap): OpusTempoEvent {
            return OpusTempoEvent(map.get_float("tempo"))
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
