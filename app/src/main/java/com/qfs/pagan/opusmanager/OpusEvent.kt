package com.qfs.pagan.opusmanager

abstract class OpusEvent(var duration: Int = 1) {
    override fun equals(other: Any?): Boolean {
        return other is OpusEvent && other.duration == this.duration
    }
}
