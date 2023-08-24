package com.qfs.pagan.opusmanager

import kotlinx.serialization.Serializable

@Serializable
data class OpusEvent(
    var note: Int,
    var radix: Int,
    var channel: Int,
    var relative: Boolean,
    var duration: Int = 1
)