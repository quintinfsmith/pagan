package com.qfs.apres.event

class TuneRequest: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xF6.toByte())
    }
}