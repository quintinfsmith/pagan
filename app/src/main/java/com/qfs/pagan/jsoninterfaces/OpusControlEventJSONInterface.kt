package com.qfs.pagan.structure.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.pagan.jsoninterfaces.UnknownEventTypeException
import com.qfs.pagan.structure.Rational
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent

class OpusControlEventJSONInterface {
    companion object {
        fun to_json(input: EffectEvent): JSONHashMap {
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
                is OpusVelocityEvent -> {
                    output["velocity"] = input.value
                    output["transition"] = input.transition.name
                }
                is OpusReverbEvent -> {
                    output["wetness"] = input.value
                }
                is OpusPanEvent -> {
                    output["value"] = input.value
                    output["transition"] = input.transition.name
                }
                is DelayEvent -> {
                    output["frequency"] = JSONList(
                        JSONInteger(input.frequency.numerator),
                        JSONInteger(input.frequency.denominator)
                    )
                    output["echo"] = input.repeat
                    output["fade"] = input.repeat_decay
                }
            }
            return output
        }

        fun convert_v2_to_v3(input: JSONHashMap): JSONHashMap {
            val output = JSONHashMap()
            val type = input.get_stringn("type")
            when (type) {
                "com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent" -> {
                    output["tempo"] = input["value"]
                }
                "com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent" -> {
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
            val value = if (map["volume"] is JSONInteger) {
                map.get_int("volume").toFloat() / 128F
            } else {
                map.get_float("volume")
            }
            return OpusVolumeEvent(
                value,
                map.get_int("duration", 1),
                /* Note: Need the try catch since I initially had transitions as int, but only used 0 */
                try {
                    EffectTransition.valueOf(map.get_string("transition", "Instant"))
                } catch (e: ClassCastException) {
                    EffectTransition.Instant
                }
            )
        }

        fun velocity_event(map: JSONHashMap): OpusVelocityEvent {
            val value = if (map["velocity"] is JSONInteger) {
                map.get_int("velocity").toFloat() / 128F
            } else {
                map.get_float("velocity")
            }
            return OpusVelocityEvent(
                value,
                map.get_int("duration", 1),
                /* Note: Need the try catch since I initially had transitions as int, but only used 0 */
                try {
                    EffectTransition.valueOf(map.get_string("transition", "Instant"))
                } catch (e: ClassCastException) {
                    EffectTransition.Instant
                }
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
                map.get_int("duration", 1),
                EffectTransition.valueOf(map.get_string("transition", "Instant"))
            )
        }
        fun delay_event(map: JSONHashMap): DelayEvent {
            val freq = map.get_list("frequency")
            return DelayEvent(
                Rational(
                    freq.get_int(0),
                    freq.get_int(1)
                ),
                map.get_int("echo"),
                map.get_float("fade"),
            )
        }
        // ------------------------
    }
}
