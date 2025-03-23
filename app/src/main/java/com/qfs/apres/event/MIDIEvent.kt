package com.qfs.apres.event

interface GeneralMIDIEvent {
    fun as_bytes(): ByteArray
}

interface MIDIEvent: GeneralMIDIEvent {
    fun as_ump_bytes(): ByteArray {
        TODO()
    }
}
