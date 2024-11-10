package com.qfs.pagan.opusmanager

abstract class InstrumentEvent(duration: Int = 1): OpusEvent(duration) {
    override fun equals(other: Any?): Boolean {
        return other is InstrumentEvent && other.duration == this.duration
    }
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
}

class PercussionEvent(duration: Int = 1): InstrumentEvent(duration) {
    override fun copy(): PercussionEvent {
        val output = PercussionEvent()
        output.duration = this.duration
        return output
    }

    override fun equals(other: Any?): Boolean {
        return other is PercussionEvent && super.equals(other)
    }
}
