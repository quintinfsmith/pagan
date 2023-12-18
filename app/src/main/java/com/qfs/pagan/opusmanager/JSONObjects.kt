package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable


@Serializable
data class ChannelJSONData(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<OpusTreeJSON>,
    var line_volumes: List<Int>
)

@Serializable
data class LoadedJSONData(
    var tempo: Float,
    var channels: List<ChannelJSONData>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String = BaseLayer.DEFAULT_NAME,
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) },
    var radix: Int = tuning_map.size
)

@Serializable
data class OpusTreeJSON(
    var event: OpusEvent?,
    var children: List<OpusTreeJSON?>?
)

// Old Fmt
@Serializable
data class ChannelJSONData0(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<String>,
    var line_volumes: List<Int>
)
@Serializable
data class LoadedJSONData0(
    var tempo: Float,
    var radix: Int,
    var channels: List<ChannelJSONData0>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String = BaseLayer.DEFAULT_NAME
)
