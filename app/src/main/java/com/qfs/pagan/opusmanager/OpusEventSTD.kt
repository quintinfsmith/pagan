package com.qfs.pagan.opusmanager

interface OpusEvent {
}

data class OpusEventSTD(
    var note: Int,
    var channel: Int,
    var relative: Boolean,
    var duration: Int = 1
): OpusEvent