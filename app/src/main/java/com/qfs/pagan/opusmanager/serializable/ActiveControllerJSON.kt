package com.qfs.pagan.opusmanager.serializable
import com.qfs.pagan.opusmanager.ControlEventType
import com.qfs.pagan.opusmanager.OpusControlEvent
import kotlinx.serialization.Serializable

@Serializable
data class ActiveControllerJSON(
    var type: ControlEventType,
    var initial_value: OpusControlEvent,
    var children: List<Pair<Int, OpusTreeJSON<OpusControlEvent>>>
)
