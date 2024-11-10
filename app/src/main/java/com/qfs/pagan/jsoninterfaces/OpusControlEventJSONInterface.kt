package com.qfs.pagan.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger

class OpusControlEventJSONInterface {
    companion object {
        fun to_json(input: OpusControlEvent): JSONHashMap {
            val output = JSONHashMap()
            output["duration"] = input.duration
            when (input) {
                is OpusTempoEvent -> {
                    output["tempo"] = input.value
                }
                is OpusVolumeEvent -> {
                    output["volume"] = input.value
                    output["transition"] = input.transition.name
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
            return OpusTempoEvent(map.get_float("tempo"), map.get_int("duration", 1))
        }
        fun volume_event(map: JSONHashMap): OpusVolumeEvent {
            return OpusVolumeEvent(
                map.get_int("volume"),
                /* Note: Need the try catch since I initially had transitions as int, but only used 0 */
                try {
                    ControlTransition.valueOf(map.get_string("transition", "Instant"))
                } catch (e: ClassCastException) {
                    ControlTransition.Instant
                },
                map.get_int("duration", 1)
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
