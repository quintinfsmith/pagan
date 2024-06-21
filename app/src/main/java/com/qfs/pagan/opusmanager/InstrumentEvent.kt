package com.qfs.pagan.opusmanager

abstract class InstrumentEvent(var duration: Int = 1) {
    abstract fun copy(): InstrumentEvent
}

abstract class TunedInstrumentEvent(duration: Int): InstrumentEvent(duration)
class AbsoluteNoteEvent(var note: Int, duration: Int = 1): TunedInstrumentEvent(duration) {
    override fun copy(): AbsoluteNoteEvent {
        val output = AbsoluteNoteEvent(this.note)
        output.duration = this.duration
        return output
    }
}

class RelativeNoteEvent(var offset: Int, duration: Int = 1): TunedInstrumentEvent(duration) {
    override fun copy(): RelativeNoteEvent {
        val output = RelativeNoteEvent(this.offset)
        output.duration = this.duration
        return output
    }
}

class PercussionEvent(duration: Int = 1): InstrumentEvent(duration) {
    override fun copy(): PercussionEvent {
        val output = PercussionEvent()
        output.duration = this.duration
        return output
    }
}
