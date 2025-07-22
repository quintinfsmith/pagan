package com.qfs.pagan.structure.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.pagan.jsoninterfaces.UnknownEventTypeException
import com.qfs.pagan.structure.opusmanager.base.ControlTransition
import com.qfs.pagan.structure.opusmanager.base.OpusControlEvent
import com.qfs.pagan.structure.opusmanager.base.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.OpusVolumeEvent

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
                is OpusPanEvent -> {
                    output["value"] = input.value
                    output["transition"] = input.transition.name
                }
            }
            return output
        }

        fun convert_v2_to_v3(input: JSONHashMap): JSONHashMap {
            val output = JSONHashMap()
            val type = input.get_stringn("type")
            when (type) {
                "com.qfs.pagan.structure.opusmanager.base.OpusTempoEvent" -> {
                    output["tempo"] = input["value"]
                }
                "com.qfs.pagan.structure.opusmanager.base.OpusVolumeEvent" -> {
                    output["volume"] = input["value"]
                    output["transition"] = JSONInteger(0)
                }
                else -> throw UnknownEventTypeException(type)
            }

            return output
        }

        // from_jsons-----------------
        fun tempo_event(map: JSONHashMap): OpusTempoEvent {
            return OpusTempoEvent(map.get_float("tempo"), map.get_int("duration", 1))
        }
        fun volume_event(map: JSONHashMap): OpusVolumeEvent {
            val value = if (map.get("volume") is JSONInteger) {
                map.get_int("volume").toFloat() / 128F
            } else {
                map.get_float("volume")
            }
            return OpusVolumeEvent(
                value,
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
                map.get_float("wetness"),
                map.get_int("duration", 1)
            )
        }
        fun pan_event(map: JSONHashMap): OpusPanEvent {
            return OpusPanEvent(
                map.get_float("value"),
                ControlTransition.valueOf(map.get_string("transition", "Instant")),
                map.get_int("duration", 1)
            )
        }
        // ------------------------
    }
}
