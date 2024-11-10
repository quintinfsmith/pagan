package com.qfs.pagan.opusmanager

interface Copyable {
    fun copy(): Copyable
}
abstract class OpusEvent(var duration: Int = 1): Copyable {
    override fun equals(other: Any?): Boolean {
        return other is OpusEvent && other.duration == this.duration
    }
    abstract override fun copy(): OpusEvent
}
