package com.qfs.apres.event

import com.qfs.apres.event2.UMPEvent
interface GeneralMIDIEvent {
    fun as_bytes(): ByteArray
}

interface MIDIEvent: GeneralMIDIEvent
