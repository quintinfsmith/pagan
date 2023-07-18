package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

class MIDIStop: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFC.toByte())
    }
}