package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable

@Serializable
data class ChannelJSONData(
    var midi_channel: Int,
    var midi_instrument: Int,
    var lines: List<String>,
    var line_volumes: List<Int>
)

@Serializable
data class LoadedJSONData(
    var tempo: Float,
    var radix: Int,
    var channels: List<ChannelJSONData>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String = "New Opus"
)

