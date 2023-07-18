package com.qfs.apres.event

import com.qfs.apres.event.MIDIEvent

class MIDIContinue: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFB.toByte())
    }
}