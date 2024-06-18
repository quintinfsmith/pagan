package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable

@Serializable
data class LineJSONData(
    var static_value: Int? = null,
    var beats: List<OpusTreeJSON<OpusEventSTD>?>,
    var controllers: List<ActiveControllerJSON>
)
