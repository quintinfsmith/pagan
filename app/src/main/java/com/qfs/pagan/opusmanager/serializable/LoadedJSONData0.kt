package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable

@Serializable
data class LoadedJSONData0(
    var tempo: Float,
    var radix: Int,
    var channels: List<ChannelJSONData0>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String
)
