package com.qfs.apres.event

class MIDIStop: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFC.toByte())
    }
}