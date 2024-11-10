package com.qfs.pagan.opusmanager
abstract class CopyableEvent<T: OpusEvent> {
    abstract fun <T> copy(): T
}
abstract class OpusEvent(var duration: Int = 1): CopyableEvent<OpusEvent>() {
    override fun equals(other: Any?): Boolean {
        return other is OpusEvent && other.duration == this.duration
    }
}
