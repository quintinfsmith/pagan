package com.qfs.apres.event

class Reset: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFF.toByte())
    }
}