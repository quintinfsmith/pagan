package com.qfs.apres.event

class ActiveSense: MIDIEvent {
    override fun as_bytes(): ByteArray {
        return byteArrayOf(0xFE.toByte())
    }
}