package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable

@Serializable
data class ChannelJSONData1(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<OpusTreeJSON<OpusEventSTD>>,
    var line_volumes: List<Int>,
    var line_static_values: List<Int?> = listOf()
)
