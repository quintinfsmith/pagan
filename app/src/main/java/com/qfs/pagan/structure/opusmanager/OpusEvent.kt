package com.qfs.pagan.structure.opusmanager

abstract class OpusEvent(var duration: Int = 1) {
    override fun equals(other: Any?): Boolean {
        return other is OpusEvent && other.duration == this.duration
    }

    override fun hashCode(): Int {
        val code = javaClass.hashCode()
        return (code shl (this.duration % 32)) + (code shr (32 - (this.duration % 32)))
    }

    abstract fun copy(): OpusEvent
}
