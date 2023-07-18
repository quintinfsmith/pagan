package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

class TuneRequest: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF6.toByte())
    }
}