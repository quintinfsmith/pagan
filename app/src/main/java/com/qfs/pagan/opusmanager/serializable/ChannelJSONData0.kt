package com.qfs.pagan.opusmanager.serializable
import kotlinx.serialization.Serializable

@Serializable
data class ChannelJSONData0(
    var midi_channel: Int,
    var midi_bank: Int,
    var midi_program: Int,
    var lines: List<String>,
    var line_volumes: List<Int>
)
