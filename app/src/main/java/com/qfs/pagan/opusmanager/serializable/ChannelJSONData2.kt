package com.qfs.pagan.opusmanager.serializable
import com.qfs.pagan.opusmanager.OpusEventSTD
import kotlinx.serialization.Serializable

@Serializable
data class ChannelJSONData2(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<OpusTreeJSON<OpusEventSTD>>,
    var line_static_values: List<Int?> = listOf(),
    var line_controllers: List<List<ActiveControllerJSON>>,
    var channel_controllers: List<ActiveControllerJSON>,
)
