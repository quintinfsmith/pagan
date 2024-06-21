package com.qfs.pagan.opusmanager

abstract class InstrumentEvent() {
    var duration: Int = 1
}

abstract class TunedInstrumentEvent(): InstrumentEvent()
class AbsoluteNoteEvent(var note: Int): TunedInstrumentEvent()
class RelativeNoteEvent(var offset: Int): TunedInstrumentEvent()
class PercussionEvent(): InstrumentEvent()

