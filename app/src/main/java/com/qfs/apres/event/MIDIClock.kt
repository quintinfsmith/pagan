package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

class MIDIClock: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF8.toByte())
    }
}