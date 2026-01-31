package com.qfs.pagan.structure.opusmanager
import com.qfs.json.JSONHashMap
import com.qfs.json.JSONInteger
import com.qfs.json.JSONList
import com.qfs.pagan.jsoninterfaces.UnknownEventTypeException
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.EffectTransition
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.DelayEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.EffectEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusPanEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusReverbEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusTempoEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVelocityEvent
import com.qfs.pagan.structure.opusmanager.base.effectcontrol.event.OpusVolumeEvent

object OpusControlEventJSONInterface {
    fun to_json(input: EffectEvent): JSONHashMap {
        val output = JSONHashMap()
        output["duration"] = input.duration
        output["transition"] = input.transition.name
        when (input) {
            is OpusTempoEvent -> {
                output["tempo"] = input.value
            }
            is OpusVolumeEvent -> {
                output["volume"] = input.value
            }
            is OpusVelocityEvent -> {
                output["velocity"] = input.value
            }
            is OpusReverbEvent -> {
                output["wetness"] = input.value
            }
            is OpusPanEvent -> {
                output["value"] = input.value
            }
            is DelayEvent -> {
                output["frequency"] = JSONList(
                    JSONInteger(input.numerator),
                    JSONInteger(input.denominator)
                )
                output["echo"] = input.echo
                output["fade"] = input.fade
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
        return OpusTempoEvent(
            map.get_float("tempo"),
            map.get_int("duration", 1),
            EffectTransition.valueOf(map.get_string("transition", "Instant"))
        )
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
            freq.get_int(0),
            freq.get_int(1),
            map.get_int("echo"),
            map.get_float("fade"),
            map.get_int("duration", 1),
            EffectTransition.valueOf(map.get_string("transition", "Instant"))
        )
    }
    // ------------------------
}
