package com.qfs.apres.event

class MIDIStart: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFA.toByte())
    }
}