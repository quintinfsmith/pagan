package com.qfs.pagan.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger

class OpusControlEventJSONInterface {
    companion object {
        fun to_json(input: OpusControlEvent): JSONHashMap {
            val output = JSONHashMap()
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

        fun convert_v2_to_v3(input: JSONHashMap): JSONHashMap {
            val output = JSONHashMap()
            when (input.get_string("type")) {
                "com.qfs.pagan.opusmanager.OpusTempoEvent" -> {
                    output["tempo"] = input["value"]
                }
                "com.qfs.pagan.opusmanager.OpusVolumeEvent" -> {
                    output["volume"] = input["value"]
                    output["transition"] = JSONInteger(0)
                }
                else -> throw Exception() // Unreachable, nothing else was implemented
            }

            return output
        }

        // from_jsons-----------------
        fun tempo_event(map: JSONHashMap): OpusTempoEvent {
            return OpusTempoEvent(map.get_float("tempo"))
        }
        fun volume_event(map: JSONHashMap): OpusVolumeEvent {
            return OpusVolumeEvent(
                map.get_int("volume"),
                map.get_int("transition", 0)
            )
        }
        fun reverb_event(map: JSONHashMap): OpusReverbEvent {
            return OpusReverbEvent(
                map.get_float("wetness")
            )
        }
        // ------------------------
    }
}
