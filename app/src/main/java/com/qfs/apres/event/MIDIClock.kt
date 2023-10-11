package com.qfs.apres.event

class MIDIClock: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF8.toByte())
    }
}