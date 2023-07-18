package com.qfs.apres.event

class EndOfTrack: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFF.toByte(), 0x2F.toByte(), 0x00.toByte())
    }
}