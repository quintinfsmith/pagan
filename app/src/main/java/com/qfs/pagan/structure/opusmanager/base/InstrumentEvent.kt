package com.qfs.pagan.structure.opusmanager.base

abstract class InstrumentEvent(duration: Int = 1): OpusEvent(duration) {
    abstract override fun copy(): InstrumentEvent
}

abstract class TunedInstrumentEvent(duration: Int): InstrumentEvent(duration)

class AbsoluteNoteEvent(var note: Int, duration: Int = 1): TunedInstrumentEvent(duration) {
    override fun copy(): AbsoluteNoteEvent {
        val output = AbsoluteNoteEvent(this.note)
        output.duration = this.duration
        return output
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsoluteNoteEvent && this.note == other.note && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.note)
    }
}

class RelativeNoteEvent(var offset: Int, duration: Int = 1): TunedInstrumentEvent(duration) {
    override fun copy(): RelativeNoteEvent {
        val output = RelativeNoteEvent(this.offset)
        output.duration = this.duration
        return output
    }

    override fun equals(other: Any?): Boolean {
        return other is RelativeNoteEvent && this.offset == other.offset && super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode().xor(this.offset)
    }
}

class PercussionEvent(duration: Int = 1): InstrumentEvent(duration) {
    override fun hashCode(): Int {
        return (super.hashCode() shl 1) + 1
    }

    override fun copy(): PercussionEvent {
        val output = PercussionEvent()
        output.duration = this.duration
        return output
    }

    override fun equals(other: Any?): Boolean {
        return other is PercussionEvent && super.equals(other)
    }
}
