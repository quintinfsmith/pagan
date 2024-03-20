package com.qfs.pagan.opusmanager
import kotlinx.serialization.Serializable

@Serializable
data class ChannelJSONData(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<OpusTreeJSON<OpusEventSTD>>,
    var line_static_values: List<Int?> = listOf(),
    var line_controllers: List<List<ActiveControllerJSON>> = listOf(),
    var channel_controllers: List<ActiveControllerJSON> = listOf()
)

@Serializable
data class LoadedJSONData(
    var tempo: Float,
    var channels: List<ChannelJSONData>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String = OpusLayerBase.DEFAULT_NAME,
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) },
    var radix: Int = tuning_map.size,
    var controllers: List<ActiveControllerJSON> = listOf()
)

@Serializable
data class ActiveControllerJSON(
    var type: Int,
    var children: List<Pair<Int, OpusTreeJSON<OpusControlEvent>>>
)

@Serializable
data class OpusTreeJSON<T>(
    var event: T?,
    var children: List<OpusTreeJSON<T>?>?
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
    var name: String = OpusLayerBase.DEFAULT_NAME
)

@Serializable
data class ChannelJSONData1(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<OpusTreeJSON<OpusEventSTD>>,
    var line_volumes: List<Int>,
    var line_static_values: List<Int?> = listOf()
)

@Serializable
data class LoadedJSONData1(
    var tempo: Float,
    var channels: List<ChannelJSONData1>,
    var reflections: List<List<BeatKey>>? = null,
    var transpose: Int = 0,
    var name: String = OpusLayerBase.DEFAULT_NAME,
    var tuning_map: Array<Pair<Int, Int>> = Array(12) { i: Int -> Pair(i, 12) },
    var radix: Int = tuning_map.size,
)

